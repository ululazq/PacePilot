package com.pacepilot.app.data.api

import com.pacepilot.app.data.model.BikePoint
import com.pacepilot.app.data.model.ManeuverType
import com.pacepilot.app.data.model.RouteProfile
import com.pacepilot.app.data.model.RouteStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RoutingService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Mengambil rute khusus sepeda dari OSRM OpenStreetMap routing endpoint
     */
    suspend fun getBicycleRoute(
        start: BikePoint,
        destination: BikePoint,
        startName: String = "Titik Awal",
        destName: String = "Tujuan"
    ): Result<RouteProfile> = withContext(Dispatchers.IO) {
        val primaryUrl = "https://routing.openstreetmap.de/routed-bike/route/v1/driving/" +
                "${start.longitude},${start.latitude};${destination.longitude},${destination.latitude}" +
                "?overview=full&geometries=geojson&steps=true"

        val fallbackUrl = "https://router.project-osrm.org/route/v1/driving/" +
                "${start.longitude},${start.latitude};${destination.longitude},${destination.latitude}" +
                "?overview=full&geometries=geojson"

        // Coba endpoint khusus sepeda terlebih dahulu
        var result = fetchRouteFromUrl(primaryUrl, start, destination, startName, destName)
        if (result.isFailure) {
            // Jika gagal/timeout, coba fallback endpoint
            result = fetchRouteFromUrl(fallbackUrl, start, destination, startName, destName)
        }

        if (result.isFailure) {
            // Fallback offline / direct path jika tidak ada koneksi internet sama sekali
            val distance = calculateHaversineDistance(start, destination)
            val fallbackWaypoints = generateInterpolatedPath(start, destination, 20)
            val estDurationSeconds = (distance / 4.16) // ~15 km/h sepeda santai
            Result.success(
                RouteProfile(
                    waypoints = fallbackWaypoints,
                    totalDistanceMeters = distance,
                    estimatedDurationSeconds = estDurationSeconds,
                    startName = startName,
                    destinationName = destName
                )
            )
        } else {
            result
        }
    }

    private fun fetchRouteFromUrl(
        url: String,
        start: BikePoint,
        destination: BikePoint,
        startName: String,
        destName: String
    ): Result<RouteProfile> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PacePilot-Bicycle-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP Error: ${response.code}"))
            }

            val body = response.body?.string() ?: return Result.failure(Exception("Empty body"))
            val json = JSONObject(body)

            val routes = json.optJSONArray("routes")
            if (routes == null || routes.length() == 0) {
                return Result.failure(Exception("Rute tidak ditemukan"))
            }

            val routeObj = routes.getJSONObject(0)
            val distanceMeters = routeObj.getDouble("distance")
            val durationSeconds = routeObj.getDouble("duration")

            val geometry = routeObj.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            val waypoints = mutableListOf<BikePoint>()
            for (i in 0 until coordinates.length()) {
                val point = coordinates.getJSONArray(i)
                val lon = point.getDouble(0)
                val lat = point.getDouble(1)
                waypoints.add(BikePoint(lat, lon))
            }

            // Ekstrak langkah navigasi turn-by-turn dari OSRM legs
            val stepsList = mutableListOf<RouteStep>()
            val legs = routeObj.optJSONArray("legs")
            if (legs != null && legs.length() > 0) {
                val leg = legs.getJSONObject(0)
                val steps = leg.optJSONArray("steps")
                if (steps != null) {
                    for (s in 0 until steps.length()) {
                        val stepObj = steps.getJSONObject(s)
                        val streetName = stepObj.optString("name", "")
                        val stepDist = stepObj.optDouble("distance", 0.0)
                        val stepDur = stepObj.optDouble("duration", 0.0)

                        val manObj = stepObj.optJSONObject("maneuver")
                        val manTypeStr = manObj?.optString("type", "") ?: ""
                        val manModStr = manObj?.optString("modifier", null)
                        val manType = ManeuverType.fromOsrm(manTypeStr, manModStr)

                        val locArray = manObj?.optJSONArray("location")
                        val stepPt = if (locArray != null && locArray.length() >= 2) {
                            BikePoint(locArray.getDouble(1), locArray.getDouble(0))
                        } else {
                            waypoints.firstOrNull() ?: start
                        }

                        val instruction = buildInstruction(manType, streetName)
                        stepsList.add(RouteStep(instruction, streetName, manType, stepDist, stepDur, stepPt))
                    }
                }
            }

            Result.success(
                RouteProfile(
                    waypoints = waypoints,
                    totalDistanceMeters = distanceMeters,
                    estimatedDurationSeconds = durationSeconds,
                    startName = startName,
                    destinationName = destName,
                    steps = stepsList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildInstruction(maneuver: ManeuverType, street: String): String {
        val streetSuffix = if (street.isNotBlank()) " ke $street" else ""
        return when (maneuver) {
            ManeuverType.DEPART -> "Mulai perjalanan$streetSuffix"
            ManeuverType.TURN_LEFT -> "Belok kiri$streetSuffix"
            ManeuverType.TURN_RIGHT -> "Belok kanan$streetSuffix"
            ManeuverType.SLIGHT_LEFT -> "Ambil serong kiri$streetSuffix"
            ManeuverType.SLIGHT_RIGHT -> "Ambil serong kanan$streetSuffix"
            ManeuverType.SHARP_LEFT -> "Belok tajam ke kiri$streetSuffix"
            ManeuverType.SHARP_RIGHT -> "Belok tajam ke kanan$streetSuffix"
            ManeuverType.U_TURN -> "Lakukan putar balik$streetSuffix"
            ManeuverType.STRAIGHT -> "Terus lurus$streetSuffix"
            ManeuverType.ROUNDABOUT -> "Masuk bundaran$streetSuffix"
            ManeuverType.ARRIVE -> "Anda telah tiba di tujuan"
        }
    }

    /**
     * Mencari tempat atau alamat berdasarkan query teks menggunakan OpenStreetMap Nominatim
     */
    suspend fun searchPlace(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&addressdetails=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PacePilot-Bicycle-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val array = JSONArray(body)
            val results = mutableListOf<SearchResult>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val displayName = item.getString("display_name")
                val lat = item.getDouble("lat")
                val lon = item.getDouble("lon")
                results.add(SearchResult(displayName, BikePoint(lat, lon)))
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun calculateHaversineDistance(p1: BikePoint, p2: BikePoint): Double {
            val r = 6371000.0 // meters
            val dLat = Math.toRadians(p2.latitude - p1.latitude)
            val dLon = Math.toRadians(p2.longitude - p1.longitude)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        private fun generateInterpolatedPath(start: BikePoint, dest: BikePoint, steps: Int): List<BikePoint> {
            val list = mutableListOf<BikePoint>()
            for (i in 0..steps) {
                val fraction = i.toDouble() / steps.toDouble()
                val lat = start.latitude + (dest.latitude - start.latitude) * fraction
                val lon = start.longitude + (dest.longitude - start.longitude) * fraction
                list.add(BikePoint(lat, lon))
            }
            return list
        }
    }
}

data class SearchResult(
    val displayName: String,
    val point: BikePoint
)
