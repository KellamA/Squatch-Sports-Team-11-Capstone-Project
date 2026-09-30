package com.squatchsports.training.data

import com.squatchsports.training.shared.CelebrationMessage
import com.squatchsports.training.shared.CelebrationMessages
import com.squatchsports.training.shared.GoalProgress
import com.squatchsports.training.shared.StreakCelebration

/** A popup shown after a saved workout. */
sealed interface Celebration {
    fun toMessage(): CelebrationMessage

    data class Streak(val streak: StreakCelebration) : Celebration {
        override fun toMessage() = CelebrationMessages.forStreak(streak)
    }

    data class Goals(val completed: List<GoalProgress>) : Celebration {
        override fun toMessage() = CelebrationMessages.forGoals(completed)
    }
}
