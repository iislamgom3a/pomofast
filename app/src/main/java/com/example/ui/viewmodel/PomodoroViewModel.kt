package com.example.ui.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.TimerMode
import com.example.data.preferences.TimerSettings
import com.example.data.preferences.TimerSettingsRepository
import com.example.data.repository.FocusSessionRepository
import com.example.domain.PeriodStatistics
import com.example.domain.ProductivityStatsCalculator
import com.example.domain.StatsPeriod
import com.example.domain.TodaySummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUiState(
    val mode: TimerMode = TimerMode.FOCUS,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val hasStartedCurrentInterval: Boolean = false,
    val completedFocusInCycle: Int = 0,
    val settings: TimerSettings = TimerSettings(),
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f
        else ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val elapsedSeconds: Int
        get() = (totalSeconds - remainingSeconds).coerceAtLeast(0)

    val formattedTime: String
        get() {
            val mins = (remainingSeconds / 60).coerceAtLeast(0)
            val secs = (remainingSeconds % 60).coerceAtLeast(0)
            return "%02d:%02d".format(mins, secs)
        }
}

sealed interface TimerEvent {
    data class FocusLogged(val minutes: Int, val sound: Boolean, val haptics: Boolean) : TimerEvent
    data class BreakFinished(val sound: Boolean, val haptics: Boolean) : TimerEvent
    data class ManualSessionLogged(val minutes: Int) : TimerEvent
    data object TapHaptic : TimerEvent
}

