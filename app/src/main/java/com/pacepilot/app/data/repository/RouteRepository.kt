package com.pacepilot.app.data.repository

import android.content.Context
import android.util.Xml
import com.pacepilot.app.data.model.BikePoint
import com.pacepilot.app.data.model.ManeuverType
import com.pacepilot.app.data.model.RouteStep
import com.pacepilot.app.data.model.SavedRoute
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RouteRepository(private val context: Context) {

    private val storageFile by lazy {
        File(context.filesDir, "saved_routes.json")
    }

    /**
     * Mengambil semua daftar rute tersimpan
     */
    fun getAllRoutes(): List<SavedRoute> {
        return try {
            if (!storageFile.exists()) return emptyList()
            val content = storageFile.readText()
            val array = JSONArray(content)
            val list = mutableListOf<SavedRoute>()
            for (i in 0 until array.length()) {
                list.add(SavedRoute.fromJson(array.getJSONObject(i)))
            }
            list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Menyimpan rute baru atau memperbarui rute yang ada
     */
    fun saveRoute(route: SavedRoute) {
        val currentList = getAllRoutes().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == route.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = route
        } else {
            currentList.add(0, route)
        }
        persistList(currentList)
    }

    /**
     * Menghapus rute dari daftar tersimpan
     */
    fun deleteRoute(id: String) {
        val currentList = getAllRoutes().toMutableList()
        currentList.removeAll { it.id == id }
        persistList(currentList)
    }

    private fun persistList(list: List<SavedRoute>) {
        try {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            storageFile.writeText(array.toString())
        } catch (e: Exception) {
            // Logged or handled
        }
    }

    /**
     * Ekspor rute menjadi format standar GPX 1.1 XML (dapat dibuka di Garmin, Wahoo, Strava, Komoot)
     */
    fun exportToGpx(route: SavedRoute): String {
        val sb = StringBuilder()
        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(route.createdAt))

        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="PacePilot Cycling App" xmlns="http://www.topografix.com/GPX/1/1">""").append("\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(route.title)).append("</name>\n")
        sb.append("    <time>").append(isoDate).append("</time>\n")
        sb.append("  </metadata>\n")
        sb.append("  <trk>\n")
        sb.append("    <name>").append(escapeXml(route.title)).append("</name>\n")
        sb.append("    <trkseg>\n")

        for (pt in route.waypoints) {
            sb.append(String.format(Locale.US, "      <trkpt lat=\"%.6f\" lon=\"%.6f\">\n", pt.latitude, pt.longitude))
            if (pt.altitude > 0.0) {
                sb.append(String.format(Locale.US, "        <ele>%.1f</ele>\n", pt.altitude))
            }
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>")

        return sb.toString()
    }

    /**
     * Impor file GPX dari string XML menjadi objek SavedRoute
     */
    fun importFromGpx(gpxContent: String, defaultTitle: String = "Impor GPX"): SavedRoute? {
        return try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(gpxContent))

            var eventType = parser.eventType
            val waypoints = mutableListOf<BikePoint>()
            var title = defaultTitle

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "name" -> {
                            val text = parser.nextText()
                            if (text.isNotBlank() && title == defaultTitle) {
                                title = text
                            }
                        }
                        "trkpt", "wpt" -> {
                            val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            if (lat != null && lon != null) {
                                waypoints.add(BikePoint(lat, lon))
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            if (waypoints.isEmpty()) return null

            // Hitung estimasi jarak dari track points
            var totalDist = 0.0
            for (i in 0 until waypoints.size - 1) {
                totalDist += calculateDistance(waypoints[i], waypoints[i + 1])
            }
            val estDuration = totalDist / 5.5 // ~20 km/jam

            SavedRoute(
                title = title,
                totalDistanceMeters = totalDist,
                estimatedDurationSeconds = estDuration,
                waypoints = waypoints,
                steps = emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun calculateDistance(p1: BikePoint, p2: BikePoint): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
