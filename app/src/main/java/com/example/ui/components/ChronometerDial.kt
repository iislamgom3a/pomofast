package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TimerMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChronometerDial(
    formattedTime: String,
    progress: Float,
    mode: TimerMode,
    isRunning: Boolean,
    hasStarted: Boolean,
    completedFocusInCycle: Int,
    sessionsBeforeLongBreak: Int,
    accentColor: Color,
    accentContainerColor: Color,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "dialProgress",
    )
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 320),
        label = "dialAccent",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val majorTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val faceFillColor = MaterialTheme.colorScheme.surface

    val statusText = when {
        isRunning -> stringResource(R.string.timer_state_running)
        hasStarted -> stringResource(R.string.timer_state_paused)
        else -> stringResource(R.string.timer_state_ready)
    }

    val subtitleText = when (mode) {
        TimerMode.FOCUS -> stringResource(R.string.timer_subtitle_focus)
        TimerMode.SHORT_BREAK -> stringResource(R.string.timer_subtitle_short_break)
        TimerMode.LONG_BREAK -> stringResource(R.string.timer_subtitle_long_break)
    }

    Box(
        modifier = modifier
            .widthIn(max = 310.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.048f
            val bezelPadding = size.minDimension * 0.07f
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.minDimension / 2f) - bezelPadding
            val innerFaceRadius = outerRadius - strokeWidth * 1.15f

            // Subtle warm atmospheric halo behind dial
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = if (isRunning) 0.16f else 0.08f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.52f,
                ),
                radius = size.minDimension * 0.52f,
                center = center,
            )

            // Crisp watch face circle
            drawCircle(
                color = faceFillColor,
                radius = innerFaceRadius,
                center = center,
            )

            // 60 Swiss bezel tick marks
            val tickOuter = outerRadius + bezelPadding * 0.45f
            for (i in 0 until 60) {
                val angleRad = (i * 6.0 - 90.0) * PI / 180.0
                val isMajor = i % 5 == 0
                val tickLength = if (isMajor) bezelPadding * 0.48f else bezelPadding * 0.24f
                val start = Offset(
                    x = center.x + (tickOuter - tickLength) * cos(angleRad).toFloat(),
                    y = center.y + (tickOuter - tickLength) * sin(angleRad).toFloat(),
                )
                val end = Offset(
                    x = center.x + tickOuter * cos(angleRad).toFloat(),
                    y = center.y + tickOuter * sin(angleRad).toFloat(),
                )
                val tickProgress = i / 60f
                val resolvedTickColor = when {
                    tickProgress <= animatedProgress && animatedProgress > 0f ->
                        animatedAccent.copy(alpha = if (isMajor) 0.9f else 0.5f)
                    isMajor -> majorTickColor
                    else -> tickColor
                }
                drawLine(
                    color = resolvedTickColor,
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.25.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Base arc track
            val arcTopLeft = Offset(center.x - outerRadius, center.y - outerRadius)
            val arcSize = Size(outerRadius * 2f, outerRadius * 2f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Animated sweep arc
            val sweep = animatedProgress * 360f
            if (sweep > 0.4f) {
                drawArc(
                    color = animatedAccent,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // Tactile indicator knob at arc head
            val knobAngleRad = (-90.0 + sweep) * PI / 180.0
            val knobCenter = Offset(
                x = center.x + outerRadius * cos(knobAngleRad).toFloat(),
                y = center.y + outerRadius * sin(knobAngleRad).toFloat(),
            )
            drawCircle(
                color = faceFillColor,
                radius = strokeWidth * 0.72f,
                center = knobCenter,
            )
            drawCircle(
                color = animatedAccent,
                radius = strokeWidth * 0.45f,
                center = knobCenter,
            )
        }

        // Center content: status pill, JetBrains Mono clock, subtitle, cycle dots
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Surface(
                color = accentContainerColor,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = statusText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = animatedAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Cycle progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val safeTotal = sessionsBeforeLongBreak.coerceIn(2, 8)
                for (index in 0 until safeTotal) {
                    val isCompleted = index < completedFocusInCycle
                    val isActiveFocusDot = mode == TimerMode.FOCUS && index == completedFocusInCycle
                    val dotColor = when {
                        isCompleted -> animatedAccent
                        isActiveFocusDot -> animatedAccent.copy(alpha = 0.42f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isActiveFocusDot) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
            }
        }
    }
}
