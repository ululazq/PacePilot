package com.pacepilot.app.data.repository

import android.content.Context
import com.pacepilot.app.data.model.RideRecord
import org.json.JSONArray
import java.io.File

class ActivityRepository(private val context: Context) {

    private val storageFile by lazy {
        File(context.filesDir, "ride_activities.json")
    }

    /**
     * Mengambil seluruh daftar riwayat aktivitas gowes
     */
    fun getAllActivities(): List<RideRecord> {
        return try {
            if (!storageFile.exists()) return emptyList()
            val content = storageFile.readText()
            val array = JSONArray(content)
            val list = mutableListOf<RideRecord>()
            for (i in 0 until array.length()) {
                list.add(RideRecord.fromJson(array.getJSONObject(i)))
            }
            list.sortedByDescending { it.dateMillis }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Menyimpan sesi gowes yang baru saja selesai
     */
    fun saveActivity(record: RideRecord) {
        val currentList = getAllActivities().toMutableList()
        currentList.add(0, record)
        persistList(currentList)
    }

    /**
     * Menghapus catatan riwayat
     */
    fun deleteActivity(id: String) {
        val currentList = getAllActivities().toMutableList()
        currentList.removeAll { it.id == id }
        persistList(currentList)
    }

    private fun persistList(list: List<RideRecord>) {
        try {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            storageFile.writeText(array.toString())
        } catch (e: Exception) {
            // Handled
        }
    }

    /**
     * Total statistik akumulatif seluruh aktivitas
     */
    fun getSummaryStats(): ActivitySummaryStats {
        val activities = getAllActivities()
        val totalDistanceKm = activities.sumOf { it.totalDistanceKm }
        val totalDurationMillis = activities.sumOf { it.durationMillis }
        val totalCalories = activities.sumOf { it.caloriesKcal }
        val maxSpeed = activities.maxOfOrNull { it.maxSpeedKmh } ?: 0.0
        val longestDistanceKm = activities.maxOfOrNull { it.totalDistanceKm } ?: 0.0
        val cotSuccessCount = activities.count { it.isCotAchieved }

        return ActivitySummaryStats(
            totalRides = activities.size,
            totalDistanceKm = totalDistanceKm,
            totalDurationMillis = totalDurationMillis,
            totalCaloriesKcal = totalCalories,
            maxSpeedKmh = maxSpeed,
            longestDistanceKm = longestDistanceKm,
            cotSuccessCount = cotSuccessCount
        )
    }
}

data class ActivitySummaryStats(
    val totalRides: Int,
    val totalDistanceKm: Double,
    val totalDurationMillis: Long,
    val totalCaloriesKcal: Int,
    val maxSpeedKmh: Double,
    val longestDistanceKm: Double,
    val cotSuccessCount: Int
)
