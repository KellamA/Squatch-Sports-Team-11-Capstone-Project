package com.squatchsports.training.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GamificationPayloadsTest {
    @Test fun statsRoundTrip() {
        val stats = WatchStats(
            currentStreak = 7,
            bestStreak = 9,
            trainedToday = true,
            goals = listOf(
                GoalProgress(GoalType.DAILY_SHOTS, 120, 200),
                GoalProgress(GoalType.TARGET_FG, 80, 75, eligible = false),
            ),
        )
        assertEquals(stats, GamificationPayloads.decodeStats(GamificationPayloads.stats(stats)))
    }

    @Test fun celebrationRoundTrip() {
        val message = CelebrationMessages.forStreak(StreakCelebration(streak = 7, isMilestone = true, isNewBest = true))
        assertEquals(message, GamificationPayloads.decodeCelebration(GamificationPayloads.celebration(message)))
    }

    @Test fun invalidPayloadsDecodeToNull() {
        assertNull(GamificationPayloads.decodeStats("not json".encodeToByteArray()))
        assertNull(GamificationPayloads.decodeCelebration("{}".encodeToByteArray()))
    }

    @Test fun streakMessagesMatchCelebrationType() {
        assertEquals("Milestone reached!", CelebrationMessages.forStreak(StreakCelebration(7, true, true)).title)
        assertEquals("New personal best!", CelebrationMessages.forStreak(StreakCelebration(8, false, true)).title)
        assertEquals("Streak started!", CelebrationMessages.forStreak(StreakCelebration(1, false, false)).title)
        assertEquals("Streak continued!", CelebrationMessages.forStreak(StreakCelebration(2, false, false)).title)
    }

    @Test fun goalMessageListsEachGoal() {
        val message = CelebrationMessages.forGoals(
            listOf(GoalProgress(GoalType.DAILY_SHOTS, 50, 50), GoalProgress(GoalType.TARGET_FG, 80, 75)),
        )
        assertEquals("You crushed 2 goals!", message.title)
        assertEquals("Daily Shots: 50 / 50 shots\nToday's FG%: 80% (target 75%)", message.subtitle)
    }
}
