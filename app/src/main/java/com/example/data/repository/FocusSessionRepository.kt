package com.example.data.repository

import com.example.data.local.FocusSessionDao
import com.example.data.local.FocusSessionEntity
import com.example.data.local.TimerMode
import kotlinx.coroutines.flow.Flow

class FocusSessionRepository(private val dao: FocusSessionDao) {

    val allSessions: Flow<List<FocusSessionEntity>> = dao.observeAllSessions()

    val focusSessions: Flow<List<FocusSessionEntity>> =
        dao.observeSessionsByType(TimerMode.FOCUS.storageKey)

    suspend fun recordSession(
        mode: TimerMode,
        label: String,
        plannedDurationSeconds: Int,
        actualDurationSeconds: Int,
        completedAtMillis: Long = System.currentTimeMillis(),
        completedFull: Boolean = true,
    ): Long {
        val sanitizedLabel = label.trim().ifBlank { "Deep Work" }.take(32)
        val entity = FocusSessionEntity(
            sessionType = mode.storageKey,
            label = sanitizedLabel,
            plannedDurationSeconds = plannedDurationSeconds.coerceAtLeast(60),
            actualDurationSeconds = actualDurationSeconds.coerceAtLeast(1),
            completedAtMillis = completedAtMillis,
            completedFull = completedFull,
        )
        return dao.insertSession(entity)
    }

    suspend fun deleteSession(id: Long) {
        dao.deleteSessionById(id)
    }
}
