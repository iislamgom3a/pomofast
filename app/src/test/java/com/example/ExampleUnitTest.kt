package com.example

import com.example.data.local.FocusSessionEntity
import com.example.data.local.TimerMode
import com.example.domain.ProductivityStatsCalculator
import com.example.domain.StatsPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun `daily weekly monthly and annual statistics aggregate accurately`() {
        val anchor = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterday = Calendar.getInstance().apply {
            timeInMillis = anchor
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        val lastMonth = Calendar.getInstance().apply {
            timeInMillis = anchor
            add(Calendar.MONTH, -1)
        }.timeInMillis

        val sessions = listOf(
            FocusSessionEntity(
                id = 1,
                sessionType = TimerMode.FOCUS.storageKey,
                label = "Deep Work",
                plannedDurationSeconds = 1500,
                actualDurationSeconds = 1500,
                completedAtMillis = anchor,
            ),
            FocusSessionEntity(
                id = 2,
                sessionType = TimerMode.FOCUS.storageKey,
                label = "Coding",
                plannedDurationSeconds = 1500,
                actualDurationSeconds = 1500,
                completedAtMillis = anchor - 3_600_000L,
            ),
            FocusSessionEntity(
                id = 3,
                sessionType = TimerMode.FOCUS.storageKey,
                label = "Deep Work",
                plannedDurationSeconds = 1500,
                actualDurationSeconds = 1500,
                completedAtMillis = yesterday,
            ),
            FocusSessionEntity(
                id = 4,
                sessionType = TimerMode.FOCUS.storageKey,
                label = "Study",
                plannedDurationSeconds = 3000,
                actualDurationSeconds = 3000,
                completedAtMillis = lastMonth,
            ),
        )

        val daily = ProductivityStatsCalculator.computePeriodStatistics(
            allSessions = sessions,
            period = StatsPeriod.DAILY,
            offset = 0,
            dailyGoalMinutes = 100,
            nowMillis = anchor,
        )
        assertEquals(50, daily.totalFocusMinutes)
        assertEquals(2, daily.sessionCount)
        assertEquals(50, daily.goalCompletionPercent)
        assertEquals(2, daily.currentStreakDays)
        assertEquals(2, daily.bestStreakDays)

        val weekly = ProductivityStatsCalculator.computePeriodStatistics(
            allSessions = sessions,
            period = StatsPeriod.WEEKLY,
            offset = 0,
            dailyGoalMinutes = 100,
            nowMillis = anchor,
        )
        assertEquals(75, weekly.totalFocusMinutes)
        assertEquals(3, weekly.sessionCount)
        assertEquals(7, weekly.primaryBuckets.size)

        val monthly = ProductivityStatsCalculator.computePeriodStatistics(
            allSessions = sessions,
            period = StatsPeriod.MONTHLY,
            offset = 0,
            dailyGoalMinutes = 100,
            nowMillis = anchor,
        )
        assertEquals(75, monthly.totalFocusMinutes)
        assertEquals(30, monthly.monthlyHeatmapCells.size)
        assertEquals(2, monthly.activeDaysCount)

        val annual = ProductivityStatsCalculator.computePeriodStatistics(
            allSessions = sessions,
            period = StatsPeriod.ANNUAL,
            offset = 0,
            dailyGoalMinutes = 100,
            nowMillis = anchor,
        )
        assertEquals(125, annual.totalFocusMinutes)
        assertEquals(4, annual.sessionCount)
        assertEquals(12, annual.primaryBuckets.size)
        assertTrue(annual.tagBreakdown.isNotEmpty())
    }
}
