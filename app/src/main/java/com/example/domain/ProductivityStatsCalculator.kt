package com.example.domain

import com.example.data.local.FocusSessionEntity
import com.example.data.local.TimerMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class StatsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    ANNUAL,
}

data class ChartBucket(
    val key: String,
    val shortLabel: String,
    val detailLabel: String,
    val focusMinutes: Int,
    val sessionCount: Int,
    val isCurrent: Boolean = false,
)

data class HeatmapDayCell(
    val dayOfMonth: Int,
    val focusMinutes: Int,
    val sessionCount: Int,
    val intensityLevel: Int, // 0..4
    val isToday: Boolean,
    val detailLabel: String,
)

data class TagBreakdownItem(
    val label: String,
    val focusMinutes: Int,
    val sessionCount: Int,
    val fraction: Float,
)

data class PeriodStatistics(
    val period: StatsPeriod,
    val periodTitle: String,
    val canNavigateForward: Boolean,
    val totalFocusMinutes: Int,
    val sessionCount: Int,
    val averageSessionMinutes: Int,
    val activeDaysCount: Int,
    val goalCompletionPercent: Int,
    val deltaPercentVsPrevious: Int?,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val primaryBuckets: List<ChartBucket>,
    val monthlyHeatmapLeadingBlanks: Int = 0,
    val monthlyHeatmapCells: List<HeatmapDayCell> = emptyList(),
    val peakLabel: String? = null,
    val tagBreakdown: List<TagBreakdownItem> = emptyList(),
    val periodSessions: List<FocusSessionEntity> = emptyList(),
)

data class TodaySummary(
    val focusMinutes: Int = 0,
    val sessionCount: Int = 0,
)

object ProductivityStatsCalculator {

