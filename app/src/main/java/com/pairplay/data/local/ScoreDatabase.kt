package com.pairplay.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "score")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerName: String,
    val gameId: String,
    val score: Int,
    val timestamp: Long,
    val wasWinner: Boolean
)

@Dao
interface ScoreDao {
    @Query("SELECT * FROM score ORDER BY timestamp DESC LIMIT 200")
    fun all(): Flow<List<ScoreEntity>>

    @Insert
    suspend fun insert(e: ScoreEntity)

    @Query("DELETE FROM score")
    suspend fun clear()
}

@Database(entities = [ScoreEntity::class], version = 1, exportSchema = false)
abstract class ScoreDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        fun build(ctx: Context): ScoreDatabase =
            Room.databaseBuilder(ctx.applicationContext, ScoreDatabase::class.java, "pairplay.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
