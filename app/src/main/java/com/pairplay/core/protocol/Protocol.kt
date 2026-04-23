package com.pairplay.core.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Protocole JSON échangé entre les peers via RFCOMM.
 * Sérialisation polymorphe fournie par kotlinx.serialization (sealed interface).
 */
@Serializable
sealed interface Msg {
    @Serializable
    data class Invite(val fromId: String, val fromName: String) : Msg

    @Serializable
    data class Accept(val fromId: String, val fromName: String) : Msg

    @Serializable
    data class StartGame(val seed: Long, val gameIds: List<String>) : Msg

    @Serializable
    data class Score(val fromId: String, val gameId: String, val score: Int) : Msg

    @Serializable
    data class FinalResult(
        val totals: Map<String, Int>,
        val winnerId: String?,
        val isDraw: Boolean
    ) : Msg

    @Serializable
    data class Ping(val t: Long) : Msg
}

object ProtocolCodec {
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        prettyPrint = false
    }

    fun encode(m: Msg): String = json.encodeToString(Msg.serializer(), m)
    fun decode(raw: String): Msg = json.decodeFromString(Msg.serializer(), raw)
}
