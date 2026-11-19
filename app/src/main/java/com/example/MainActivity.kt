package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CadenceDatabase
import com.example.data.preferences.TimerSettingsRepository
import com.example.data.repository.FocusSessionRepository
import com.example.ui.components.IntervalSettingsSheet
import com.example.ui.feedback.FeedbackHelper
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.CadenceTheme
import com.example.ui.viewmodel.PomodoroViewModel
import com.example.ui.viewmodel.TimerEvent

private enum class CadenceDestination {
    TIMER,
    STATISTICS,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CadenceTheme {
                CadenceApp()
            }
        }
    }
}

@Composable
fun CadenceApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val database = remember { CadenceDatabase.getInstance(appContext) }
    val sessionRepository = remember { FocusSessionRepository(database.focusSessionDao()) }
    val settingsRepository = remember { TimerSettingsRepository(appContext) }
    val feedbackHelper = remember { FeedbackHelper(appContext) }

    val viewModel: PomodoroViewModel = viewModel(
        factory = PomodoroViewModel.Factory(
            sessionRepository = sessionRepository,
            settingsRepository = settingsRepository,
        )
    )

    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val todaySummary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val periodStats by viewModel.periodStatistics.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(CadenceDestination.TIMER) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusLoggedTemplate = stringResource(R.string.timer_finish_logged_message)
    val breakCompleteMessage = stringResource(R.string.timer_break_complete_message)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TimerEvent.FocusLogged -> {
                    if (event.sound) feedbackHelper.playCompletionChime()
                    if (event.haptics) feedbackHelper.vibrateCompletion()
                    snackbarHostState.showSnackbar(focusLoggedTemplate.format(event.minutes))
                }
                is TimerEvent.BreakFinished -> {
                    if (event.sound) feedbackHelper.playCompletionChime()
                    if (event.haptics) feedbackHelper.vibrateCompletion()
                    snackbarHostState.showSnackbar(breakCompleteMessage)
                }
                is TimerEvent.ManualSessionLogged -> {
                    snackbarHostState.showSnackbar(focusLoggedTemplate.format(event.minutes))
                }
                TimerEvent.TapHaptic -> {
                    feedbackHelper.vibrateTap()
                }
            }
        }
    }

    if (currentDestination != CadenceDestination.TIMER) {
        BackHandler {
            currentDestination = CadenceDestination.TIMER
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                    ) {
                        NavigationBarItem(
                            selected = currentDestination == CadenceDestination.TIMER,
                            onClick = { currentDestination = CadenceDestination.TIMER },
                            icon = {
                                Icon(
                                    imageVector = if (currentDestination == CadenceDestination.TIMER)
                                        Icons.Rounded.Timer else Icons.Outlined.Timer,
                                    contentDescription = stringResource(R.string.nav_timer),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_timer)) },
                            modifier = Modifier.testTag("nav_timer_item"),
                        )
                        NavigationBarItem(
                            selected = currentDestination == CadenceDestination.STATISTICS,
                            onClick = { currentDestination = CadenceDestination.STATISTICS },
                            icon = {
                                Icon(
                                    imageVector = if (currentDestination == CadenceDestination.STATISTICS)
                                        Icons.Rounded.BarChart else Icons.Outlined.BarChart,
                                    contentDescription = stringResource(R.string.nav_statistics),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_statistics)) },
                            modifier = Modifier.testTag("nav_statistics_item"),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        NavigationRailItem(
                            selected = currentDestination == CadenceDestination.TIMER,
                            onClick = { currentDestination = CadenceDestination.TIMER },
                            icon = {
                                Icon(
                                    imageVector = if (currentDestination == CadenceDestination.TIMER)
                                        Icons.Rounded.Timer else Icons.Outlined.Timer,
                                    contentDescription = stringResource(R.string.nav_timer),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_timer)) },
                            modifier = Modifier.testTag("rail_timer_item"),
                        )
                        NavigationRailItem(
                            selected = currentDestination == CadenceDestination.STATISTICS,
                            onClick = { currentDestination = CadenceDestination.STATISTICS },
                            icon = {
                                Icon(
                                    imageVector = if (currentDestination == CadenceDestination.STATISTICS)
                                        Icons.Rounded.BarChart else Icons.Outlined.BarChart,
                                    contentDescription = stringResource(R.string.nav_statistics),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_statistics)) },
                            modifier = Modifier.testTag("rail_statistics_item"),
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentDestination) {
                        CadenceDestination.TIMER -> {
                            TimerScreen(
                                uiState = timerState,
                                todaySummary = todaySummary,
                                onSelectMode = viewModel::selectMode,
                                onToggleStartPause = viewModel::toggleStartPause,
                                onReset = viewModel::resetTimer,
                                onSkipOrFinish = viewModel::skipOrFinishCurrentInterval,
                                onOpenSettings = { showSettingsSheet = true },
                                onUpdateActiveLabel = viewModel::updateActiveLabel,
                            )
                        }
                        CadenceDestination.STATISTICS -> {
                            StatisticsScreen(
                                stats = periodStats,
                                dailyGoalMinutes = timerState.settings.dailyGoalMinutes,
                                defaultFocusMinutes = timerState.settings.focusMinutes,
                                activeLabel = timerState.settings.activeLabel,
                                onSelectPeriod = viewModel::selectStatsPeriod,
                                onNavigateOffset = viewModel::navigateStatsPeriod,
                                onLogManualSession = viewModel::logManualFocusSession,
                                onDeleteSession = viewModel::deleteSession,
                                onStartFocusingClick = {
                                    currentDestination = CadenceDestination.TIMER
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        IntervalSettingsSheet(
            settings = timerState.settings,
            onUpdateSettings = viewModel::updateSettings,
            onApplyPreset = viewModel::applyPreset,
            onDismiss = { showSettingsSheet = false },
        )
    }
}
