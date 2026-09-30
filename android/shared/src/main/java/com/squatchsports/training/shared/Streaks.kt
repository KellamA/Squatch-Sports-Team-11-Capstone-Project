package com.squatchsports.training.shared

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Streak rules:
 * - A training day is any calendar day with at least one saved session.
 * - One missed day between training days is a rest day: the streak survives but does not grow.
 * - Two missed days in a row end the streak.
 */
data class StreakResult(
    val current: Int,
    val best: Int,
    val lastTrainingDay: LocalDate?,
)

/** Feedback shown when a saved workout grows the current streak. */
data class StreakCelebration(
    val streak: Int,
    val isMilestone: Boolean,
    val isNewBest: Boolean,
)

object StreakCalculator {
    const val MAX_REST_DAYS = 1
    val MILESTONES = listOf(3, 7, 14, 30, 50, 100, 365)

    /** Largest day gap between two training days that keeps a streak alive (1 = consecutive). */
    private const val MAX_GAP = MAX_REST_DAYS + 1L

    fun calculate(trainingDays: Collection<LocalDate>, today: LocalDate): StreakResult {
        val days = trainingDays.filter { !it.isAfter(today) }.distinct().sorted()
        if (days.isEmpty()) return StreakResult(current = 0, best = 0, lastTrainingDay = null)

        var run = 0
        var best = 0
        var previous: LocalDate? = null
        for (day in days) {
            run = if (previous != null && ChronoUnit.DAYS.between(previous, day) <= MAX_GAP) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }

        val last = days.last()
        val alive = ChronoUnit.DAYS.between(last, today) <= MAX_GAP
        return StreakResult(current = if (alive) run else 0, best = best, lastTrainingDay = last)
    }

    fun calculateFromSessions(
        sessions: Collection<WorkoutSession>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): StreakResult = calculate(sessions.map { toLocalDate(it.endDateMillis, zone) }, today)

    /** Returns feedback when [after] grew the streak compared to [before], or null otherwise. */
    fun celebrationFor(before: StreakResult, after: StreakResult): StreakCelebration? {
        if (after.current <= before.current) return null
        return StreakCelebration(
            streak = after.current,
            isMilestone = after.current in MILESTONES,
            isNewBest = after.current > before.best && after.current > 1,
        )
    }

    fun nextMilestone(current: Int): Int? = MILESTONES.firstOrNull { it > current }

    fun toLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
