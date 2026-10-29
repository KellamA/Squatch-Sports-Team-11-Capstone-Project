package com.squatchsports.training.shared

import org.junit.Assert.assertEquals
import org.junit.Test

/** Same cases as XPCalculatorTests on iOS, so both platforms award the same XP. */
class XpCalculatorTest {
    private fun session(makes: Int, attempts: Int, swishes: Int = 0) = WorkoutSession(
        drill = "Spot Shooting",
        makes = makes,
        misses = attempts - makes,
        swishes = swishes,
        attempts = attempts,
        startDateMillis = 0L,
        endDateMillis = 0L,
    )

    @Test fun workoutXpFollowsRules() {
        // 25 per workout + 1 per make. 10/25 = 40%, so no accuracy bonus.
        assertEquals(35, XpCalculator.xpFor(session(makes = 10, attempts = 25)))
        // Swishes are included in makes and earn 2 bonus XP each: 25 + 10 + 3 * 2.
        assertEquals(41, XpCalculator.xpFor(session(makes = 10, attempts = 25, swishes = 3)))
        // 75%+ shooting earns 15 bonus XP: 25 + 15 + 15.
        assertEquals(55, XpCalculator.xpFor(session(makes = 15, attempts = 20)))
    }

    @Test fun levelsEvery100Xp() {
        assertEquals(1, XpCalculator.level(0))
        assertEquals(1, XpCalculator.level(99))
        assertEquals(2, XpCalculator.level(100))
        assertEquals(3, XpCalculator.level(250))
        assertEquals(50, XpCalculator.xpInCurrentLevel(250))
        assertEquals(50, XpCalculator.xpUntilNextLevel(250))
    }

    @Test fun totalXpAddsEveryWorkout() {
        assertEquals(90, XpCalculator.totalXp(listOf(session(10, 25), session(15, 20))))
    }

    @Test fun workoutMessageShowsXpOrLevelUp() {
        val workout = CelebrationMessages.forWorkout(earnedXp = 35, newLevel = 2, leveledUp = false, xpUntilNextLevel = 40)
        assertEquals("Workout Complete!", workout.title)
        assertEquals("Only 40 XP until Level 3.", workout.subtitle)

        val levelUp = CelebrationMessages.forWorkout(earnedXp = 55, newLevel = 3, leveledUp = true, xpUntilNextLevel = 95)
        assertEquals("Level Up!", levelUp.title)
        assertEquals("You earned 55 XP and reached Level 3!", levelUp.subtitle)
    }
}
