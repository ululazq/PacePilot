package com.pacepilot.app.data.model

import org.osmdroid.util.GeoPoint

/**
 * Representasi koordinat titik lokasi sepeda
 */
data class BikePoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude, altitude)

    companion object {
        fun fromGeoPoint(gp: GeoPoint): BikePoint = BikePoint(gp.latitude, gp.longitude, gp.altitude)
    }
}

/**
 * Data hasil perutean rute sepeda (OSRM / OpenStreetMap)
 */
data class RouteProfile(
    val waypoints: List<BikePoint>,
    val totalDistanceMeters: Double,
    val estimatedDurationSeconds: Double,
    val startName: String = "Titik Awal",
    val destinationName: String = "Tujuan",
    val steps: List<RouteStep> = emptyList()
) {
    val totalDistanceKm: Double
        get() = totalDistanceMeters / 1000.0

    val estimatedDurationMinutes: Int
        get() = (estimatedDurationSeconds / 60.0).toInt()
}

/**
 * Status pacing real-time pesepeda terhadap target COT
 */
enum class PacingStatus {
    ON_PACE,      // Kecepatan pas sesuai target COT
    SPEED_UP,     // Perlu menambah kecepatan (tertinggal target COT)
    SLOW_DOWN,    // Terlalu cepat (bisa kelelahan lebih awal)
    BEHIND_COT,   // Melewati batas waktu COT
    AHEAD_OF_COT  // Lebih cepat dan aman
}

/**
 * Statistik dan metrik gowes langsung (Live HUD)
 */
data class RideMetrics(
    val totalDistanceMeters: Double = 0.0,
    val distanceCoveredMeters: Double = 0.0,
    val distanceRemainingMeters: Double = 0.0,
    val targetCotMillis: Long = 0L,
    val elapsedTimeMillis: Long = 0L,
    val remainingTimeMillis: Long = 0L,
    val currentSpeedKmh: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
    val requiredSpeedKmh: Double = 0.0,
    val pacingStatus: PacingStatus = PacingStatus.ON_PACE,
    val pacingAdvice: String = "Mulai kayuh sepeda Anda",
    val nextFuelingSeconds: Long = 0L,
    val nextRestSeconds: Long = 0L,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val currentPosition: BikePoint? = null,
    val currentStep: RouteStep? = null,
    val distanceToNextStepMeters: Double = 0.0,
    val caloriesKcal: Int = 0,
    val sweatLossLiters: Double = 0.0
) {
    val distanceCoveredKm: Double
        get() = distanceCoveredMeters / 1000.0

    val distanceRemainingKm: Double
        get() = distanceRemainingMeters / 1000.0

    val totalDistanceKm: Double
        get() = totalDistanceMeters / 1000.0

    val progressPercent: Float
        get() = if (totalDistanceMeters > 0) {
            ((distanceCoveredMeters / totalDistanceMeters) * 100).coerceIn(0.0, 100.0).toFloat()
        } else 0f

    val cotProgressPercent: Float
        get() = if (targetCotMillis > 0) {
            ((elapsedTimeMillis.toDouble() / targetCotMillis.toDouble()) * 100).coerceIn(0.0, 100.0).toFloat()
        } else 0f
}

/**
 * Pengaturan kustom pengguna untuk jeda peringatan dan navigasi suara
 */
data class UserSettings(
    val fuelingIntervalMinutes: Int = 30, // Default notif makan/minum tiap 30 menit
    val restIntervalMinutes: Int = 60,    // Default notif istirahat tiap 60 menit
    val speedAlertThresholdKmh: Double = 2.5, // Toleransi deviasi kecepatan (km/jam)
    val enableAudioAlerts: Boolean = true,
    val enableVibrationAlerts: Boolean = true,
    val enableVoiceNavigation: Boolean = true
)

/**
 * Ringkasan hasil gowes setelah selesai
 */
data class RideSummary(
    val totalDistanceKm: Double,
    val totalTimeMillis: Long,
    val targetCotMillis: Long,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val isCotAchieved: Boolean,
    val timeDifferenceMillis: Long
)
