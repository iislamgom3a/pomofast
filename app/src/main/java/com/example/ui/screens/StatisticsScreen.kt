package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FocusSessionEntity
import com.example.domain.PeriodStatistics
import com.example.domain.ProductivityStatsCalculator
import com.example.domain.StatsPeriod
import com.example.ui.components.MonthlyHeatmapCard
import com.example.ui.components.ProductivityBarChartCard
import com.example.ui.components.TagBreakdownCard
import kotlin.math.roundToInt

private val QUICK_LOG_TAGS = listOf("Deep Work", "Study", "Coding", "Writing", "Design", "Reading")

@Composable
fun StatisticsScreen(
    stats: PeriodStatistics,
    dailyGoalMinutes: Int,
    defaultFocusMinutes: Int,
    activeLabel: String,
    onSelectPeriod: (StatsPeriod) -> Unit,
    onNavigateOffset: (Int) -> Unit,
    onLogManualSession: (minutes: Int, label: String) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onStartFocusingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showManualLogDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    FilledTonalButton(
                        onClick = { showManualLogDialog = true },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("log_manual_session_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.stats_log_session_action),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // Period Selector (Daily / Weekly / Monthly / Annual)
            item(key = "period_tabs") {
                PeriodSelectorBar(
                    selectedPeriod = stats.period,
                    onSelectPeriod = onSelectPeriod,
                )
            }

            // Date Range Navigator
            item(key = "date_navigator") {
                PeriodDateNavigator(
                    title = stats.periodTitle,
                    canNavigateForward = stats.canNavigateForward,
                    onPrevious = { onNavigateOffset(-1) },
                    onNext = { onNavigateOffset(1) },
                )
            }

            // Bento Summary Metrics (2x2)
            item(key = "metrics_bento") {
                SummaryMetricsBento(stats = stats)
            }

            // Main Visualization per Period
            item(key = "primary_visualization") {
                when (stats.period) {
                    StatsPeriod.DAILY -> {
                        ProductivityBarChartCard(
                            title = stringResource(R.string.stats_chart_daily_title),
                            buckets = stats.primaryBuckets,
                        )
                    }
                    StatsPeriod.WEEKLY -> {
                        ProductivityBarChartCard(
                            title = stringResource(R.string.stats_chart_weekly_title),
                            buckets = stats.primaryBuckets,
                            goalReferenceMinutes = dailyGoalMinutes,
                            subtitleBadge = stringResource(
                                R.string.stats_active_days_format,
                                stats.activeDaysCount,
                            ),
                        )
                    }
                    StatsPeriod.MONTHLY -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MonthlyHeatmapCard(
                                leadingBlanks = stats.monthlyHeatmapLeadingBlanks,
                                cells = stats.monthlyHeatmapCells,
                            )
                            ProductivityBarChartCard(
                                title = stringResource(R.string.stats_chart_monthly_weeks_title),
                                buckets = stats.primaryBuckets,
                                subtitleBadge = stringResource(
                                    R.string.stats_active_days_format,
                                    stats.activeDaysCount,
                                ),
                            )
                        }
                    }
                    StatsPeriod.ANNUAL -> {
                        ProductivityBarChartCard(
                            title = stringResource(R.string.stats_chart_annual_title),
                            buckets = stats.primaryBuckets,
                            subtitleBadge = stats.peakLabel?.let {
                                stringResource(R.string.stats_peak_month_format, it)
                            } ?: stringResource(
                                R.string.stats_active_days_format,
                                stats.activeDaysCount,
                            ),
                        )
                    }
                }
            }

            // Tag Breakdown
            if (stats.tagBreakdown.isNotEmpty()) {
                item(key = "tag_breakdown") {
                    TagBreakdownCard(items = stats.tagBreakdown)
                }
            }

            // Empty State or Session Log
            if (stats.periodSessions.isEmpty()) {
                item(key = "empty_state") {
                    EmptyProductivityCard(onStartFocusingClick = onStartFocusingClick)
                }
            } else {
                item(key = "session_log_header") {
                    Text(
                        text = stringResource(R.string.stats_session_log_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                items(
                    items = stats.periodSessions.take(25),
                    key = { it.id },
                ) { session ->
                    SessionHistoryRow(
                        session = session,
                        onDelete = { onDeleteSession(session.id) },
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showManualLogDialog) {
        ManualLogSessionDialog(
            initialMinutes = defaultFocusMinutes,
            initialLabel = activeLabel,
            onDismiss = { showManualLogDialog = false },
            onSave = { mins, lbl ->
                onLogManualSession(mins, lbl)
                showManualLogDialog = false
            },
        )
    }
}

@Composable
private fun PeriodSelectorBar(
    selectedPeriod: StatsPeriod,
    onSelectPeriod: (StatsPeriod) -> Unit,
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
            val periods = listOf(
                StatsPeriod.DAILY to stringResource(R.string.stats_period_daily),
                StatsPeriod.WEEKLY to stringResource(R.string.stats_period_weekly),
                StatsPeriod.MONTHLY to stringResource(R.string.stats_period_monthly),
                StatsPeriod.ANNUAL to stringResource(R.string.stats_period_annual),
            )
            periods.forEach { (period, label) ->
                val isSelected = selectedPeriod == period
                val bg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(220),
                    label = "statsPeriodBg",
                )
                val fg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "statsPeriodFg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .clickable { onSelectPeriod(period) }
                        .testTag("stats_tab_${period.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodDateNavigator(
    title: String,
    canNavigateForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("period_prev_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_previous_period),
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = onNext,
                enabled = canNavigateForward,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("period_next_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_period),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricsBento(stats: PeriodStatistics) {
    val deltaText = when (val delta = stats.deltaPercentVsPrevious) {
        null -> null
        0 -> stringResource(R.string.stats_vs_previous_flat)
        in 1..Int.MAX_VALUE -> stringResource(R.string.stats_vs_previous_up, delta)
        else -> stringResource(R.string.stats_vs_previous_down, delta)
    }

    val streakText = if (stats.currentStreakDays == 1) {
        stringResource(R.string.stats_streak_day_single)
    } else {
        stringResource(R.string.stats_streak_days_format, stats.currentStreakDays)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricTile(
                icon = Icons.Rounded.Schedule,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                label = stringResource(R.string.stats_total_focus),
                value = ProductivityStatsCalculator.formatMinutesReadable(stats.totalFocusMinutes),
                footnote = deltaText ?: stringResource(R.string.stats_active_days_format, stats.activeDaysCount),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                icon = Icons.Rounded.TaskAlt,
                iconTint = MaterialTheme.colorScheme.secondary,
                iconBg = MaterialTheme.colorScheme.secondaryContainer,
                label = stringResource(R.string.stats_sessions_completed),
                value = stats.sessionCount.toString(),
                footnote = stringResource(R.string.stats_avg_session_format, stats.averageSessionMinutes),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricTile(
                icon = Icons.Rounded.LocalFireDepartment,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                label = stringResource(R.string.stats_current_streak),
                value = streakText,
                footnote = stringResource(R.string.stats_best_streak_format, stats.bestStreakDays),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                icon = Icons.Rounded.TrackChanges,
                iconTint = MaterialTheme.colorScheme.tertiary,
                iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                label = stringResource(R.string.stats_daily_goal_rate),
                value = "${stats.goalCompletionPercent}%",
                footnote = stringResource(R.string.stats_active_days_format, stats.activeDaysCount),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String,
    footnote: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            Text(
                text = footnote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyProductivityCard(
    onStartFocusingClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_focus_empty_1795055675621),
                contentDescription = stringResource(R.string.cd_empty_desk_illustration),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(18.dp)),
            )

            Text(
                text = stringResource(R.string.stats_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.stats_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = onStartFocusingClick,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(48.dp)
                    .testTag("empty_start_focusing_button"),
            ) {
                Text(
                    text = stringResource(R.string.stats_empty_cta),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SessionHistoryRow(
    session: FocusSessionEntity,
    onDelete: () -> Unit,
) {
    val minutes = ((session.actualDurationSeconds + 30) / 60).coerceAtLeast(1)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Column {
                    Text(
                        text = session.label.ifBlank { "Deep Work" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = ProductivityStatsCalculator.formatSessionTime(session.completedAtMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = ProductivityStatsCalculator.formatMinutesReadable(minutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_session_${session.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_session),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualLogSessionDialog(
    initialMinutes: Int,
    initialLabel: String,
    onDismiss: () -> Unit,
    onSave: (minutes: Int, label: String) -> Unit,
) {
    var minutes by remember { mutableIntStateOf(initialMinutes.coerceIn(5, 120)) }
    var label by remember { mutableStateOf(initialLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.manual_log_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.manual_log_duration_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = ProductivityStatsCalculator.formatMinutesReadable(minutes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = { minutes = (it / 5f).roundToInt() * 5 },
                        valueRange = 5f..120f,
                        steps = 22,
                        modifier = Modifier.testTag("manual_log_duration_slider"),
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    QUICK_LOG_TAGS.forEach { tag ->
                        FilterChip(
                            selected = label.equals(tag, ignoreCase = true),
                            onClick = { label = tag },
                            label = { Text(tag) },
                        )
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { if (it.length <= 32) label = it },
                    label = { Text(stringResource(R.string.manual_log_tag_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_log_tag_input"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(minutes.coerceAtLeast(5), label) },
                modifier = Modifier.testTag("manual_log_save_button"),
            ) {
                Text(stringResource(R.string.manual_log_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
