package com.pairplay.domain.models

import kotlinx.serialization.Serializable

/** 3 catégories obligatoires — garantit la diversité de chaque partie. */
enum class Category(val label: String, val emoji: String) {
    SENSOR("Capteurs", "📡"),
    GESTURE("Gestes", "✋"),
    QNA("Q/R", "❓")
}

@Serializable
data class Player(
    val id: String,
    val name: String,
    val isHost: Boolean = false
)

/** Score d'un joueur à un mini-jeu particulier. */
@Serializable
data class MiniGameResult(
    val playerId: String,
    val gameId: String,
    val score: Int
)

/** État final d'une partie. */
data class SessionResult(
    val perPlayerTotals: Map<String, Int>,
    val perGame: List<MiniGameResult>,
    val winnerId: String?,
    val isDraw: Boolean
)

/** Représentation d'un device Bluetooth détecté. */
data class DiscoveredDevice(
    val address: String,
    val name: String,
    val rssi: Int? = null,
    val isBonded: Boolean = false
)

/** État haut-niveau d'une session de jeu. */
sealed interface GamePhase {
    data object Idle : GamePhase
    data object Discovering : GamePhase
    data class Connecting(val device: DiscoveredDevice) : GamePhase
    data class Connected(val peers: List<Player>) : GamePhase
    data class Playing(val gameId: String, val index: Int, val total: Int) : GamePhase
    data class Syncing(val gameId: String) : GamePhase
    data class Finished(val result: SessionResult) : GamePhase
    data class Error(val message: String) : GamePhase
}