    private val dayTitleFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val shortMonthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val monthShortFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val monthFullFormat = SimpleDateFormat("MMMM", Locale.getDefault())
    private val weekdayShortFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun formatMinutesReadable(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0m"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours == 0 -> "${mins}m"
            mins == 0 -> "${hours}h"
            else -> "${hours}h ${mins}m"
        }
    }

    fun formatSessionTime(epochMillis: Long): String = timeFormat.format(epochMillis)

    fun computeTodaySummary(
        allSessions: List<FocusSessionEntity>,
        nowMillis: Long = System.currentTimeMillis(),
    ): TodaySummary {
        val focusOnly = allSessions.filter { it.sessionType == TimerMode.FOCUS.storageKey }
        val start = startOfDay(nowMillis)
        val end = addDays(start, 1)
        val todaySessions = focusOnly.filter { it.completedAtMillis in start until end }
        val totalSeconds = todaySessions.sumOf { it.actualDurationSeconds }
        return TodaySummary(
            focusMinutes = secondsToRoundedMinutes(totalSeconds),
            sessionCount = todaySessions.size,
        )
    }

    fun computePeriodStatistics(
        allSessions: List<FocusSessionEntity>,
        period: StatsPeriod,
        offset: Int, // 0 = current period, -1 = previous, etc.
        dailyGoalMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): PeriodStatistics {
        val safeOffset = offset.coerceAtMost(0)
        val focusOnly = allSessions.filter { it.sessionType == TimerMode.FOCUS.storageKey }
        val (currentStreak, bestStreak) = computeStreaks(focusOnly, nowMillis)

        return when (period) {
            StatsPeriod.DAILY -> computeDaily(
                focusOnly, safeOffset, dailyGoalMinutes, currentStreak, bestStreak, nowMillis
            )
            StatsPeriod.WEEKLY -> computeWeekly(
                focusOnly, safeOffset, dailyGoalMinutes, currentStreak, bestStreak, nowMillis
            )
            StatsPeriod.MONTHLY -> computeMonthly(
                focusOnly, safeOffset, dailyGoalMinutes, currentStreak, bestStreak, nowMillis
            )
            StatsPeriod.ANNUAL -> computeAnnual(
                focusOnly, safeOffset, dailyGoalMinutes, currentStreak, bestStreak, nowMillis
            )
        }
    }

    private fun computeDaily(
        focusSessions: List<FocusSessionEntity>,
        offset: Int,
        dailyGoalMinutes: Int,
        currentStreak: Int,
        bestStreak: Int,
        nowMillis: Long,
    ): PeriodStatistics {
        val dayStart = addDays(startOfDay(nowMillis), offset)
        val dayEnd = addDays(dayStart, 1)
        val prevStart = addDays(dayStart, -1)

        val currentSessions = focusSessions.filter { it.completedAtMillis in dayStart until dayEnd }
        val prevSessions = focusSessions.filter { it.completedAtMillis in prevStart until dayStart }

        val totalMinutes = secondsToRoundedMinutes(currentSessions.sumOf { it.actualDurationSeconds })
        val prevMinutes = secondsToRoundedMinutes(prevSessions.sumOf { it.actualDurationSeconds })

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentHourBucket = nowCal.get(Calendar.HOUR_OF_DAY) / 2

        // 12 two-hour buckets across the day (12a, 2a, ..., 10p) for readable mobile bar charts
        val bucketSeconds = IntArray(12)
        val bucketCounts = IntArray(12)
        val tempCal = Calendar.getInstance()
        for (session in currentSessions) {
            tempCal.timeInMillis = session.completedAtMillis
            val idx = (tempCal.get(Calendar.HOUR_OF_DAY) / 2).coerceIn(0, 11)
            bucketSeconds[idx] += session.actualDurationSeconds
            bucketCounts[idx] += 1
        }

        val labels = listOf("12a", "2a", "4a", "6a", "8a", "10a", "12p", "2p", "4p", "6p", "8p", "10p")
        val detailRanges = listOf(
            "12–2 AM", "2–4 AM", "4–6 AM", "6–8 AM", "8–10 AM", "10–12 PM",
            "12–2 PM", "2–4 PM", "4–6 PM", "6–8 PM", "8–10 PM", "10–12 AM"
        )
        val buckets = (0 until 12).map { i ->
            ChartBucket(
                key = "h_$i",
                shortLabel = if (i % 2 == 0) labels[i] else "",
                detailLabel = detailRanges[i],
                focusMinutes = secondsToRoundedMinutes(bucketSeconds[i]),
                sessionCount = bucketCounts[i],
                isCurrent = (offset == 0 && i == currentHourBucket),
            )
        }

        val goalPct = if (dailyGoalMinutes > 0) {
            ((totalMinutes.toFloat() / dailyGoalMinutes) * 100f).roundToInt().coerceAtMost(999)
        } else 0

        val title = when (offset) {
            0 -> "Today · ${shortMonthDayFormat.format(dayStart)}"
            -1 -> "Yesterday · ${shortMonthDayFormat.format(dayStart)}"
            else -> dayTitleFormat.format(dayStart)
        }

        return PeriodStatistics(
            period = StatsPeriod.DAILY,
            periodTitle = title,
            canNavigateForward = offset < 0,
            totalFocusMinutes = totalMinutes,
            sessionCount = currentSessions.size,
            averageSessionMinutes = if (currentSessions.isEmpty()) 0 else (totalMinutes / currentSessions.size),
            activeDaysCount = if (currentSessions.isNotEmpty()) 1 else 0,
            goalCompletionPercent = goalPct,
            deltaPercentVsPrevious = computeDeltaPercent(totalMinutes, prevMinutes),
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            primaryBuckets = buckets,
            tagBreakdown = computeTagBreakdown(currentSessions),
            periodSessions = currentSessions.sortedByDescending { it.completedAtMillis },
        )
    }

    private fun computeWeekly(
        focusSessions: List<FocusSessionEntity>,
        offset: Int,
        dailyGoalMinutes: Int,
        currentStreak: Int,
        bestStreak: Int,
        nowMillis: Long,
    ): PeriodStatistics {
        val weekStart = addDays(startOfWeekMonday(nowMillis), offset * 7)
        val weekEnd = addDays(weekStart, 7)
        val prevStart = addDays(weekStart, -7)

        val currentSessions = focusSessions.filter { it.completedAtMillis in weekStart until weekEnd }
        val prevSessions = focusSessions.filter { it.completedAtMillis in prevStart until weekStart }

        val totalMinutes = secondsToRoundedMinutes(currentSessions.sumOf { it.actualDurationSeconds })
        val prevMinutes = secondsToRoundedMinutes(prevSessions.sumOf { it.actualDurationSeconds })

        val todayKey = dayKey(nowMillis)
        val buckets = (0 until 7).map { dayOffset ->
            val dStart = addDays(weekStart, dayOffset)
            val dEnd = addDays(dStart, 1)
            val daySessions = currentSessions.filter { it.completedAtMillis in dStart until dEnd }
            val mins = secondsToRoundedMinutes(daySessions.sumOf { it.actualDurationSeconds })
            ChartBucket(
                key = "w_$dayOffset",
                shortLabel = weekdayShortFormat.format(dStart).take(3),
                detailLabel = "${weekdayShortFormat.format(dStart)}, ${shortMonthDayFormat.format(dStart)}",
                focusMinutes = mins,
                sessionCount = daySessions.size,
                isCurrent = dayKey(dStart) == todayKey,
            )
        }

        val activeDays = buckets.count { it.sessionCount > 0 }
        val weeklyGoal = dailyGoalMinutes * 7
        val goalPct = if (weeklyGoal > 0) {
            ((totalMinutes.toFloat() / weeklyGoal) * 100f).roundToInt().coerceAtMost(999)
        } else 0

        val lastDayOfWeek = addDays(weekStart, 6)
        val title = "${shortMonthDayFormat.format(weekStart)} – ${shortMonthDayFormat.format(lastDayOfWeek)}"

        return PeriodStatistics(
            period = StatsPeriod.WEEKLY,
            periodTitle = title,
            canNavigateForward = offset < 0,
            totalFocusMinutes = totalMinutes,
            sessionCount = currentSessions.size,
            averageSessionMinutes = if (currentSessions.isEmpty()) 0 else (totalMinutes / currentSessions.size),
            activeDaysCount = activeDays,
            goalCompletionPercent = goalPct,
            deltaPercentVsPrevious = computeDeltaPercent(totalMinutes, prevMinutes),
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            primaryBuckets = buckets,
            tagBreakdown = computeTagBreakdown(currentSessions),
            periodSessions = currentSessions.sortedByDescending { it.completedAtMillis },
        )
    }

    private fun computeMonthly(
        focusSessions: List<FocusSessionEntity>,
        offset: Int,
        dailyGoalMinutes: Int,
        currentStreak: Int,
        bestStreak: Int,
        nowMillis: Long,
    ): PeriodStatistics {
        val monthCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, offset)
        }
        val monthStart = monthCal.timeInMillis
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
        // Monday-based leading blanks (Mon=0..Sun=6)
        val leadingBlanks = (firstDayOfWeek + 5) % 7

        val nextMonthCal = (monthCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val monthEnd = nextMonthCal.timeInMillis

        val prevMonthCal = (monthCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevStart = prevMonthCal.timeInMillis

        val currentSessions = focusSessions.filter { it.completedAtMillis in monthStart until monthEnd }
        val prevSessions = focusSessions.filter { it.completedAtMillis in prevStart until monthStart }

        val totalMinutes = secondsToRoundedMinutes(currentSessions.sumOf { it.actualDurationSeconds })
        val prevMinutes = secondsToRoundedMinutes(prevSessions.sumOf { it.actualDurationSeconds })

        val daySeconds = IntArray(daysInMonth + 1)
        val dayCounts = IntArray(daysInMonth + 1)
        val tempCal = Calendar.getInstance()
        for (session in currentSessions) {
            tempCal.timeInMillis = session.completedAtMillis
            val dom = tempCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
            daySeconds[dom] += session.actualDurationSeconds
            dayCounts[dom] += 1
        }

        val todayKey = dayKey(nowMillis)
        val heatmapCells = (1..daysInMonth).map { dom ->
            val cellCal = (monthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dom) }
            val mins = secondsToRoundedMinutes(daySeconds[dom])
            val ratio = if (dailyGoalMinutes > 0) mins.toFloat() / dailyGoalMinutes else 0f
            val level = when {
                mins <= 0 -> 0
                ratio < 0.35f -> 1
                ratio < 0.70f -> 2
                ratio < 1.0f -> 3
                else -> 4
            }
            HeatmapDayCell(
                dayOfMonth = dom,
                focusMinutes = mins,
                sessionCount = dayCounts[dom],
                intensityLevel = level,
                isToday = dayKey(cellCal.timeInMillis) == todayKey,
                detailLabel = shortMonthDayFormat.format(cellCal.timeInMillis),
            )
        }

        // Group month days into 4-5 week buckets for trend bar chart
        val numWeekBuckets = ((daysInMonth - 1) / 7) + 1
        val weekBuckets = (0 until numWeekBuckets).map { w ->
            val startDom = w * 7 + 1
            val endDom = minOf((w + 1) * 7, daysInMonth)
            var secs = 0
            var cnt = 0
            for (d in startDom..endDom) {
                secs += daySeconds[d]
                cnt += dayCounts[d]
            }
            ChartBucket(
                key = "mw_$w",
                shortLabel = "W${w + 1}",
                detailLabel = "${monthShortFormat.format(monthStart)} $startDom–$endDom",
                focusMinutes = secondsToRoundedMinutes(secs),
                sessionCount = cnt,
                isCurrent = false,
            )
        }

        val activeDays = heatmapCells.count { it.sessionCount > 0 }
        val monthlyGoal = dailyGoalMinutes * daysInMonth
        val goalPct = if (monthlyGoal > 0) {
            ((totalMinutes.toFloat() / monthlyGoal) * 100f).roundToInt().coerceAtMost(999)
        } else 0

        return PeriodStatistics(
            period = StatsPeriod.MONTHLY,
            periodTitle = monthYearFormat.format(monthStart),
            canNavigateForward = offset < 0,
            totalFocusMinutes = totalMinutes,
            sessionCount = currentSessions.size,
            averageSessionMinutes = if (currentSessions.isEmpty()) 0 else (totalMinutes / currentSessions.size),
            activeDaysCount = activeDays,
            goalCompletionPercent = goalPct,
            deltaPercentVsPrevious = computeDeltaPercent(totalMinutes, prevMinutes),
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            primaryBuckets = weekBuckets,
            monthlyHeatmapLeadingBlanks = leadingBlanks,
            monthlyHeatmapCells = heatmapCells,
            tagBreakdown = computeTagBreakdown(currentSessions),
            periodSessions = currentSessions.sortedByDescending { it.completedAtMillis },
        )
    }

    private fun computeAnnual(
        focusSessions: List<FocusSessionEntity>,
        offset: Int,
        dailyGoalMinutes: Int,
        currentStreak: Int,
        bestStreak: Int,
        nowMillis: Long,
    ): PeriodStatistics {
        val yearCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.YEAR, offset)
        }
        val targetYear = yearCal.get(Calendar.YEAR)
        val yearStart = yearCal.timeInMillis
        val daysInYear = yearCal.getActualMaximum(Calendar.DAY_OF_YEAR)

        val nextYearCal = (yearCal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
        val yearEnd = nextYearCal.timeInMillis

        val prevYearCal = (yearCal.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        val prevStart = prevYearCal.timeInMillis

        val currentSessions = focusSessions.filter { it.completedAtMillis in yearStart until yearEnd }
        val prevSessions = focusSessions.filter { it.completedAtMillis in prevStart until yearStart }

        val totalMinutes = secondsToRoundedMinutes(currentSessions.sumOf { it.actualDurationSeconds })
        val prevMinutes = secondsToRoundedMinutes(prevSessions.sumOf { it.actualDurationSeconds })

        val monthSeconds = IntArray(12)
        val monthCounts = IntArray(12)
        val activeDayKeys = HashSet<Long>()
        val tempCal = Calendar.getInstance()
        for (session in currentSessions) {
            tempCal.timeInMillis = session.completedAtMillis
            val m = tempCal.get(Calendar.MONTH).coerceIn(0, 11)
            monthSeconds[m] += session.actualDurationSeconds
            monthCounts[m] += 1
            activeDayKeys.add(dayKey(session.completedAtMillis))
        }

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMonthIdx = nowCal.get(Calendar.MONTH)
        val isCurrentYear = (offset == 0)

        val buckets = (0 until 12).map { m ->
            val mCal = (yearCal.clone() as Calendar).apply { set(Calendar.MONTH, m) }
            val shortName = monthShortFormat.format(mCal.timeInMillis)
            val fullName = monthFullFormat.format(mCal.timeInMillis)
            ChartBucket(
                key = "y_$m",
                shortLabel = shortName.take(1),
                detailLabel = "$fullName $targetYear",
                focusMinutes = secondsToRoundedMinutes(monthSeconds[m]),
                sessionCount = monthCounts[m],
                isCurrent = isCurrentYear && m == currentMonthIdx,
            )
        }

        val peakBucket = buckets.maxByOrNull { it.focusMinutes }?.takeIf { it.focusMinutes > 0 }
        val annualGoal = dailyGoalMinutes * daysInYear
        val goalPct = if (annualGoal > 0) {
            ((totalMinutes.toFloat() / annualGoal) * 100f).roundToInt().coerceAtMost(999)
        } else 0

        return PeriodStatistics(
            period = StatsPeriod.ANNUAL,
            periodTitle = targetYear.toString(),
            canNavigateForward = offset < 0,
            totalFocusMinutes = totalMinutes,
            sessionCount = currentSessions.size,
            averageSessionMinutes = if (currentSessions.isEmpty()) 0 else (totalMinutes / currentSessions.size),
            activeDaysCount = activeDayKeys.size,
            goalCompletionPercent = goalPct,
            deltaPercentVsPrevious = computeDeltaPercent(totalMinutes, prevMinutes),
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            primaryBuckets = buckets,
            peakLabel = peakBucket?.detailLabel?.substringBefore(" "),
            tagBreakdown = computeTagBreakdown(currentSessions),
            periodSessions = currentSessions.sortedByDescending { it.completedAtMillis },
        )
    }

    private fun computeTagBreakdown(sessions: List<FocusSessionEntity>): List<TagBreakdownItem> {
        if (sessions.isEmpty()) return emptyList()
        val totalSeconds = sessions.sumOf { it.actualDurationSeconds }.coerceAtLeast(1)
        return sessions.groupBy { it.label.ifBlank { "Deep Work" } }
            .map { (label, list) ->
                val secs = list.sumOf { it.actualDurationSeconds }
                TagBreakdownItem(
                    label = label,
                    focusMinutes = secondsToRoundedMinutes(secs),
                    sessionCount = list.size,
                    fraction = (secs.toFloat() / totalSeconds).coerceIn(0f, 1f),
                )
            }
            .sortedByDescending { it.focusMinutes }
            .take(6)
    }

    private fun computeStreaks(
        focusSessions: List<FocusSessionEntity>,
        nowMillis: Long,
    ): Pair<Int, Int> {
        if (focusSessions.isEmpty()) return 0 to 0

        val uniqueDayStarts = focusSessions
            .map { startOfDay(it.completedAtMillis) }
            .toSortedSet()
            .toList()

        // Best streak
        var best = 1
        var running = 1
        for (i in 1 until uniqueDayStarts.size) {
            val expectedNext = addDays(uniqueDayStarts[i - 1], 1)
            if (uniqueDayStarts[i] == expectedNext) {
                running++
                if (running > best) best = running
            } else {
                running = 1
            }
        }

        // Current streak (anchored at today, or yesterday if nothing logged yet today)
        val todayStart = startOfDay(nowMillis)
        val yesterdayStart = addDays(todayStart, -1)
        val daySet = uniqueDayStarts.toHashSet()
        var cursor = when {
            daySet.contains(todayStart) -> todayStart
            daySet.contains(yesterdayStart) -> yesterdayStart
            else -> return 0 to best
        }
        var current = 0
        while (daySet.contains(cursor)) {
            current++
            cursor = addDays(cursor, -1)
        }
        return current to maxOf(best, current)
    }

    private fun computeDeltaPercent(current: Int, previous: Int): Int? {
        if (previous <= 0) return if (current > 0) 100 else null
        return (((current - previous).toFloat() / previous) * 100f).roundToInt()
    }

    private fun secondsToRoundedMinutes(seconds: Int): Int {
        if (seconds <= 0) return 0
        return ((seconds + 30) / 60).coerceAtLeast(1)
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun startOfWeekMonday(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfDay(millis)
            val dow = get(Calendar.DAY_OF_WEEK)
            val diffToMonday = (dow + 5) % 7
            add(Calendar.DAY_OF_YEAR, -diffToMonday)
        }
        return cal.timeInMillis
    }

    private fun addDays(dayStartMillis: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }

    private fun dayKey(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.YEAR) * 10000L +
            (cal.get(Calendar.MONTH) + 1) * 100L +
            cal.get(Calendar.DAY_OF_MONTH)
    }
}
