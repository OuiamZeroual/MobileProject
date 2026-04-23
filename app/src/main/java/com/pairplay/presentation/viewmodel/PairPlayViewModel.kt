package com.pairplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairplay.core.engine.GameEngine
import com.pairplay.core.sound.SoundManager
import com.pairplay.domain.bluetooth.BluetoothGateway
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.game.MiniGameRegistry
import com.pairplay.domain.models.DiscoveredDevice
import com.pairplay.domain.models.GamePhase
import com.pairplay.domain.models.SessionResult
import com.pairplay.domain.repository.ScoreEntry
import com.pairplay.domain.repository.ScoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PairPlayViewModel @Inject constructor(
    private val engine: GameEngine,
    private val gateway: BluetoothGateway,
    private val registry: MiniGameRegistry,
    private val scoreRepo: ScoreRepository,
    private val sound: SoundManager
) : ViewModel() {

    val phase: StateFlow<GamePhase> = engine.phase
    val currentGame: StateFlow<MiniGame?> = engine.currentGame
    val devices: StateFlow<List<DiscoveredDevice>> =
        gateway.discoveredDevices.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val availableGames: List<MiniGame> = registry.all()

    fun seed(): Long = engine.seedForCurrentGame()
    fun meId(): String = engine.localId()
    fun playerName(id: String): String = engine.playerName(id)
    fun plannedGames(): List<String> = engine.plannedGames()

    fun startSolo() = engine.startSolo()

    fun startAsHost() = viewModelScope.launch {
        engine.startAsHost()
    }

    fun beginDiscovery() = viewModelScope.launch { gateway.startDiscovery() }
    fun stopDiscovery() = viewModelScope.launch { gateway.stopDiscovery() }

    fun connect(d: DiscoveredDevice) = viewModelScope.launch { engine.startAsClient(d.address) }

    fun hostStarts() = viewModelScope.launch { engine.hostStartsMatch() }

    fun onGameFinished(gameId: String, score: Int) {
        engine.onLocalGameFinished(gameId, score)
        viewModelScope.launch {
            scoreRepo.save(
                ScoreEntry(
                    playerName = playerName(meId()),
                    gameId = gameId,
                    score = score,
                    timestamp = System.currentTimeMillis(),
                    wasWinner = false
                )
            )
        }
    }

    fun onSessionFinished(result: SessionResult) {
        val me = meId()
        val iWin = result.winnerId == me && !result.isDraw
        if (iWin) sound.playVictory() else sound.playDefeat()
    }

    fun reset() = engine.reset()
}
