package com.pacepilot.app

import com.pacepilot.app.data.model.PacingStatus
import com.pacepilot.app.pacing.PacingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacingEngineTest {

    @Test
    fun testInitialTargetSpeedCalculation() {
        // 50 km distance in 2 hours -> required 25 km/h
        val totalDistanceMeters = 50000.0
        val twoHoursMillis = 2 * 3600 * 1000L

        val targetSpeed = PacingEngine.calculateInitialTargetSpeed(totalDistanceMeters, twoHoursMillis)
        assertEquals(25.0, targetSpeed, 0.01)
    }

    @Test
    fun testPacingEvaluationSpeedUp() {
        // 40 km total, 10 km covered, 1 hour elapsed out of 2 hours COT
        // Remaining: 30 km in 1 hour -> Required speed = 30 km/h
        // Current speed is 20 km/h (< 30 - tolerance) -> Should suggest SPEED_UP
        val totalDistance = 40000.0
        val covered = 10000.0
        val cotMillis = 2 * 3600 * 1000L
        val elapsedMillis = 1 * 3600 * 1000L
        val currentSpeed = 20.0

        val result = PacingEngine.evaluatePacing(
            totalDistanceMeters = totalDistance,
            distanceCoveredMeters = covered,
            targetCotMillis = cotMillis,
            elapsedTimeMillis = elapsedMillis,
            currentSpeedKmh = currentSpeed
        )

        assertEquals(PacingStatus.SPEED_UP, result.status)
        assertEquals(30.0, result.requiredSpeedKmh, 0.01)
        assertTrue(result.advice.contains("Tambah kecepatan"))
    }

    @Test
    fun testPacingEvaluationSlowDown() {
        // Remaining: 15 km in 1 hour -> Required speed = 15 km/h
        // Current speed is 25 km/h (> 15 + 5 km/h) -> Should suggest SLOW_DOWN
        val totalDistance = 30000.0
        val covered = 15000.0
        val cotMillis = 2 * 3600 * 1000L
        val elapsedMillis = 1 * 3600 * 1000L
        val currentSpeed = 25.0

        val result = PacingEngine.evaluatePacing(
            totalDistanceMeters = totalDistance,
            distanceCoveredMeters = covered,
            targetCotMillis = cotMillis,
            elapsedTimeMillis = elapsedMillis,
            currentSpeedKmh = currentSpeed
        )

        assertEquals(PacingStatus.SLOW_DOWN, result.status)
        assertEquals(15.0, result.requiredSpeedKmh, 0.01)
        assertTrue(result.advice.contains("Kurangi kecepatan"))
    }

    @Test
    fun testPacingEvaluationOnPace() {
        // Remaining: 20 km in 1 hour -> Required speed = 20 km/h
        // Current speed is 20.5 km/h -> Should be ON_PACE
        val totalDistance = 40000.0
        val covered = 20000.0
        val cotMillis = 2 * 3600 * 1000L
        val elapsedMillis = 1 * 3600 * 1000L
        val currentSpeed = 20.5

        val result = PacingEngine.evaluatePacing(
            totalDistanceMeters = totalDistance,
            distanceCoveredMeters = covered,
            targetCotMillis = cotMillis,
            elapsedTimeMillis = elapsedMillis,
            currentSpeedKmh = currentSpeed
        )

        assertEquals(PacingStatus.ON_PACE, result.status)
        assertEquals(20.0, result.requiredSpeedKmh, 0.01)
        assertTrue(result.advice.contains("Pacing ideal"))
    }

    @Test
    fun testPacingEvaluationBehindCot() {
        // COT is 1 hour, elapsed is 1h 10m, route not finished
        val totalDistance = 30000.0
        val covered = 25000.0
        val cotMillis = 3600 * 1000L
        val elapsedMillis = 4200 * 1000L
        val currentSpeed = 18.0

        val result = PacingEngine.evaluatePacing(
            totalDistanceMeters = totalDistance,
            distanceCoveredMeters = covered,
            targetCotMillis = cotMillis,
            elapsedTimeMillis = elapsedMillis,
            currentSpeedKmh = currentSpeed
        )

        assertEquals(PacingStatus.BEHIND_COT, result.status)
        assertTrue(result.advice.contains("Waktu COT terlewati"))
    }
}
