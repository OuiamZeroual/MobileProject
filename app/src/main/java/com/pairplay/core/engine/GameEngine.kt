package com.pairplay.core.engine

import com.pairplay.core.protocol.Msg
import com.pairplay.core.protocol.ProtocolCodec
import com.pairplay.domain.bluetooth.BluetoothGateway
import com.pairplay.domain.bluetooth.ConnectionEvent
import com.pairplay.domain.game.MiniGame
import com.pairplay.domain.game.MiniGameRegistry
import com.pairplay.domain.models.Category
import com.pairplay.domain.models.GamePhase
import com.pairplay.domain.models.MiniGameResult
import com.pairplay.domain.models.Player
import com.pairplay.domain.models.SessionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

/**
 * Orchestrateur central. Ne connaît AUCUN jeu concret : travaille uniquement via
 * l'interface MiniGame. Responsabilités :
 *   - sélection de 3 jeux aléatoires (1 par catégorie, seed partagée)
 *   - enchaînement des jeux
 *   - collecte et synchronisation des scores via le BluetoothGateway
 *   - détermination du gagnant
 */
class GameEngine(
    private val registry: MiniGameRegistry,
    private val gateway: BluetoothGateway,
    parentScope: CoroutineScope? = null
) {
    private val scope = parentScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val me: Player = Player(
        id = UUID.randomUUID().toString().take(8),
        name = android.os.Build.MODEL ?: "Player"
    )

    private val _phase = MutableStateFlow<GamePhase>(GamePhase.Idle)
    val phase: StateFlow<GamePhase> = _phase.asStateFlow()

    private val _currentGame = MutableStateFlow<MiniGame?>(null)
    val currentGame: StateFlow<MiniGame?> = _currentGame.asStateFlow()

    /** Scores : gameId → (playerId → score). Rempli incrémentalement. */
    private val scores: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
    private val players: MutableMap<String, Player> = mutableMapOf(me.id to me)

    private var plannedGameIds: List<String> = emptyList()
    private var currentSeed: Long = 0L
    private var collectingJob: Job? = null
    private var isHost: Boolean = false
    private var soloMode: Boolean = false

    // --- Public API (appelée par les ViewModels) --------------------------------

    fun listen() {
        scope.launch {
            gateway.connectionEvents.collect { ev ->
                when (ev) {
                    is ConnectionEvent.Connected -> {
                        val peer = Player(id = ev.deviceAddress.takeLast(8), name = ev.deviceName)
                        players[peer.id] = peer
                        _phase.value = GamePhase.Connected(players.values.toList())
                        if (isHost) {
                            // L'hôte envoie une Invite dès la connexion pour initier l'échange d'IDs
                            gateway.send(ProtocolCodec.encode(Msg.Invite(me.id, me.name)))
                        }
                    }
                    ConnectionEvent.Disconnected -> {
                        if (_phase.value !is GamePhase.Finished) {
                            _phase.value = GamePhase.Error("Peer déconnecté")
                        }
                    }
                    is ConnectionEvent.Failed -> _phase.value = GamePhase.Error(ev.reason)
                }
            }
        }
        scope.launch {
            gateway.incomingMessages.collect { raw ->
                runCatching { ProtocolCodec.decode(raw) }.getOrNull()?.let(::onMessage)
            }
        }
    }

    suspend fun startAsHost() {
        isHost = true
        soloMode = false
        _phase.value = GamePhase.Idle
        gateway.startServer()
    }

    suspend fun startAsClient(address: String) {
        isHost = false
        soloMode = false
        _phase.value = GamePhase.Connecting(com.pairplay.domain.models.DiscoveredDevice(address, ""))
        gateway.connect(address)
    }

    /** Mode solo — joue les 3 jeux localement sans réseau. */
    fun startSolo() {
        soloMode = true
        isHost = true
        players.clear(); players[me.id] = me
        scores.clear()
        currentSeed = System.currentTimeMillis()
        plannedGameIds = pickThree(currentSeed)
        runNextGame(index = 0)
    }

    /** L'hôte décide du set et envoie START_GAME. */
    suspend fun hostStartsMatch() {
        if (!isHost) return
        currentSeed = System.currentTimeMillis()
        plannedGameIds = pickThree(currentSeed)
        scores.clear()
        gateway.send(ProtocolCodec.encode(Msg.StartGame(currentSeed, plannedGameIds)))
        runNextGame(index = 0)
    }

    /** Remonté par l'UI lorsqu'un mini-jeu se termine. */
    fun onLocalGameFinished(gameId: String, score: Int) {
        recordScore(me.id, gameId, score)
        if (!soloMode) {
            scope.launch {
                gateway.send(ProtocolCodec.encode(Msg.Score(me.id, gameId, score)))
            }
        }
        val idx = plannedGameIds.indexOf(gameId)
        val next = idx + 1
        if (next < plannedGameIds.size) {
            if (soloMode) {
                runNextGame(next)
            } else {
                // On attend le score du peer avant d'enchaîner
                _phase.value = GamePhase.Syncing(gameId)
                scope.launch { waitAllScoresFor(gameId); runNextGame(next) }
            }
        } else {
            if (soloMode) {
                finish()
            } else {
                _phase.value = GamePhase.Syncing(gameId)
                scope.launch { waitAllScoresFor(gameId); finish() }
            }
        }
    }

    fun reset() {
        scores.clear()
        plannedGameIds = emptyList()
        _phase.value = GamePhase.Idle
        _currentGame.value = null
        scope.launch { runCatching { gateway.disconnect() } }
    }

    // --- Internal logic ---------------------------------------------------------

    private fun onMessage(m: Msg) {
        when (m) {
            is Msg.Invite -> {
                players[m.fromId] = Player(m.fromId, m.fromName)
                if (!isHost) {
                    scope.launch { gateway.send(ProtocolCodec.encode(Msg.Accept(me.id, me.name))) }
                }
                _phase.value = GamePhase.Connected(players.values.toList())
            }
            is Msg.Accept -> {
                players[m.fromId] = Player(m.fromId, m.fromName)
                _phase.value = GamePhase.Connected(players.values.toList())
            }
            is Msg.StartGame -> {
                currentSeed = m.seed
                plannedGameIds = m.gameIds
                scores.clear()
                runNextGame(0)
            }
            is Msg.Score -> {
                recordScore(m.fromId, m.gameId, m.score)
            }
            is Msg.FinalResult -> {
                // Le client reçoit le verdict de l'hôte — source de vérité
                _phase.value = GamePhase.Finished(
                    SessionResult(
                        perPlayerTotals = m.totals,
                        perGame = scores.flatMap { (gid, map) ->
                            map.map { (pid, s) -> MiniGameResult(pid, gid, s) }
                        },
                        winnerId = m.winnerId,
                        isDraw = m.isDraw
                    )
                )
            }
            is Msg.Ping -> Unit
        }
    }

    private fun recordScore(playerId: String, gameId: String, score: Int) {
        scores.getOrPut(gameId) { mutableMapOf() }[playerId] = score
    }

    private suspend fun waitAllScoresFor(gameId: String) {
        val needed = players.keys
        while (true) {
            val got = scores[gameId]?.keys ?: emptySet()
            if (got.containsAll(needed)) return
            kotlinx.coroutines.delay(100)
        }
    }

    private fun runNextGame(index: Int) {
        val gameId = plannedGameIds.getOrNull(index) ?: return finish()
        val game = registry.byId(gameId) ?: return finish()
        _currentGame.value = game
        _phase.value = GamePhase.Playing(gameId, index, plannedGameIds.size)
    }

    private fun finish() {
        val totals: Map<String, Int> = players.keys.associateWith { pid ->
            scores.values.sumOf { it[pid] ?: 0 }
        }
        val maxScore = totals.values.maxOrNull() ?: 0
        val winners = totals.filterValues { it == maxScore }.keys.toList()
        val isDraw = winners.size > 1
        val winnerId = if (isDraw) null else winners.firstOrNull()

        val result = SessionResult(
            perPlayerTotals = totals,
            perGame = scores.flatMap { (gid, map) ->
                map.map { (pid, s) -> MiniGameResult(pid, gid, s) }
            },
            winnerId = winnerId,
            isDraw = isDraw
        )
        _phase.value = GamePhase.Finished(result)
        _currentGame.value = null

        if (!soloMode && isHost) {
            scope.launch {
                gateway.send(ProtocolCodec.encode(Msg.FinalResult(totals, winnerId, isDraw)))
            }
        }
    }

    /** Sélection déterministe à partir d'une seed — les deux peers obtiennent la même liste. */
    private fun pickThree(seed: Long): List<String> {
        val rng = Random(seed)
        return Category.entries.map { cat ->
            val options = registry.byCategory(cat)
            require(options.isNotEmpty()) { "Aucun jeu enregistré pour la catégorie $cat" }
            options[rng.nextInt(options.size)].id
        }
    }

    fun seedForCurrentGame(): Long = currentSeed

    fun playerName(id: String): String = players[id]?.name ?: id
    fun localId(): String = me.id
    fun allPlayers(): List<Player> = players.values.toList()
    fun plannedGames(): List<String> = plannedGameIds
}
