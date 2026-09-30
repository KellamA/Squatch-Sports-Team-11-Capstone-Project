package com.squatchsports.training.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 9, 30)

    private fun sep(vararg days: Int) = days.map { LocalDate.of(2026, 9, it) }

    private fun assertStreak(days: List<LocalDate>, current: Int, best: Int) {
        val result = StreakCalculator.calculate(days, today)
        assertEquals("current", current, result.current)
        assertEquals("best", best, result.best)
    }

    @Test fun case01_noSessions() = assertStreak(emptyList(), current = 0, best = 0)

    @Test fun case02_firstWorkoutToday() = assertStreak(sep(30), current = 1, best = 1)

    @Test fun case03_consecutiveDays() = assertStreak(sep(28, 29, 30), current = 3, best = 3)

    @Test fun case04_sameDaySessionsCountOnce() = assertStreak(sep(29, 30, 30), current = 2, best = 2)

    @Test fun case05_restDayKeepsStreakWithoutAdding() = assertStreak(sep(27, 28, 30), current = 3, best = 3)

    @Test fun case06_notTrainedTodayYetStillAlive() = assertStreak(sep(28, 29), current = 2, best = 2)

    @Test fun case07_restDayYesterdayStillAlive() = assertStreak(sep(27, 28), current = 2, best = 2)

    @Test fun case08_twoMissedDaysBreakStreak() = assertStreak(sep(26, 27), current = 0, best = 2)

    @Test fun case09_bestStreakRemembered() =
        assertStreak(sep(20, 21, 22, 23, 24, 28, 29, 30), current = 3, best = 5)

    @Test fun case10_everyOtherDayContinues() = assertStreak(sep(24, 26, 28, 30), current = 4, best = 4)

    @Test fun futureDatesAreIgnored() {
        val days = sep(29, 30) + LocalDate.of(2026, 10, 2)
        assertStreak(days, current = 2, best = 2)
    }

    @Test fun lastTrainingDayIsReported() {
        assertEquals(LocalDate.of(2026, 9, 28), StreakCalculator.calculate(sep(27, 28), today).lastTrainingDay)
        assertNull(StreakCalculator.calculate(emptyList(), today).lastTrainingDay)
    }

    @Test fun sessionsAreGroupedByLocalDay() {
        // 23:30 and 00:30 UTC fall on two different days in UTC.
        val lateSep29 = LocalDate.of(2026, 9, 29).atTime(23, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val earlySep30 = LocalDate.of(2026, 9, 30).atTime(0, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val sessions = listOf(lateSep29, earlySep30).map {
            WorkoutSession(
                drill = "Form Shooting",
                makes = 1,
                misses = 0,
                swishes = 0,
                attempts = 1,
                startDateMillis = it,
                endDateMillis = it,
            )
        }
        val result = StreakCalculator.calculateFromSessions(sessions, today, ZoneOffset.UTC)
        assertEquals(2, result.current)
    }

    @Test fun celebrationWhenStreakGrows() {
        val before = StreakCalculator.calculate(sep(28, 29), today)
        val after = StreakCalculator.calculate(sep(28, 29, 30), today)
        val celebration = StreakCalculator.celebrationFor(before, after)!!
        assertEquals(3, celebration.streak)
        assertTrue(celebration.isMilestone)
        assertTrue(celebration.isNewBest)
    }

    @Test fun noCelebrationForSecondSessionSameDay() {
        val before = StreakCalculator.calculate(sep(29, 30), today)
        val after = StreakCalculator.calculate(sep(29, 30, 30), today)
        assertNull(StreakCalculator.celebrationFor(before, after))
    }

    @Test fun continuedStreakBelowBestIsNotNewBest() {
        val before = StreakCalculator.calculate(sep(20, 21, 22, 23, 24, 29), today)
        val after = StreakCalculator.calculate(sep(20, 21, 22, 23, 24, 29, 30), today)
        val celebration = StreakCalculator.celebrationFor(before, after)!!
        assertEquals(2, celebration.streak)
        assertFalse(celebration.isMilestone)
        assertFalse(celebration.isNewBest)
    }

    @Test fun nextMilestone() {
        assertEquals(3, StreakCalculator.nextMilestone(0))
        assertEquals(7, StreakCalculator.nextMilestone(3))
        assertNull(StreakCalculator.nextMilestone(365))
    }
}
