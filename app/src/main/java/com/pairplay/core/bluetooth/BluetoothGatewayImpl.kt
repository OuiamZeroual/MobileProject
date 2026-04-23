package com.pairplay.core.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.pairplay.domain.bluetooth.BluetoothGateway
import com.pairplay.domain.bluetooth.ConnectionEvent
import com.pairplay.domain.models.DiscoveredDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

/**
 * Implémentation Bluetooth Classic (RFCOMM) pour l'échange entre téléphones.
 * Protocole : lignes JSON terminées par \n.
 */
class BluetoothGatewayImpl(
    private val appContext: Context
) : BluetoothGateway {

    companion object {
        /** UUID RFCOMM partagé par toutes les instances PairPlay. */
        val PAIRPLAY_UUID: UUID = UUID.fromString("b7c9e9b2-4e4e-4c6a-9f2d-7a1b2c3d4e5f")
        const val SERVICE_NAME = "PairPlay-RFCOMM"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    override val isEnabled: Boolean get() = adapter?.isEnabled == true

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 16)
    override val connectionEvents: SharedFlow<ConnectionEvent> = _events.asSharedFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val incomingMessages: SharedFlow<String> = _messages.asSharedFlow()

    private var socket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var readerJob: Job? = null

    private var receiver: BroadcastReceiver? = null

    private fun hasPerm(name: String) =
        ContextCompat.checkSelfPermission(appContext, name) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override suspend fun startDiscovery() = withContext(Dispatchers.IO) {
        val adapter = adapter ?: return@withContext
        if (Build.VERSION.SDK_INT >= 31 && !hasPerm(Manifest.permission.BLUETOOTH_SCAN)) return@withContext

        _devices.value = adapter.bondedDevices
            ?.map { it.toDiscovered(bonded = true) }
            ?: emptyList()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        val rx = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val dev: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= 33) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        dev?.let { d ->
                            val current = _devices.value.toMutableList()
                            if (current.none { it.address == d.address }) {
                                current.add(d.toDiscovered(rssi = rssi, bonded = false))
                                _devices.value = current
                            }
                        }
                    }
                }
            }
        }
        receiver = rx
        appContext.registerReceiver(rx, filter)
        adapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopDiscovery() = withContext(Dispatchers.IO) {
        runCatching { adapter?.cancelDiscovery() }
        runCatching { receiver?.let { appContext.unregisterReceiver(it) } }
        receiver = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun startServer() = withContext(Dispatchers.IO) {
        val adapter = adapter ?: return@withContext
        runCatching {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, PAIRPLAY_UUID)
            val s = serverSocket!!.accept()
            runCatching { serverSocket?.close() }
            onSocketReady(s)
        }.onFailure { _events.emit(ConnectionEvent.Failed(it.message ?: "server error")) }
    }

    override suspend fun stopServer() = withContext(Dispatchers.IO) {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String) = withContext(Dispatchers.IO) {
        val adapter = adapter ?: return@withContext
        runCatching {
            adapter.cancelDiscovery()
            val device = adapter.getRemoteDevice(address)
            val s = device.createRfcommSocketToServiceRecord(PAIRPLAY_UUID)
            s.connect()
            onSocketReady(s)
        }.onFailure { _events.emit(ConnectionEvent.Failed(it.message ?: "connect error")) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun onSocketReady(s: BluetoothSocket) {
        socket = s
        writer = BufferedWriter(OutputStreamWriter(s.outputStream, Charsets.UTF_8))
        reader = BufferedReader(InputStreamReader(s.inputStream, Charsets.UTF_8))
        val name = runCatching { s.remoteDevice.name ?: s.remoteDevice.address }.getOrElse { "peer" }
        _events.emit(ConnectionEvent.Connected(s.remoteDevice.address, name))
        readerJob?.cancel()
        readerJob = scope.launch {
            try {
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line.isNotBlank()) _messages.emit(line)
                }
            } catch (_: Throwable) { /* closed */ }
            _events.emit(ConnectionEvent.Disconnected)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { writer?.close() }
        runCatching { reader?.close() }
        runCatching { socket?.close() }
        runCatching { serverSocket?.close() }
        readerJob?.cancel()
        socket = null; writer = null; reader = null; serverSocket = null
    }

    override suspend fun send(json: String) = withContext(Dispatchers.IO) {
        runCatching {
            val w = writer ?: return@runCatching
            w.write(json); w.write("\n"); w.flush()
        }
        Unit
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toDiscovered(rssi: Int? = null, bonded: Boolean = false) =
        DiscoveredDevice(
            address = address,
            name = runCatching { name }.getOrNull() ?: address,
            rssi = rssi,
            isBonded = bonded
        )
}
