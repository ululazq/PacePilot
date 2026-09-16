package com.pacepilot.app.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Jenis manuver turn-by-turn navigasi
 */
enum class ManeuverType {
    DEPART,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    U_TURN,
    STRAIGHT,
    ROUNDABOUT,
    ARRIVE;

    companion object {
        fun fromOsrm(type: String, modifier: String?): ManeuverType {
            return when (type) {
                "depart" -> DEPART
                "arrive" -> ARRIVE
                "roundabout", "rotary" -> ROUNDABOUT
                "turn", "end of road", "fork" -> when (modifier) {
                    "left" -> TURN_LEFT
                    "right" -> TURN_RIGHT
                    "slight left" -> SLIGHT_LEFT
                    "slight right" -> SLIGHT_RIGHT
                    "sharp left" -> SHARP_LEFT
                    "sharp right" -> SHARP_RIGHT
                    "uturn" -> U_TURN
                    else -> STRAIGHT
                }
                "continue", "new name" -> when (modifier) {
                    "left" -> SLIGHT_LEFT
                    "right" -> SLIGHT_RIGHT
                    "uturn" -> U_TURN
                    else -> STRAIGHT
                }
                else -> STRAIGHT
            }
        }
    }
}

/**
 * Langkah petunjuk arah turn-by-turn (Google Maps style)
 */
data class RouteStep(
    val instruction: String,
    val streetName: String,
    val maneuver: ManeuverType,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val location: BikePoint
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("instruction", instruction)
            put("streetName", streetName)
            put("maneuver", maneuver.name)
            put("distanceMeters", distanceMeters)
            put("durationSeconds", durationSeconds)
            put("lat", location.latitude)
            put("lon", location.longitude)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RouteStep {
            return RouteStep(
                instruction = json.optString("instruction", "Lanjut lurus"),
                streetName = json.optString("streetName", ""),
                maneuver = try {
                    ManeuverType.valueOf(json.optString("maneuver", "STRAIGHT"))
                } catch (e: Exception) {
                    ManeuverType.STRAIGHT
                },
                distanceMeters = json.optDouble("distanceMeters", 0.0),
                durationSeconds = json.optDouble("durationSeconds", 0.0),
                location = BikePoint(json.optDouble("lat", 0.0), json.optDouble("lon", 0.0))
            )
        }
    }
}

/**
 * Rute tersimpan favorit pesepeda (Komoot style)
 */
data class SavedRoute(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val totalDistanceMeters: Double,
    val estimatedDurationSeconds: Double,
    val waypoints: List<BikePoint>,
    val steps: List<RouteStep> = emptyList(),
    val isRoundTrip: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDistanceKm: Double
        get() = totalDistanceMeters / 1000.0

    val estimatedDurationMinutes: Int
        get() = (estimatedDurationSeconds / 60.0).toInt()

    fun toJson(): JSONObject {
        val wpArray = JSONArray()
        waypoints.forEach { p ->
            wpArray.put(JSONObject().apply {
                put("lat", p.latitude)
                put("lon", p.longitude)
            })
        }

        val stepsArray = JSONArray()
        steps.forEach { s -> stepsArray.put(s.toJson()) }

        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("totalDistanceMeters", totalDistanceMeters)
            put("estimatedDurationSeconds", estimatedDurationSeconds)
            put("waypoints", wpArray)
            put("steps", stepsArray)
            put("isRoundTrip", isRoundTrip)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SavedRoute {
            val wpList = mutableListOf<BikePoint>()
            val wpArray = json.optJSONArray("waypoints") ?: JSONArray()
            for (i in 0 until wpArray.length()) {
                val obj = wpArray.getJSONObject(i)
                wpList.add(BikePoint(obj.getDouble("lat"), obj.getDouble("lon")))
            }

            val stepsList = mutableListOf<RouteStep>()
            val stepsArray = json.optJSONArray("steps") ?: JSONArray()
            for (i in 0 until stepsArray.length()) {
                stepsList.add(RouteStep.fromJson(stepsArray.getJSONObject(i)))
            }

            return SavedRoute(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Rute Sepeda"),
                totalDistanceMeters = json.optDouble("totalDistanceMeters", 0.0),
                estimatedDurationSeconds = json.optDouble("estimatedDurationSeconds", 0.0),
                waypoints = wpList,
                steps = stepsList,
                isRoundTrip = json.optBoolean("isRoundTrip", false),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Riwayat aktivitas gowes lengkap (Strava style)
 */
data class RideRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val totalDistanceMeters: Double,
    val durationMillis: Long,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val targetCotMillis: Long,
    val isCotAchieved: Boolean,
    val caloriesKcal: Int,
    val elevationGainMeters: Double = 0.0,
    val sweatLossLiters: Double = 0.0,
    val recoveryHours: Int = 12
) {
    val totalDistanceKm: Double
        get() = totalDistanceMeters / 1000.0

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("dateMillis", dateMillis)
            put("totalDistanceMeters", totalDistanceMeters)
            put("durationMillis", durationMillis)
            put("avgSpeedKmh", avgSpeedKmh)
            put("maxSpeedKmh", maxSpeedKmh)
            put("targetCotMillis", targetCotMillis)
            put("isCotAchieved", isCotAchieved)
            put("caloriesKcal", caloriesKcal)
            put("elevationGainMeters", elevationGainMeters)
            put("sweatLossLiters", sweatLossLiters)
            put("recoveryHours", recoveryHours)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RideRecord {
            return RideRecord(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Gowes Pagi"),
                dateMillis = json.optLong("dateMillis", System.currentTimeMillis()),
                totalDistanceMeters = json.optDouble("totalDistanceMeters", 0.0),
                durationMillis = json.optLong("durationMillis", 0L),
                avgSpeedKmh = json.optDouble("avgSpeedKmh", 0.0),
                maxSpeedKmh = json.optDouble("maxSpeedKmh", 0.0),
                targetCotMillis = json.optLong("targetCotMillis", 0L),
                isCotAchieved = json.optBoolean("isCotAchieved", true),
                caloriesKcal = json.optInt("caloriesKcal", 0),
                elevationGainMeters = json.optDouble("elevationGainMeters", 0.0),
                sweatLossLiters = json.optDouble("sweatLossLiters", 0.0),
                recoveryHours = json.optInt("recoveryHours", 12)
            )
        }
    }
}

/**
 * Profil kesehatan & fisik pesepeda (Huawei Health style)
 */
data class UserHealthProfile(
    val weightKg: Float = 68.0f,
    val bikeWeightKg: Float = 10.0f,
    val targetCadenceRpm: Int = 85
)
