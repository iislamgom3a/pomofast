package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.TimerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cadenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cadence_timer_settings"
)

data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val dailyGoalMinutes: Int = 120,
    val autoStartBreak: Boolean = false,
    val autoStartFocus: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val activeLabel: String = "Deep Work",
) {
    fun durationSecondsFor(mode: TimerMode): Int = when (mode) {
        TimerMode.FOCUS -> focusMinutes * 60
        TimerMode.SHORT_BREAK -> shortBreakMinutes * 60
        TimerMode.LONG_BREAK -> longBreakMinutes * 60
    }
}

class TimerSettingsRepository(private val context: Context) {

    private object Keys {
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("sessions_before_long_break")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val AUTO_START_BREAK = booleanPreferencesKey("auto_start_break")
        val AUTO_START_FOCUS = booleanPreferencesKey("auto_start_focus")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val ACTIVE_LABEL = stringPreferencesKey("active_label")
    }

    val settingsFlow: Flow<TimerSettings> = context.cadenceDataStore.data.map { prefs ->
        TimerSettings(
            focusMinutes = (prefs[Keys.FOCUS_MINUTES] ?: 25).coerceIn(1, 90),
            shortBreakMinutes = (prefs[Keys.SHORT_BREAK_MINUTES] ?: 5).coerceIn(1, 30),
            longBreakMinutes = (prefs[Keys.LONG_BREAK_MINUTES] ?: 15).coerceIn(5, 60),
            sessionsBeforeLongBreak = (prefs[Keys.SESSIONS_BEFORE_LONG_BREAK] ?: 4).coerceIn(2, 8),
            dailyGoalMinutes = (prefs[Keys.DAILY_GOAL_MINUTES] ?: 120).coerceIn(15, 480),
            autoStartBreak = prefs[Keys.AUTO_START_BREAK] ?: false,
            autoStartFocus = prefs[Keys.AUTO_START_FOCUS] ?: false,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            activeLabel = prefs[Keys.ACTIVE_LABEL]?.takeIf { it.isNotBlank() } ?: "Deep Work",
        )
    }

    suspend fun updateSettings(transform: (TimerSettings) -> TimerSettings) {
        context.cadenceDataStore.edit { prefs ->
            val current = TimerSettings(
                focusMinutes = prefs[Keys.FOCUS_MINUTES] ?: 25,
                shortBreakMinutes = prefs[Keys.SHORT_BREAK_MINUTES] ?: 5,
                longBreakMinutes = prefs[Keys.LONG_BREAK_MINUTES] ?: 15,
                sessionsBeforeLongBreak = prefs[Keys.SESSIONS_BEFORE_LONG_BREAK] ?: 4,
                dailyGoalMinutes = prefs[Keys.DAILY_GOAL_MINUTES] ?: 120,
                autoStartBreak = prefs[Keys.AUTO_START_BREAK] ?: false,
                autoStartFocus = prefs[Keys.AUTO_START_FOCUS] ?: false,
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
                activeLabel = prefs[Keys.ACTIVE_LABEL] ?: "Deep Work",
            )
            val updated = transform(current)
            prefs[Keys.FOCUS_MINUTES] = updated.focusMinutes.coerceIn(1, 90)
            prefs[Keys.SHORT_BREAK_MINUTES] = updated.shortBreakMinutes.coerceIn(1, 30)
            prefs[Keys.LONG_BREAK_MINUTES] = updated.longBreakMinutes.coerceIn(5, 60)
            prefs[Keys.SESSIONS_BEFORE_LONG_BREAK] = updated.sessionsBeforeLongBreak.coerceIn(2, 8)
            prefs[Keys.DAILY_GOAL_MINUTES] = updated.dailyGoalMinutes.coerceIn(15, 480)
            prefs[Keys.AUTO_START_BREAK] = updated.autoStartBreak
            prefs[Keys.AUTO_START_FOCUS] = updated.autoStartFocus
            prefs[Keys.SOUND_ENABLED] = updated.soundEnabled
            prefs[Keys.HAPTICS_ENABLED] = updated.hapticsEnabled
            prefs[Keys.ACTIVE_LABEL] = updated.activeLabel.trim().ifBlank { "Deep Work" }.take(32)
        }
    }
}
