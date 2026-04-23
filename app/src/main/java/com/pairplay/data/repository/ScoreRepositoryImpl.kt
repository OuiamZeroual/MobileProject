package com.pairplay.data.repository

import com.pairplay.data.local.ScoreDao
import com.pairplay.data.local.ScoreEntity
import com.pairplay.domain.repository.ScoreEntry
import com.pairplay.domain.repository.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScoreRepositoryImpl(private val dao: ScoreDao) : ScoreRepository {
    override fun history(): Flow<List<ScoreEntry>> = dao.all().map { list ->
        list.map { ScoreEntry(it.id, it.playerName, it.gameId, it.score, it.timestamp, it.wasWinner) }
    }

    override suspend fun save(entry: ScoreEntry) {
        dao.insert(ScoreEntity(
            playerName = entry.playerName,
            gameId = entry.gameId,
            score = entry.score,
            timestamp = entry.timestamp,
            wasWinner = entry.wasWinner
        ))
    }

    override suspend fun clear() = dao.clear()
}
