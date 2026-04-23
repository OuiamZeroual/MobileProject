package com.pairplay.domain.repository

import kotlinx.coroutines.flow.Flow

data class ScoreEntry(
    val id: Long = 0,
    val playerName: String,
    val gameId: String,
    val score: Int,
    val timestamp: Long,
    val wasWinner: Boolean
)

interface ScoreRepository {
    fun history(): Flow<List<ScoreEntry>>
    suspend fun save(entry: ScoreEntry)
    suspend fun clear()
}
