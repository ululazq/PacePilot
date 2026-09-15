package com.pacepilot.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.pacepilot.app.data.model.BikePoint
import com.pacepilot.app.data.model.RideMetrics
import com.pacepilot.app.data.model.RouteProfile
import com.pacepilot.app.data.model.UserSettings
import com.pacepilot.app.service.RideTrackingService
import com.pacepilot.app.ui.screens.ActiveRideScreen
import com.pacepilot.app.ui.screens.RideSummaryDialog
import com.pacepilot.app.ui.screens.RouteSetupScreen
import com.pacepilot.app.ui.screens.SettingsBottomSheet
import com.pacepilot.app.ui.theme.DarkBackground
import com.pacepilot.app.ui.theme.PacePilotTheme

class MainActivity : ComponentActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private val currentLocationState = mutableStateOf<BikePoint?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        checkAndRequestPermissions()

        setContent {
            PacePilotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val metrics by RideTrackingService.metrics.collectAsState()
                    val activeRoute by RideTrackingService.activeRoute.collectAsState()
                    val currentLoc by currentLocationState

                    var showSettings by remember { mutableStateOf(false) }
                    var showSummary by remember { mutableStateOf(false) }
                    var isRideActive by remember { mutableStateOf(false) }

                    LaunchedEffect(metrics.isFinished) {
                        if (metrics.isFinished && isRideActive) {
                            isRideActive = false
                            showSummary = true
                        }
                    }

                    if (isRideActive) {
                        ActiveRideScreen(
                            metrics = metrics,
                            route = activeRoute,
                            onPauseResume = {
                                if (metrics.isPaused) {
                                    sendServiceAction(RideTrackingService.ACTION_RESUME)
                                } else {
                                    sendServiceAction(RideTrackingService.ACTION_PAUSE)
                                }
                            },
                            onStopRide = {
                                sendServiceAction(RideTrackingService.ACTION_STOP)
                                isRideActive = false
                                showSummary = true
                            }
                        )
                    } else {
                        RouteSetupScreen(
                            currentLocation = currentLoc,
                            onStartRide = { route, cotMillis, isSimulation ->
                                RideTrackingService.setRoute(route)
                                val action = if (isSimulation) {
                                    RideTrackingService.ACTION_SIMULATE
                                } else {
                                    RideTrackingService.ACTION_START
                                }
                                val intent = Intent(this@MainActivity, RideTrackingService::class.java).apply {
                                    this.action = action
                                    putExtra(RideTrackingService.EXTRA_TOTAL_DISTANCE, route.totalDistanceMeters)
                                    putExtra(RideTrackingService.EXTRA_COT_MILLIS, cotMillis)
                                    putExtra(RideTrackingService.EXTRA_FUELING_MINS, RideTrackingService.currentSettings.fuelingIntervalMinutes)
                                    putExtra(RideTrackingService.EXTRA_REST_MINS, RideTrackingService.currentSettings.restIntervalMinutes)
                                }
                                ContextCompat.startForegroundService(this@MainActivity, intent)
                                isRideActive = true
                            },
                            onOpenSettings = {
                                showSettings = true
                            }
                        )
                    }

                    if (showSettings) {
                        SettingsBottomSheet(
                            currentSettings = RideTrackingService.currentSettings,
                            onSave = { updatedSettings ->
                                RideTrackingService.updateSettings(updatedSettings)
                            },
                            onDismiss = { showSettings = false }
                        )
                    }

                    if (showSummary) {
                        RideSummaryDialog(
                            metrics = metrics,
                            onDismiss = { showSummary = false }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            startLocationUpdates()
        }
    }

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    5.0f,
                    this
                )
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                    currentLocationState.value = BikePoint(it.latitude, it.longitude)
                }
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    10.0f,
                    this
                )
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                    currentLocationState.value = BikePoint(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, RideTrackingService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onLocationChanged(location: Location) {
        currentLocationState.value = BikePoint(location.latitude, location.longitude, location.altitude)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
    }
}
