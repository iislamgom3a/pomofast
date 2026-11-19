package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ChartBucket
import com.example.domain.HeatmapDayCell
import com.example.domain.ProductivityStatsCalculator
import com.example.domain.TagBreakdownItem

@Composable
fun ProductivityBarChartCard(
    title: String,
    buckets: List<ChartBucket>,
    goalReferenceMinutes: Int? = null,
    subtitleBadge: String? = null,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember(buckets) {
        mutableIntStateOf(
            buckets.indexOfLast { it.focusMinutes > 0 }
                .takeIf { it >= 0 }
                ?: buckets.indexOfFirst { it.isCurrent }.takeIf { it >= 0 }
                ?: -1
        )
    }

    var animationTriggered by remember(buckets) { mutableStateOf(false) }
    LaunchedEffect(buckets) { animationTriggered = true }
    val growthFactor by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "barGrowth",
    )

    val maxBucketMinutes = buckets.maxOfOrNull { it.focusMinutes } ?: 0
    val scaleMax = maxOf(
        maxBucketMinutes,
        goalReferenceMinutes ?: 0,
        30,
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryMuted = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val emptyBarColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val goalLineColor = MaterialTheme.colorScheme.secondary

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val selectedBucket = buckets.getOrNull(selectedIndex)
                if (selectedBucket != null && selectedBucket.focusMinutes > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = "${selectedBucket.detailLabel} · ${
                                ProductivityStatsCalculator.formatMinutesReadable(selectedBucket.focusMinutes)
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                } else if (subtitleBadge != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = subtitleBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (goalReferenceMinutes != null && goalReferenceMinutes > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(goalLineColor),
                    )
                    Text(
                        text = stringResource(R.string.stats_goal_line_label, goalReferenceMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Interactive Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .pointerInput(buckets) {
                        detectTapGestures { tapOffset ->
                            if (buckets.isNotEmpty() && size.width > 0) {
                                val slotWidth = size.width.toFloat() / buckets.size
                                val tapped = (tapOffset.x / slotWidth).toInt()
                                    .coerceIn(0, buckets.lastIndex)
                                selectedIndex = tapped
                            }
                        }
                    },
            ) {
                if (buckets.isEmpty()) return@Canvas
                val chartHeight = size.height
                val chartWidth = size.width

                // 3 subtle horizontal grid lines
                for (step in 0..2) {
                    val y = chartHeight * (step / 2f)
                    drawLine(
                        color = gridColor.copy(alpha = 0.7f),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                // Dashed daily goal reference line
                if (goalReferenceMinutes != null && goalReferenceMinutes > 0) {
                    val goalY = chartHeight - (goalReferenceMinutes.toFloat() / scaleMax) * chartHeight
                    drawLine(
                        color = goalLineColor.copy(alpha = 0.8f),
                        start = Offset(0f, goalY),
                        end = Offset(chartWidth, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    )
                }

                val slotWidth = chartWidth / buckets.size
                val barWidth = (slotWidth * 0.56f).coerceIn(8.dp.toPx(), 28.dp.toPx())
                val cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f)

                buckets.forEachIndexed { index, bucket ->
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val isSelected = index == selectedIndex
                    val rawRatio = (bucket.focusMinutes.toFloat() / scaleMax).coerceIn(0f, 1f)
                    val animatedHeight = if (bucket.focusMinutes > 0) {
                        (rawRatio * chartHeight * growthFactor).coerceAtLeast(8.dp.toPx())
                    } else {
                        5.dp.toPx()
                    }
                    val top = chartHeight - animatedHeight
                    val fillColor = when {
                        bucket.focusMinutes <= 0 -> emptyBarColor
                        isSelected || bucket.isCurrent -> primaryColor
                        else -> primaryMuted
                    }

                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, animatedHeight),
                        cornerRadius = cornerRadius,
                    )
                }
            }

            // X-Axis Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                buckets.forEachIndexed { index, bucket ->
                    val isHighlighted = index == selectedIndex || bucket.isCurrent
                    Text(
                        text = bucket.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHighlighted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyHeatmapCard(
    leadingBlanks: Int,
    cells: List<HeatmapDayCell>,
    modifier: Modifier = Modifier,
) {
    var selectedCell by remember(cells) {
        mutableStateOf(
            cells.firstOrNull { it.isToday }
                ?: cells.lastOrNull { it.focusMinutes > 0 }
        )
    }

    val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
    val totalSlots = leadingBlanks + cells.size
    val rowCount = (totalSlots + 6) / 7

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.stats_chart_monthly_heatmap_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                selectedCell?.let { cell ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = "${cell.detailLabel} · ${
                                ProductivityStatsCalculator.formatMinutesReadable(cell.focusMinutes)
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Weekday Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Calendar Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (col in 0 until 7) {
                            val slotIdx = row * 7 + col
                            val cellIdx = slotIdx - leadingBlanks
                            val cell = cells.getOrNull(cellIdx)
                            if (cell == null) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val cellColor = when (cell.intensityLevel) {
                                    0 -> MaterialTheme.colorScheme.surfaceContainer
                                    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                                    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.74f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                val isSelected = selectedCell?.dayOfMonth == cell.dayOfMonth
                                val textColor = if (cell.intensityLevel >= 3) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellColor)
                                        .then(
                                            if (isSelected || cell.isToday) {
                                                Modifier.border(
                                                    width = if (isSelected) 2.dp else 1.5.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                                    else MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(10.dp),
                                                )
                                            } else Modifier
                                        )
                                        .clickable { selectedCell = cell },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = cell.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp),
                )
                listOf(0f, 0.24f, 0.48f, 0.74f, 1f).forEach { alpha ->
                    val swatch = if (alpha == 0f) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(swatch),
                    )
                }
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
fun TagBreakdownCard(
    items: List<TagBreakdownItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val barColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.stats_tag_breakdown_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            items.forEachIndexed { index, item ->
                val color = barColors[index % barColors.size]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${ProductivityStatsCalculator.formatMinutesReadable(item.focusMinutes)} · ${item.sessionCount}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.fraction.coerceIn(0.05f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(color),
                        )
                    }
                }
            }
        }
    }
}
