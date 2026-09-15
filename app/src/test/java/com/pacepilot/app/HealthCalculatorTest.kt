package com.pacepilot.app

import com.pacepilot.app.fitness.HealthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthCalculatorTest {

    @Test
    fun testMetValuesBySpeed() {
        assertEquals(4.0, HealthCalculator.getCyclingMet(14.0), 0.01)
        assertEquals(6.8, HealthCalculator.getCyclingMet(18.0), 0.01)
        assertEquals(8.5, HealthCalculator.getCyclingMet(22.0), 0.01)
        assertEquals(10.0, HealthCalculator.getCyclingMet(28.0), 0.01)
        assertEquals(12.5, HealthCalculator.getCyclingMet(33.0), 0.01)
    }

    @Test
    fun testCaloriesCalculation() {
        // 1 hour at 22 km/h (MET 8.5) for a 70 kg rider
        // Calories = 8.5 * 70 * 1 = 595 kcal
        val oneHourMillis = 3600000L
        val calories = HealthCalculator.calculateCalories(oneHourMillis, 22.0, 70.0f)
        assertEquals(595, calories)
    }

    @Test
    fun testRecoveryHours() {
        val twoHoursMillis = 7200000L
        val recovery = HealthCalculator.calculateRecoveryHours(twoHoursMillis, 25.0)
        assertTrue(recovery in 12..48)
    }
}
