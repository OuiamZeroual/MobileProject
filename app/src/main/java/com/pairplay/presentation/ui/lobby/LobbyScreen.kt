package com.pairplay.presentation.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairplay.domain.models.DiscoveredDevice
import com.pairplay.domain.models.GamePhase
import com.pairplay.domain.models.Player
import com.pairplay.presentation.ui.components.GradientButton

@Composable
fun LobbyScreen(
    isHost: Boolean,
    phase: GamePhase,
    devices: List<DiscoveredDevice>,
    onRescan: () -> Unit,
    onConnect: (DiscoveredDevice) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
            Text(
                if (isHost) "Salon (Hôte)" else "Rejoindre",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            if (!isHost) IconButton(onClick = onRescan) { Icon(Icons.Filled.Refresh, null) }
        }
        Spacer(Modifier.height(16.dp))

        when (phase) {
            GamePhase.Idle, GamePhase.Discovering -> {
                if (isHost) WaitingForPeer()
                else ClientDeviceList(devices, onConnect)
            }
            is GamePhase.Connecting -> {
                CenterLoading("Connexion en cours…")
            }
            is GamePhase.Connected -> {
                PeersReady(phase.peers, isHost, onStart)
            }
            is GamePhase.Error -> {
                Text("Erreur : ${phase.message}",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold)
            }
            else -> {
                CenterLoading("Chargement…")
            }
        }
    }
}

@Composable
private fun WaitingForPeer() {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Bluetooth, null, modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("En attente d'un joueur…", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Demande à ton ami d'appuyer sur \"Rejoindre\" puis de te sélectionner.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator()
    }
}

@Composable
private fun ClientDeviceList(devices: List<DiscoveredDevice>, onConnect: (DiscoveredDevice) -> Unit) {
    if (devices.isEmpty()) {
        CenterLoading("Recherche d'appareils…")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(devices) { d ->
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .clickable { onConnect(d) },
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Row(Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Bluetooth, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, fontWeight = FontWeight.SemiBold)
                        Text(d.address, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (d.isBonded) Text("★ Appairé", color = Color(0xFF10B981),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PeersReady(peers: List<Player>, isHost: Boolean, onStart: () -> Unit) {
    Column {
        Text("${peers.size} joueur${if (peers.size > 1) "s" else ""} connecté${if (peers.size > 1) "s" else ""}",
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        peers.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981))
                Spacer(Modifier.width(12.dp))
                Text(p.name, fontWeight = FontWeight.Medium)
                if (p.isHost) {
                    Spacer(Modifier.width(8.dp))
                    Text("(hôte)", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (isHost) {
            GradientButton(
                text = "Lancer la partie",
                colors = listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
                onClick = onStart
            )
        } else {
            Text("En attente du lancement par l'hôte…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun CenterLoading(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
