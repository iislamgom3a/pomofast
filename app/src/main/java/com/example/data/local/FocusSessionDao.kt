package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY completedAtMillis DESC")
    fun observeAllSessions(): Flow<List<FocusSessionEntity>>

    @Query(
        "SELECT * FROM focus_sessions " +
            "WHERE sessionType = :sessionType " +
            "ORDER BY completedAtMillis DESC"
    )
    fun observeSessionsByType(sessionType: String): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
