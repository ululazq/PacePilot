package com.pacepilot.app.pacing

import com.pacepilot.app.data.model.PacingStatus
import com.pacepilot.app.data.model.RideMetrics
import java.util.Locale
import kotlin.math.max

object PacingEngine {

    /**
     * Menghitung kecepatan rata-rata target awal dari total jarak dan target COT
     */
    fun calculateInitialTargetSpeed(totalDistanceMeters: Double, targetCotMillis: Long): Double {
        if (targetCotMillis <= 0L) return 0.0
        val distanceKm = totalDistanceMeters / 1000.0
        val hours = targetCotMillis.toDouble() / 3600000.0
        return if (hours > 0) distanceKm / hours else 0.0
    }

    /**
     * Menghitung status pacing terkini dan saran penyesuaian kecepatan
     */
    fun evaluatePacing(
        totalDistanceMeters: Double,
        distanceCoveredMeters: Double,
        targetCotMillis: Long,
        elapsedTimeMillis: Long,
        currentSpeedKmh: Double,
        speedToleranceKmh: Double = 2.0
    ): PacingResult {
        val remainingDistanceMeters = max(0.0, totalDistanceMeters - distanceCoveredMeters)
        val remainingTimeMillis = targetCotMillis - elapsedTimeMillis

        // Hitung kecepatan rata-rata perjalanan sejauh ini
        val elapsedHours = elapsedTimeMillis.toDouble() / 3600000.0
        val distanceCoveredKm = distanceCoveredMeters / 1000.0
        val avgSpeedKmh = if (elapsedHours > 0.005) distanceCoveredKm / elapsedHours else currentSpeedKmh

        // Kasus: Rute sudah selesai
        if (remainingDistanceMeters <= 25.0 && distanceCoveredMeters > 50.0) {
            return PacingResult(
                status = PacingStatus.ON_PACE,
                requiredSpeedKmh = 0.0,
                speedDeltaKmh = 0.0,
                advice = "🏁 Garis akhir tercapai! Selesaikan perjalanan.",
                avgSpeedKmh = avgSpeedKmh,
                remainingTimeMillis = remainingTimeMillis,
                remainingDistanceMeters = 0.0
            )
        }

        // Kasus: Waktu COT telah habis tapi rute belum selesai
        if (remainingTimeMillis <= 0) {
            val overtimeMinutes = (-remainingTimeMillis / 60000)
            return PacingResult(
                status = PacingStatus.BEHIND_COT,
                requiredSpeedKmh = 0.0,
                speedDeltaKmh = 0.0,
                advice = "⚠️ Waktu COT terlewati (+${overtimeMinutes}m). Kayuh santai dan tetap aman!",
                avgSpeedKmh = avgSpeedKmh,
                remainingTimeMillis = remainingTimeMillis,
                remainingDistanceMeters = remainingDistanceMeters
            )
        }

        val remainingDistanceKm = remainingDistanceMeters / 1000.0
        val remainingHours = remainingTimeMillis.toDouble() / 3600000.0
        val requiredSpeedKmh = if (remainingHours > 0) remainingDistanceKm / remainingHours else 0.0

        val speedDiff = currentSpeedKmh - requiredSpeedKmh

        val (status, advice) = when {
            speedDiff < -speedToleranceKmh -> {
                val needed = -speedDiff
                PacingStatus.SPEED_UP to String.format(
                    Locale.US,
                    "⚡ Tambah kecepatan +%.1f km/j! Target: %.1f km/j",
                    needed,
                    requiredSpeedKmh
                )
            }
            speedDiff > 5.0 -> {
                PacingStatus.SLOW_DOWN to String.format(
                    Locale.US,
                    "🛑 Kurangi kecepatan %.1f km/j! Simpan tenaga untuk sisa rute",
                    speedDiff
                )
            }
            speedDiff > 2.0 -> {
                PacingStatus.AHEAD_OF_COT to String.format(
                    Locale.US,
                    "🚀 Di depan target (+%.1f km/j). Ritme sangat baik!",
                    speedDiff
                )
            }
            else -> {
                PacingStatus.ON_PACE to String.format(
                    Locale.US,
                    "✅ Pacing ideal! Pertahankan kecepatan %.1f km/j",
                    requiredSpeedKmh
                )
            }
        }

        return PacingResult(
            status = status,
            requiredSpeedKmh = requiredSpeedKmh,
            speedDeltaKmh = speedDiff,
            advice = advice,
            avgSpeedKmh = avgSpeedKmh,
            remainingTimeMillis = remainingTimeMillis,
            remainingDistanceMeters = remainingDistanceMeters
        )
    }
}

data class PacingResult(
    val status: PacingStatus,
    val requiredSpeedKmh: Double,
    val speedDeltaKmh: Double,
    val advice: String,
    val avgSpeedKmh: Double,
    val remainingTimeMillis: Long,
    val remainingDistanceMeters: Double
)
