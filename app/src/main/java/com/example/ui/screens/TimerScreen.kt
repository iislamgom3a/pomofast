package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TimerMode
import com.example.domain.TodaySummary
import com.example.ui.components.ChronometerDial
import com.example.ui.viewmodel.TimerUiState

private val PRESET_TAGS = listOf("Deep Work", "Study", "Coding", "Writing", "Design", "Reading")

@Composable
fun TimerScreen(
    uiState: TimerUiState,
    todaySummary: TodaySummary,
    onSelectMode: (TimerMode) -> Unit,
    onToggleStartPause: () -> Unit,
    onReset: () -> Unit,
    onSkipOrFinish: () -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateActiveLabel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTagDialog by remember { mutableStateOf(false) }

    val targetAccent = when (uiState.mode) {
        TimerMode.FOCUS -> MaterialTheme.colorScheme.primary
        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
    }
    val targetAccentContainer = when (uiState.mode) {
        TimerMode.FOCUS -> MaterialTheme.colorScheme.primaryContainer
        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val accentColor by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(320),
        label = "screenAccent",
    )
    val accentContainer by animateColorAsState(
        targetValue = targetAccentContainer,
        animationSpec = tween(320),
        label = "screenAccentContainer",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.09f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.28f),
                        radius = size.maxDimension * 0.65f,
                    )
                )
            },
    ) {
        val isWide = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = if (isWide) 840.dp else 560.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header Row: Brand wordmark + Focus tag chip + Interval Settings trigger
                TimerHeaderRow(
                    activeLabel = uiState.settings.activeLabel,
                    onTagClick = { showTagDialog = true },
                    onSettingsClick = onOpenSettings,
                )

                // Mode Switcher Pill
                ModeSelectorBar(
                    selectedMode = uiState.mode,
                    accentColor = accentColor,
                    onSelectMode = onSelectMode,
                )

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            ChronometerDial(
                                formattedTime = uiState.formattedTime,
                                progress = uiState.progress,
                                mode = uiState.mode,
                                isRunning = uiState.isRunning,
                                hasStarted = uiState.hasStartedCurrentInterval,
                                completedFocusInCycle = uiState.completedFocusInCycle,
                                sessionsBeforeLongBreak = uiState.settings.sessionsBeforeLongBreak,
                                accentColor = accentColor,
                                accentContainerColor = accentContainer,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TimerControlsRow(
                                uiState = uiState,
                                accentColor = accentColor,
                                onReset = onReset,
                                onToggleStartPause = onToggleStartPause,
                                onSkipOrFinish = onSkipOrFinish,
                            )

                            QuickIntervalSummaryRow(
                                focusMin = uiState.settings.focusMinutes,
                                shortBreakMin = uiState.settings.shortBreakMinutes,
                                longBreakMin = uiState.settings.longBreakMinutes,
                                activeMode = uiState.mode,
                                onCustomizeClick = onOpenSettings,
                            )

                            TodayProgressCard(
                                todaySummary = todaySummary,
                                dailyGoalMinutes = uiState.settings.dailyGoalMinutes,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(2.dp))

                    ChronometerDial(
                        formattedTime = uiState.formattedTime,
                        progress = uiState.progress,
                        mode = uiState.mode,
                        isRunning = uiState.isRunning,
                        hasStarted = uiState.hasStartedCurrentInterval,
                        completedFocusInCycle = uiState.completedFocusInCycle,
                        sessionsBeforeLongBreak = uiState.settings.sessionsBeforeLongBreak,
                        accentColor = accentColor,
                        accentContainerColor = accentContainer,
                    )

                    TimerControlsRow(
                        uiState = uiState,
                        accentColor = accentColor,
                        onReset = onReset,
                        onToggleStartPause = onToggleStartPause,
                        onSkipOrFinish = onSkipOrFinish,
                    )

                    QuickIntervalSummaryRow(
                        focusMin = uiState.settings.focusMinutes,
                        shortBreakMin = uiState.settings.shortBreakMinutes,
                        longBreakMin = uiState.settings.longBreakMinutes,
                        activeMode = uiState.mode,
                        onCustomizeClick = onOpenSettings,
                    )

                    TodayProgressCard(
                        todaySummary = todaySummary,
                        dailyGoalMinutes = uiState.settings.dailyGoalMinutes,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showTagDialog) {
        FocusTagDialog(
            currentLabel = uiState.settings.activeLabel,
            onDismiss = { showTagDialog = false },
            onSelectLabel = { chosen ->
                onUpdateActiveLabel(chosen)
                showTagDialog = false
            },
        )
    }
}

@Composable
private fun TimerHeaderRow(
    activeLabel: String,
    onTagClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onTagClick)
                    .testTag("focus_tag_chip"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = activeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        IconButton(
            onClick = onSettingsClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier
                .size(48.dp)
                .testTag("open_settings_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(R.string.cd_open_settings),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ModeSelectorBar(
    selectedMode: TimerMode,
    accentColor: Color,
    onSelectMode: (TimerMode) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val items = listOf(
                TimerMode.FOCUS to stringResource(R.string.mode_focus),
                TimerMode.SHORT_BREAK to stringResource(R.string.mode_short_break),
                TimerMode.LONG_BREAK to stringResource(R.string.mode_long_break),
            )
            items.forEach { (mode, label) ->
                val isSelected = selectedMode == mode
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else Color.Transparent,
                    animationSpec = tween(240),
                    label = "modePillBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(240),
                    label = "modePillText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .clickable { onSelectMode(mode) }
                        .testTag("mode_${mode.storageKey.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerControlsRow(
    uiState: TimerUiState,
    accentColor: Color,
    onReset: () -> Unit,
    onToggleStartPause: () -> Unit,
    onSkipOrFinish: () -> Unit,
) {
    val canLogEarly = uiState.mode == TimerMode.FOCUS && uiState.elapsedSeconds >= 60

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onReset,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .size(54.dp)
                .testTag("timer_reset_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.cd_reset_timer),
                modifier = Modifier.size(24.dp),
            )
        }

        Button(
            onClick = onToggleStartPause,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .height(62.dp)
                .widthIn(min = 168.dp)
                .testTag("timer_play_pause_button"),
        ) {
            Icon(
                imageVector = if (uiState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    uiState.isRunning -> stringResource(R.string.timer_action_pause)
                    uiState.hasStartedCurrentInterval -> stringResource(R.string.timer_action_resume)
                    else -> stringResource(R.string.timer_action_start)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        IconButton(
            onClick = onSkipOrFinish,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (canLogEarly) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
                contentColor = if (canLogEarly) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .size(54.dp)
                .testTag("timer_skip_button"),
        ) {
            Icon(
                imageVector = if (canLogEarly) Icons.Rounded.CheckCircleOutline else Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.cd_skip_interval),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun QuickIntervalSummaryRow(
    focusMin: Int,
    shortBreakMin: Int,
    longBreakMin: Int,
    activeMode: TimerMode,
    onCustomizeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IntervalMiniCard(
            label = stringResource(R.string.mode_focus),
            value = "${focusMin}m",
            highlighted = activeMode == TimerMode.FOCUS,
            accent = MaterialTheme.colorScheme.primary,
            onClick = onCustomizeClick,
            modifier = Modifier.weight(1f).testTag("quick_interval_focus"),
        )
        IntervalMiniCard(
            label = stringResource(R.string.mode_short_break),
            value = "${shortBreakMin}m",
            highlighted = activeMode == TimerMode.SHORT_BREAK,
            accent = MaterialTheme.colorScheme.secondary,
            onClick = onCustomizeClick,
            modifier = Modifier.weight(1f).testTag("quick_interval_short"),
        )
        IntervalMiniCard(
            label = stringResource(R.string.mode_long_break),
            value = "${longBreakMin}m",
            highlighted = activeMode == TimerMode.LONG_BREAK,
            accent = MaterialTheme.colorScheme.tertiary,
            onClick = onCustomizeClick,
            modifier = Modifier.weight(1f).testTag("quick_interval_long"),
        )
    }
}

@Composable
private fun IntervalMiniCard(
    label: String,
    value: String,
    highlighted: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = if (highlighted) 2.dp else 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (highlighted) accent else MaterialTheme.colorScheme.outline),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TodayProgressCard(
    todaySummary: TodaySummary,
    dailyGoalMinutes: Int,
) {
    val progress = if (dailyGoalMinutes > 0) {
        (todaySummary.focusMinutes.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.timer_today_progress_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.timer_today_sessions_format,
                        todaySummary.sessionCount,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.timer_today_progress_format,
                        todaySummary.focusMinutes,
                        dailyGoalMinutes,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusTagDialog(
    currentLabel: String,
    onDismiss: () -> Unit,
    onSelectLabel: (String) -> Unit,
) {
    var customText by remember { mutableStateOf(currentLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.timer_tag_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PRESET_TAGS.forEach { tag ->
                        FilterChip(
                            selected = customText.equals(tag, ignoreCase = true),
                            onClick = { customText = tag },
                            label = { Text(tag) },
                        )
                    }
                }

                OutlinedTextField(
                    value = customText,
                    onValueChange = { if (it.length <= 32) customText = it },
                    label = { Text(stringResource(R.string.timer_tag_custom_placeholder)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_tag_input"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSelectLabel(customText.trim().ifBlank { "Deep Work" }) },
                modifier = Modifier.testTag("apply_tag_button"),
            ) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