class PomodoroViewModel(
    private val sessionRepository: FocusSessionRepository,
    private val settingsRepository: TimerSettingsRepository,
) : ViewModel() {

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.DAILY)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    private val _periodOffset = MutableStateFlow(0)
    val periodOffset: StateFlow<Int> = _periodOffset.asStateFlow()

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    val todaySummary: StateFlow<TodaySummary> = sessionRepository.allSessions
        .map { sessions -> ProductivityStatsCalculator.computeTodaySummary(sessions) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TodaySummary(),
        )

    val periodStatistics: StateFlow<PeriodStatistics> = combine(
        sessionRepository.allSessions,
        settingsRepository.settingsFlow,
        _selectedPeriod,
        _periodOffset,
    ) { sessions, settings, period, offset ->
        ProductivityStatsCalculator.computePeriodStatistics(
            allSessions = sessions,
            period = period,
            offset = offset,
            dailyGoalMinutes = settings.dailyGoalMinutes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ProductivityStatsCalculator.computePeriodStatistics(
            allSessions = emptyList(),
            period = StatsPeriod.DAILY,
            offset = 0,
            dailyGoalMinutes = 120,
        ),
    )

    private var timerJob: Job? = null
    private var endRealtimeMs: Long = 0L

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { newSettings ->
                _timerState.update { current ->
                    val updatedDuration = newSettings.durationSecondsFor(current.mode)
                    if (!current.isRunning && !current.hasStartedCurrentInterval) {
                        current.copy(
                            settings = newSettings,
                            totalSeconds = updatedDuration,
                            remainingSeconds = updatedDuration,
                        )
                    } else {
                        current.copy(settings = newSettings)
                    }
                }
            }
        }
    }

    fun selectMode(mode: TimerMode) {
        if (_timerState.value.mode == mode) return
        stopTicker()
        val settings = _timerState.value.settings
        val duration = settings.durationSecondsFor(mode)
        _timerState.update {
            it.copy(
                mode = mode,
                totalSeconds = duration,
                remainingSeconds = duration,
                isRunning = false,
                hasStartedCurrentInterval = false,
            )
        }
        emitTapHaptic()
    }

    fun toggleStartPause() {
        val current = _timerState.value
        if (current.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
        emitTapHaptic()
    }

    private fun startTimer() {
        val current = _timerState.value
        if (current.remainingSeconds <= 0) {
            val duration = current.settings.durationSecondsFor(current.mode)
            _timerState.update {
                it.copy(totalSeconds = duration, remainingSeconds = duration)
            }
        }
        val secondsLeft = _timerState.value.remainingSeconds
        endRealtimeMs = SystemClock.elapsedRealtime() + secondsLeft * 1_000L

        _timerState.update {
            it.copy(isRunning = true, hasStartedCurrentInterval = true)
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val remainingMs = endRealtimeMs - SystemClock.elapsedRealtime()
                val remainingSec = ((remainingMs + 999L) / 1_000L).toInt().coerceAtLeast(0)
                if (remainingSec != _timerState.value.remainingSeconds) {
                    _timerState.update { it.copy(remainingSeconds = remainingSec) }
                }
                if (remainingMs <= 0L) {
                    handleIntervalCompleted(completedFull = true)
                    break
                }
                delay(200L)
            }
        }
    }

    private fun pauseTimer() {
        stopTicker()
        val remainingMs = (endRealtimeMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val remainingSec = ((remainingMs + 999L) / 1_000L).toInt()
        _timerState.update {
            it.copy(
                isRunning = false,
                remainingSeconds = remainingSec.coerceIn(0, it.totalSeconds),
            )
        }
    }

    fun resetTimer() {
        stopTicker()
        val current = _timerState.value
        val duration = current.settings.durationSecondsFor(current.mode)
        _timerState.update {
            it.copy(
                totalSeconds = duration,
                remainingSeconds = duration,
                isRunning = false,
                hasStartedCurrentInterval = false,
            )
        }
        emitTapHaptic()
    }

    fun skipOrFinishCurrentInterval() {
        val current = _timerState.value
        stopTicker()
        viewModelScope.launch {
            if (current.mode == TimerMode.FOCUS && current.elapsedSeconds >= 60) {
                // Save meaningful partial focus effort (>= 1 minute)
                handleIntervalCompleted(completedFull = false)
            } else {
                advanceToNextPhase(wasFocusCompleted = false, autoStart = false)
            }
        }
        emitTapHaptic()
    }

    private suspend fun handleIntervalCompleted(completedFull: Boolean) {
        stopTicker()
        val current = _timerState.value
        val settings = current.settings
        val recordedSeconds = if (completedFull) current.totalSeconds else current.elapsedSeconds

        if (current.mode == TimerMode.FOCUS) {
            if (recordedSeconds >= 60) {
                sessionRepository.recordSession(
                    mode = TimerMode.FOCUS,
                    label = settings.activeLabel,
                    plannedDurationSeconds = current.totalSeconds,
                    actualDurationSeconds = recordedSeconds,
                    completedFull = completedFull,
                )
                val loggedMinutes = ((recordedSeconds + 30) / 60).coerceAtLeast(1)
                _events.tryEmit(
                    TimerEvent.FocusLogged(
                        minutes = loggedMinutes,
                        sound = settings.soundEnabled,
                        haptics = settings.hapticsEnabled,
                    )
                )
            }
            advanceToNextPhase(wasFocusCompleted = true, autoStart = settings.autoStartBreak)
        } else {
            _events.tryEmit(
                TimerEvent.BreakFinished(
                    sound = settings.soundEnabled,
                    haptics = settings.hapticsEnabled,
                )
            )
            advanceToNextPhase(wasFocusCompleted = false, autoStart = settings.autoStartFocus)
        }
    }

    private fun advanceToNextPhase(wasFocusCompleted: Boolean, autoStart: Boolean) {
        val current = _timerState.value
        val settings = current.settings

        val nextMode: TimerMode
        val nextCycleCount: Int

        if (current.mode == TimerMode.FOCUS) {
            val incremented = if (wasFocusCompleted) current.completedFocusInCycle + 1 else current.completedFocusInCycle
            if (incremented >= settings.sessionsBeforeLongBreak) {
                nextMode = TimerMode.LONG_BREAK
                nextCycleCount = 0
            } else {
                nextMode = TimerMode.SHORT_BREAK
                nextCycleCount = incremented
            }
        } else {
            nextMode = TimerMode.FOCUS
            nextCycleCount = current.completedFocusInCycle
        }

        val nextDuration = settings.durationSecondsFor(nextMode)
        _timerState.update {
            it.copy(
                mode = nextMode,
                totalSeconds = nextDuration,
                remainingSeconds = nextDuration,
                isRunning = false,
                hasStartedCurrentInterval = false,
                completedFocusInCycle = nextCycleCount,
            )
        }

        if (autoStart) {
            startTimer()
        }
    }

    fun updateSettings(transform: (TimerSettings) -> TimerSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    fun applyPreset(focusMin: Int, shortBreakMin: Int, longBreakMin: Int) {
        updateSettings {
            it.copy(
                focusMinutes = focusMin,
                shortBreakMinutes = shortBreakMin,
                longBreakMinutes = longBreakMin,
            )
        }
        emitTapHaptic()
    }

    fun updateActiveLabel(label: String) {
        updateSettings { it.copy(activeLabel = label) }
    }

    fun selectStatsPeriod(period: StatsPeriod) {
        if (_selectedPeriod.value == period) return
        _selectedPeriod.value = period
        _periodOffset.value = 0
        emitTapHaptic()
    }

    fun navigateStatsPeriod(delta: Int) {
        val next = (_periodOffset.value + delta).coerceAtMost(0)
        _periodOffset.value = next
        emitTapHaptic()
    }

    fun logManualFocusSession(minutes: Int, label: String) {
        val clampedMinutes = minutes.coerceIn(1, 240)
        viewModelScope.launch {
            val activeLabel = label.trim().ifBlank {
                settingsRepository.settingsFlow.first().activeLabel
            }
            sessionRepository.recordSession(
                mode = TimerMode.FOCUS,
                label = activeLabel,
                plannedDurationSeconds = clampedMinutes * 60,
                actualDurationSeconds = clampedMinutes * 60,
                completedAtMillis = System.currentTimeMillis(),
                completedFull = true,
            )
            _events.tryEmit(TimerEvent.ManualSessionLogged(clampedMinutes))
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    private fun emitTapHaptic() {
        if (_timerState.value.settings.hapticsEnabled) {
            _events.tryEmit(TimerEvent.TapHaptic)
        }
    }

    private fun stopTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    class Factory(
        private val sessionRepository: FocusSessionRepository,
        private val settingsRepository: TimerSettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
                return PomodoroViewModel(sessionRepository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
