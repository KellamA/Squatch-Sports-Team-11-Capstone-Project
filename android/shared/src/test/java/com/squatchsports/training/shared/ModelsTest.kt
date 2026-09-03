package com.squatchsports.training.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {
    @Test
    fun shotCodesMatchAppleProtocol() {
        assertEquals(ShotDirection.MISS, ShotDirection.fromCode(0))
        assertEquals(ShotDirection.MAKE, ShotDirection.fromCode(1))
        assertEquals(ShotDirection.SWISH, ShotDirection.fromCode(2))
        assertNull(ShotDirection.fromCode(3))
    }

    @Test
    fun drillPositionsMatchWorkoutPlan() {
        assertEquals(5, CourtPositions.forDrill("Spot Shooting").size)
        assertEquals(4, CourtPositions.forDrill("Midrange Series").size)
        assertEquals("Free Throw Line", CourtPositions.forDrill("Free Throws").single().name)
        assertEquals("Center", CourtPositions.forDrill("General Workout").single().name)
    }

    @Test
    fun workoutPercentageUsesStoredMakes() {
        val session = WorkoutSession(
            drill = "Form Shooting",
            makes = 4,
            misses = 1,
            swishes = 2,
            attempts = 5,
            startDateMillis = 1L,
            endDateMillis = 2L,
        )
        assertEquals(80, session.percentage)
    }
}
