package com.squatchsports.training.shared

/**
 * XP and level rules, matching XPCalculator on iOS:
 * - 25 XP per workout, 1 XP per make, 2 bonus XP per swish (makes already include swishes).
 * - 15 bonus XP for 75%+ shooting.
 * - A new level every 100 XP, starting at Level 1.
 */
object XpCalculator {
    const val XP_PER_WORKOUT = 25
    const val XP_PER_MAKE = 1
    const val BONUS_XP_PER_SWISH = 2
    const val ACCURACY_BONUS_XP = 15
    const val ACCURACY_BONUS_PERCENT = 75
    const val XP_PER_LEVEL = 100

    fun xpFor(session: WorkoutSession): Int {
        var xp = XP_PER_WORKOUT
        xp += session.makes * XP_PER_MAKE
        xp += session.swishes * BONUS_XP_PER_SWISH
        if (session.percentage >= ACCURACY_BONUS_PERCENT) xp += ACCURACY_BONUS_XP
        return xp
    }

    fun totalXp(sessions: Collection<WorkoutSession>): Int = sessions.sumOf { xpFor(it) }

    fun level(totalXp: Int): Int = totalXp / XP_PER_LEVEL + 1

    fun xpInCurrentLevel(totalXp: Int): Int = totalXp % XP_PER_LEVEL

    fun xpUntilNextLevel(totalXp: Int): Int = XP_PER_LEVEL - xpInCurrentLevel(totalXp)
}
