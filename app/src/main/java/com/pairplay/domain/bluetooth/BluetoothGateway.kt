package com.pairplay.domain.bluetooth

import com.pairplay.domain.models.DiscoveredDevice
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction du transport Bluetooth — la couche domain ignore les détails RFCOMM.
 * Une implémentation fake peut être injectée en test.
 */
interface BluetoothGateway {
    val isEnabled: Boolean
    val discoveredDevices: Flow<List<DiscoveredDevice>>
    val connectionEvents: Flow<ConnectionEvent>
    val incomingMessages: Flow<String>

    suspend fun startDiscovery()
    suspend fun stopDiscovery()

    /** Mode hôte — écoute les connexions entrantes sur l'UUID RFCOMM. */
    suspend fun startServer()
    suspend fun stopServer()

    /** Mode client — se connecte à un device déjà découvert. */
    suspend fun connect(address: String)
    suspend fun disconnect()

    suspend fun send(json: String)
}

sealed interface ConnectionEvent {
    data class Connected(val deviceAddress: String, val deviceName: String) : ConnectionEvent
    data object Disconnected : ConnectionEvent
    data class Failed(val reason: String) : ConnectionEvent
}
