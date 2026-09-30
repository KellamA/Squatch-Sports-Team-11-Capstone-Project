package com.squatchsports.training.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class GoalTrackerTest {
    // Wednesday. With a Sunday week start, the week is Sep 27 – Oct 3.
    private val today = LocalDate.of(2026, 9, 30)
    private val zone = ZoneOffset.UTC
    private val weekStart = DayOfWeek.SUNDAY
    private val targets = GoalTargets(dailyShots = 50, dailyMakes = 30, weeklySessions = 3, targetFgPercent = 60)

    private fun session(day: Int, makes: Int, attempts: Int, month: Int = 9): WorkoutSession {
        val millis = LocalDate.of(2026, month, day).atTime(18, 0).toInstant(zone).toEpochMilli()
        return WorkoutSession(
            drill = "Spot Shooting",
            makes = makes,
            misses = attempts - makes,
            swishes = 0,
            attempts = attempts,
            startDateMillis = millis,
            endDateMillis = millis,
        )
    }

    private fun progress(sessions: List<WorkoutSession>) =
        GoalTracker.progress(sessions, targets, today, zone, weekStart).associateBy { it.type }

    @Test fun emptyHistoryHasZeroProgress() {
        val p = progress(emptyList())
        GoalType.entries.forEach { assertEquals(0, p.getValue(it).current) }
        assertFalse(p.values.any { it.isComplete })
    }

    @Test fun dailyGoalsOnlyCountToday() {
        val p = progress(listOf(session(30, makes = 20, attempts = 25), session(29, makes = 40, attempts = 50)))
        assertEquals(25, p.getValue(GoalType.DAILY_SHOTS).current)
        assertEquals(20, p.getValue(GoalType.DAILY_MAKES).current)
        assertEquals(0.5f, p.getValue(GoalType.DAILY_SHOTS).fraction, 0.001f)
    }

    @Test fun multipleSessionsTodayAddUp() {
        val p = progress(listOf(session(30, makes = 20, attempts = 25), session(30, makes = 15, attempts = 25)))
        assertEquals(50, p.getValue(GoalType.DAILY_SHOTS).current)
        assertTrue(p.getValue(GoalType.DAILY_SHOTS).isComplete)
        assertTrue(p.getValue(GoalType.DAILY_MAKES).isComplete)
    }

    @Test fun progressIsCappedAtFull() {
        val p = progress(listOf(session(30, makes = 90, attempts = 100)))
        assertEquals(100, p.getValue(GoalType.DAILY_SHOTS).current)
        assertEquals(1f, p.getValue(GoalType.DAILY_SHOTS).fraction, 0.001f)
    }

    @Test fun weeklySessionsUseWeekBoundaries() {
        // Sep 26 is the Saturday before this week; Sep 27 (Sunday) starts it.
        val p = progress(listOf(session(26, 5, 10), session(27, 5, 10), session(28, 5, 10), session(30, 5, 10)))
        assertEquals(3, p.getValue(GoalType.WEEKLY_SESSIONS).current)
        assertTrue(p.getValue(GoalType.WEEKLY_SESSIONS).isComplete)
    }

    @Test fun fgGoalNeedsMinimumAttempts() {
        val lucky = progress(listOf(session(30, makes = 5, attempts = 5))).getValue(GoalType.TARGET_FG)
        assertEquals(100, lucky.current)
        assertFalse(lucky.eligible)
        assertFalse(lucky.isComplete)

        val real = progress(listOf(session(30, makes = 7, attempts = 10))).getValue(GoalType.TARGET_FG)
        assertEquals(70, real.current)
        assertTrue(real.isComplete)
    }

    @Test fun fgBelowTargetIsNotComplete() {
        val fg = progress(listOf(session(30, makes = 11, attempts = 20))).getValue(GoalType.TARGET_FG)
        assertEquals(55, fg.current)
        assertFalse(fg.isComplete)
    }

    @Test fun periodKeysSeparateDaysAndWeeks() {
        assertEquals("DAILY_SHOTS@2026-09-30", GoalTracker.periodKey(GoalType.DAILY_SHOTS, today, weekStart))
        assertEquals("WEEKLY_SESSIONS@2026-09-27", GoalTracker.periodKey(GoalType.WEEKLY_SESSIONS, today, weekStart))
    }

    @Test fun completedGoalsAreCelebratedOncePerPeriod() {
        val p = GoalTracker.progress(listOf(session(30, makes = 40, attempts = 50)), targets, today, zone, weekStart)
        val first = GoalTracker.newlyCompleted(p, today, emptySet(), weekStart).map { it.type }
        assertEquals(listOf(GoalType.DAILY_SHOTS, GoalType.DAILY_MAKES, GoalType.TARGET_FG), first)

        val celebrated = first.map { GoalTracker.periodKey(it, today, weekStart) }.toSet()
        assertTrue(GoalTracker.newlyCompleted(p, today, celebrated, weekStart).isEmpty())

        // The next day the daily goals can be celebrated again.
        val tomorrow = today.plusDays(1)
        val nextDay = GoalTracker.progress(
            listOf(session(1, makes = 40, attempts = 50, month = 10)), targets, tomorrow, zone, weekStart,
        )
        assertEquals(3, GoalTracker.newlyCompleted(nextDay, tomorrow, celebrated, weekStart).size)
    }

    @Test fun targetsCanBeUpdated() {
        val updated = targets.with(GoalType.DAILY_MAKES, 45)
        assertEquals(45, updated.targetFor(GoalType.DAILY_MAKES))
        assertEquals(50, updated.targetFor(GoalType.DAILY_SHOTS))
    }
}
