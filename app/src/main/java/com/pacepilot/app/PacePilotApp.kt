package com.pacepilot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.osmdroid.config.Configuration
import java.io.File

class PacePilotApp : Application() {

    companion object {
        const val CHANNEL_RIDE_ID = "pacepilot_ride_tracking"
        const val CHANNEL_ALERTS_ID = "pacepilot_alerts"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize OSMDroid configuration with custom user agent and cache dir
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "PacePilot-Android-Cycling-App"
        val basePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = File(basePath, "tiles")

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground ride tracking channel (low sound/silent ongoing stats)
            val rideChannel = NotificationChannel(
                CHANNEL_RIDE_ID,
                getString(R.string.channel_ride_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_ride_desc)
                setShowBadge(false)
            }

            // High-priority alerts channel (fueling, rest, speed warnings)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alerts_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(rideChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }
}
