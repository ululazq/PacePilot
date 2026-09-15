package com.pacepilot.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.pacepilot.app.MainActivity
import com.pacepilot.app.PacePilotApp
import com.pacepilot.app.R
import com.pacepilot.app.data.model.BikePoint
import com.pacepilot.app.data.model.PacingStatus
import com.pacepilot.app.data.model.RideMetrics
import com.pacepilot.app.data.model.RouteProfile
import com.pacepilot.app.data.model.UserSettings
import com.pacepilot.app.pacing.PacingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class RideTrackingService : Service(), LocationListener {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null
    private var simulationJob: Job? = null

    private lateinit var locationManager: LocationManager
    private lateinit var notificationManager: NotificationManager
    private var vibrator: Vibrator? = null

    private var isTracking = false
    private var isPaused = false

    private var rideStartTime = 0L
    private var totalPauseDuration = 0L
    private var pauseStartTime = 0L

    private var lastFuelingTriggerTime = 0L
    private var lastRestTriggerTime = 0L
    private var lastSpeedAlertTime = 0L

    private var lastLocation: Location? = null
    private var totalCoveredMeters = 0.0
    private var currentSpeedKmh = 0.0
    private var maxSpeedKmh = 0.0

    private var smoothingSpeedBuffer = mutableListOf<Double>()

    companion object {
        const val ACTION_START = "com.pacepilot.app.START"
        const val ACTION_PAUSE = "com.pacepilot.app.PAUSE"
        const val ACTION_RESUME = "com.pacepilot.app.RESUME"
        const val ACTION_STOP = "com.pacepilot.app.STOP"
        const val ACTION_SIMULATE = "com.pacepilot.app.SIMULATE"

        const val EXTRA_TOTAL_DISTANCE = "extra_total_distance"
        const val EXTRA_COT_MILLIS = "extra_cot_millis"
        const val EXTRA_FUELING_MINS = "extra_fueling_mins"
        const val EXTRA_REST_MINS = "extra_rest_mins"

        private const val NOTIFICATION_ID_FOREGROUND = 1001
        private const val NOTIFICATION_ID_FUELING = 1002
        private const val NOTIFICATION_ID_REST = 1003
        private const val NOTIFICATION_ID_SPEED = 1004

        private val _metrics = MutableStateFlow(RideMetrics())
        val metrics: StateFlow<RideMetrics> = _metrics.asStateFlow()

        private val _activeRoute = MutableStateFlow<RouteProfile?>(null)
        val activeRoute: StateFlow<RouteProfile?> = _activeRoute.asStateFlow()

        var currentSettings = UserSettings()

        fun setRoute(route: RouteProfile) {
            _activeRoute.value = route
        }

        fun updateSettings(settings: UserSettings) {
            currentSettings = settings
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val totalDistance = intent.getDoubleExtra(EXTRA_TOTAL_DISTANCE, 0.0)
                val cotMillis = intent.getLongExtra(EXTRA_COT_MILLIS, 3600000L)
                val fuelingMins = intent.getIntExtra(EXTRA_FUELING_MINS, 30)
                val restMins = intent.getIntExtra(EXTRA_REST_MINS, 60)
                currentSettings = currentSettings.copy(
                    fuelingIntervalMinutes = fuelingMins,
                    restIntervalMinutes = restMins
                )
                startRide(totalDistance, cotMillis, isSimulation = false)
            }
            ACTION_SIMULATE -> {
                val totalDistance = intent.getDoubleExtra(EXTRA_TOTAL_DISTANCE, 0.0)
                val cotMillis = intent.getLongExtra(EXTRA_COT_MILLIS, 3600000L)
                startRide(totalDistance, cotMillis, isSimulation = true)
            }
            ACTION_PAUSE -> pauseRide()
            ACTION_RESUME -> resumeRide()
            ACTION_STOP -> stopRide()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startRide(totalDistance: Double, cotMillis: Long, isSimulation: Boolean) {
        isTracking = true
        isPaused = false
        rideStartTime = System.currentTimeMillis()
        totalPauseDuration = 0L
        lastFuelingTriggerTime = rideStartTime
        lastRestTriggerTime = rideStartTime
        lastSpeedAlertTime = rideStartTime
        totalCoveredMeters = 0.0
        currentSpeedKmh = 0.0
        maxSpeedKmh = 0.0
        smoothingSpeedBuffer.clear()
        lastLocation = null

        val initialResult = PacingEngine.evaluatePacing(
            totalDistanceMeters = totalDistance,
            distanceCoveredMeters = 0.0,
            targetCotMillis = cotMillis,
            elapsedTimeMillis = 0L,
            currentSpeedKmh = 0.0,
            speedToleranceKmh = currentSettings.speedAlertThresholdKmh
        )

        _metrics.value = RideMetrics(
            totalDistanceMeters = totalDistance,
            distanceCoveredMeters = 0.0,
            distanceRemainingMeters = totalDistance,
            targetCotMillis = cotMillis,
            elapsedTimeMillis = 0L,
            remainingTimeMillis = cotMillis,
            currentSpeedKmh = 0.0,
            averageSpeedKmh = 0.0,
            requiredSpeedKmh = initialResult.requiredSpeedKmh,
            pacingStatus = initialResult.status,
            pacingAdvice = initialResult.advice,
            nextFuelingSeconds = currentSettings.fuelingIntervalMinutes * 60L,
            nextRestSeconds = currentSettings.restIntervalMinutes * 60L,
            isPaused = false,
            isFinished = false
        )

        startForeground(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification())

        if (isSimulation) {
            startSimulation(totalDistance, cotMillis)
        } else {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        2.0f,
                        this
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2000L,
                        5.0f,
                        this
                    )
                }
            } catch (e: SecurityException) {
                // Permission not granted handled in UI
            }
        }

        startTimerTicker()
    }

    private fun startTimerTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && isTracking) {
                delay(1000L)
                if (!isPaused) {
                    processTick()
                }
            }
        }
    }

    private fun processTick() {
        val now = System.currentTimeMillis()
        val elapsed = now - rideStartTime - totalPauseDuration
        val cur = _metrics.value
        val remainingCot = cur.targetCotMillis - elapsed

        // Hitung pacing
        val pacing = PacingEngine.evaluatePacing(
            totalDistanceMeters = cur.totalDistanceMeters,
            distanceCoveredMeters = totalCoveredMeters,
            targetCotMillis = cur.targetCotMillis,
            elapsedTimeMillis = elapsed,
            currentSpeedKmh = currentSpeedKmh,
            speedToleranceKmh = currentSettings.speedAlertThresholdKmh
        )

        // Hitung mundur fueling dan rest
        val fuelingIntervalMs = currentSettings.fuelingIntervalMinutes * 60 * 1000L
        val restIntervalMs = currentSettings.restIntervalMinutes * 60 * 1000L

        val timeSinceFueling = now - lastFuelingTriggerTime
        val fuelingRemainingSec = maxOf(0L, (fuelingIntervalMs - timeSinceFueling) / 1000L)

        val timeSinceRest = now - lastRestTriggerTime
        val restRemainingSec = maxOf(0L, (restIntervalMs - timeSinceRest) / 1000L)

        // Trigger notifikasi Fueling jika waktunya tiba
        if (timeSinceFueling >= fuelingIntervalMs && elapsed > 10000L) {
            triggerFuelingAlert()
            lastFuelingTriggerTime = now
        }

        // Trigger notifikasi Rest jika waktunya tiba
        if (timeSinceRest >= restIntervalMs && elapsed > 10000L) {
            triggerRestAlert()
            lastRestTriggerTime = now
        }

        // Trigger notifikasi Speed jika tertinggal terus menerus (tiap 3 menit)
        if (pacing.status == PacingStatus.SPEED_UP && (now - lastSpeedAlertTime > 180000L) && elapsed > 60000L) {
            triggerSpeedAlert(pacing.advice)
            lastSpeedAlertTime = now
        }

        _metrics.value = cur.copy(
            distanceCoveredMeters = totalCoveredMeters,
            distanceRemainingMeters = pacing.remainingDistanceMeters,
            elapsedTimeMillis = elapsed,
            remainingTimeMillis = remainingCot,
            currentSpeedKmh = currentSpeedKmh,
            averageSpeedKmh = pacing.avgSpeedKmh,
            requiredSpeedKmh = pacing.requiredSpeedKmh,
            pacingStatus = pacing.status,
            pacingAdvice = pacing.advice,
            nextFuelingSeconds = fuelingRemainingSec,
            nextRestSeconds = restRemainingSec
        )

        // Update foreground notification tiap 2 detik
        if (elapsed % 2000L < 1000L) {
            notificationManager.notify(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification())
        }
    }

    private fun pauseRide() {
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
        _metrics.value = _metrics.value.copy(isPaused = true)
        notificationManager.notify(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification())
    }

    private fun resumeRide() {
        if (isPaused) {
            val pausedDuration = System.currentTimeMillis() - pauseStartTime
            totalPauseDuration += pausedDuration
            isPaused = false
            _metrics.value = _metrics.value.copy(isPaused = false)
            notificationManager.notify(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification())
        }
    }

    private fun stopRide() {
        isTracking = false
        tickerJob?.cancel()
        simulationJob?.cancel()
        locationManager.removeUpdates(this)

        _metrics.value = _metrics.value.copy(isFinished = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onLocationChanged(location: Location) {
        if (!isTracking || isPaused) return

        if (lastLocation != null) {
            val delta = lastLocation!!.distanceTo(location).toDouble()
            // Filter anomali GPS loncat > 80 m/s (~280 km/h)
            if (delta in 1.0..80.0) {
                totalCoveredMeters += delta
            }
        }
        lastLocation = location

        // Speed calculation
        val speedKmh = if (location.hasSpeed()) {
            (location.speed * 3.6).toDouble()
        } else {
            0.0
        }

        smoothingSpeedBuffer.add(speedKmh)
        if (smoothingSpeedBuffer.size > 4) smoothingSpeedBuffer.removeAt(0)
        currentSpeedKmh = smoothingSpeedBuffer.average()
        if (currentSpeedKmh > maxSpeedKmh) maxSpeedKmh = currentSpeedKmh

        _metrics.value = _metrics.value.copy(
            currentPosition = BikePoint(location.latitude, location.longitude, location.altitude)
        )
    }

    /**
     * Mode simulasi gowes di sepanjang rute untuk testing aplikasi
     */
    private fun startSimulation(totalDistance: Double, cotMillis: Long) {
        val waypoints = _activeRoute.value?.waypoints ?: emptyList()
        if (waypoints.isEmpty()) return

        simulationJob?.cancel()
        simulationJob = serviceScope.launch {
            var index = 0
            val speedMps = 6.5 // ~23.4 km/jam sepeda
            currentSpeedKmh = speedMps * 3.6

            while (isActive && isTracking && index < waypoints.size) {
                delay(1000L)
                if (!isPaused) {
                    val currentPoint = waypoints[index]
                    totalCoveredMeters = (index.toDouble() / waypoints.size.toDouble()) * totalDistance
                    _metrics.value = _metrics.value.copy(
                        currentPosition = currentPoint,
                        currentSpeedKmh = currentSpeedKmh
                    )
                    index += 2
                    if (index >= waypoints.size) {
                        totalCoveredMeters = totalDistance
                        break
                    }
                }
            }
        }
    }

    private fun triggerFuelingAlert() {
        triggerHaptic()
        val notif = NotificationCompat.Builder(this, PacePilotApp.CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🍌 Waktunya Fueling!")
            .setContentText("Minum air/elektrolit dan konsumsi camilan energi untuk menjaga stamina.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_FUELING, notif)
    }

    private fun triggerRestAlert() {
        triggerHaptic()
        val notif = NotificationCompat.Builder(this, PacePilotApp.CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("☕ Waktunya Istirahat Singkat!")
            .setContentText("Luangkan 2-3 menit: regangkan otot kaki & periksa hidrasi tubuh.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_REST, notif)
    }

    private fun triggerSpeedAlert(advice: String) {
        triggerHaptic()
        val notif = NotificationCompat.Builder(this, PacePilotApp.CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ Penyesuaian Kecepatan (COT)")
            .setContentText(advice)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_SPEED, notif)
    }

    private fun triggerHaptic() {
        if (!currentSettings.enableVibrationAlerts) return
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 300, 150, 300), -1)
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        val cur = _metrics.value
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remTimeMinutes = maxOf(0L, cur.remainingTimeMillis / 60000L)
        val contentText = String.format(
            Locale.US,
            "Sisa: %.1f km | COT: %dm | Wajib: %.1f km/j (Kini: %.1f km/j)",
            cur.distanceRemainingKm,
            remTimeMinutes,
            cur.requiredSpeedKmh,
            cur.currentSpeedKmh
        )

        return NotificationCompat.Builder(this, PacePilotApp.CHANNEL_RIDE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚴 PacePilot Gowes: ${cur.pacingAdvice}")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
