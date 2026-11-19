package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TimerMode(val storageKey: String) {
    FOCUS("FOCUS"),
    SHORT_BREAK("SHORT_BREAK"),
    LONG_BREAK("LONG_BREAK");

    companion object {
        fun fromKey(key: String): TimerMode =
            entries.firstOrNull { it.storageKey == key } ?: FOCUS
    }
}

@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["completedAtMillis"])]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionType: String = TimerMode.FOCUS.storageKey,
    val label: String = "Deep Work",
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val completedAtMillis: Long,
    val completedFull: Boolean = true,
)
