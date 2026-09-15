package com.pacepilot.app.fitness

import com.pacepilot.app.data.model.UserHealthProfile

object HealthCalculator {

    /**
     * Menghitung nilai MET (Metabolic Equivalent of Task) berdasarkan kecepatan sepeda
     */
    fun getCyclingMet(speedKmh: Double): Double {
        return when {
            speedKmh <= 0.5 -> 1.0
            speedKmh < 16.0 -> 4.0   // Gowes santai (< 16 km/j)
            speedKmh < 20.0 -> 6.8   // Gowes sedang (16 - 20 km/j)
            speedKmh < 25.0 -> 8.5   // Gowes tempo/aerobik (20 - 25 km/j)
            speedKmh < 30.0 -> 10.0  // Gowes cepat (25 - 30 km/j)
            else -> 12.5             // Gowes intensif/race (> 30 km/j)
        }
    }

    /**
     * Menghitung estimasi kalori aktif (kcal) terbakar
     * Formula standar kedokteran olahraga: Calories = MET * weight_kg * time_hours
     */
    fun calculateCalories(
        durationMillis: Long,
        avgSpeedKmh: Double,
        weightKg: Float = 68.0f
    ): Int {
        if (durationMillis <= 0L) return 0
        val hours = durationMillis.toDouble() / 3600000.0
        val met = getCyclingMet(avgSpeedKmh)
        return (met * weightKg * hours).toInt()
    }

    /**
     * Menghitung estimasi kehilangan cairan / keringat (Liter)
     */
    fun calculateSweatLossLiters(
        durationMillis: Long,
        avgSpeedKmh: Double
    ): Double {
        if (durationMillis <= 0L) return 0.0
        val hours = durationMillis.toDouble() / 3600000.0
        val lossRatePerHr = when {
            avgSpeedKmh < 18.0 -> 0.55
            avgSpeedKmh < 26.0 -> 0.75
            else -> 1.05
        }
        return hours * lossRatePerHr
    }

    /**
     * Menghitung rekomendasi waktu pemulihan (Recovery Time in Hours)
     */
    fun calculateRecoveryHours(
        durationMillis: Long,
        avgSpeedKmh: Double
    ): Int {
        val hours = durationMillis.toDouble() / 3600000.0
        val intensityMultiplier = if (avgSpeedKmh > 24.0) 1.5 else 1.0
        val baseHours = (hours * 8.0 * intensityMultiplier).toInt()
        return baseHours.coerceIn(4, 72)
    }

    /**
     * Saran zona irama kayuhan (Cadence recommendation)
     */
    fun getCadenceAdvice(currentSpeedKmh: Double): String {
        return when {
            currentSpeedKmh < 18.0 -> "Jaga putaran pedal santai di 70-80 RPM"
            currentSpeedKmh < 28.0 -> "Cadence optimal endurance di 80-90 RPM"
            else -> "Cadence balap agresif di 90-100 RPM, gunakan rasio gir pas"
        }
    }
}
