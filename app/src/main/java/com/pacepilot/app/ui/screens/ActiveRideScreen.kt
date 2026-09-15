package com.pacepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepilot.app.data.model.RideMetrics
import com.pacepilot.app.data.model.RouteProfile
import com.pacepilot.app.ui.components.OsmMapView
import com.pacepilot.app.ui.components.PacingBadge
import com.pacepilot.app.ui.components.SpeedGauge
import com.pacepilot.app.ui.theme.AlertRed
import com.pacepilot.app.ui.theme.BrightCyan
import com.pacepilot.app.ui.theme.DarkBackground
import com.pacepilot.app.ui.theme.DarkCard
import com.pacepilot.app.ui.theme.DarkCardBorder
import com.pacepilot.app.ui.theme.NeonGreen
import com.pacepilot.app.ui.theme.TextPrimary
import com.pacepilot.app.ui.theme.TextSecondary
import com.pacepilot.app.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun ActiveRideScreen(
    metrics: RideMetrics,
    route: RouteProfile?,
    onPauseResume: () -> Unit,
    onStopRide: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Top Section: Map showing current position on the route
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                route = route,
                currentLocation = metrics.currentPosition,
                enableLocationOverlay = true
            )

            // Overlaid Mini COT Status at top of map
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkBackground.copy(alpha = 0.85f))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val remSeconds = maxOf(0L, metrics.remainingTimeMillis / 1000L)
                val cotHrs = remSeconds / 3600
                val cotMins = (remSeconds % 3600) / 60
                val cotSecs = remSeconds % 60

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SISA COT: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format(Locale.US, "%02d:%02d:%02d", cotHrs, cotMins, cotSecs),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (metrics.remainingTimeMillis > 0) WarningAmber else AlertRed
                    )
                }
            }
        }

        // 2. Bottom Section: Cockpit HUD Dashboard
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.35f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DarkBackground)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Speedometer & Required Speed
            SpeedGauge(
                currentSpeedKmh = metrics.currentSpeedKmh,
                requiredSpeedKmh = metrics.requiredSpeedKmh,
                pacingStatus = metrics.pacingStatus
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Pacing Guidance Badge
            PacingBadge(
                status = metrics.pacingStatus,
                advice = metrics.pacingAdvice
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Distance & Progress Metrics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TERTEMPUH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.1f km", metrics.distanceCoveredKm),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("SISA JARAK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.1f km", metrics.distanceRemainingKm),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { metrics.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeonGreen,
                    trackColor = Color(0xFF0D1420)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fueling & Rest Reminder Timers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fueling Countdown Card
                val fuelMins = metrics.nextFuelingSeconds / 60
                val fuelSecs = metrics.nextFuelingSeconds % 60
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard)
                        .padding(12.dp)
                ) {
                    Column {
                        Text("🍌 FUELING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", fuelMins, fuelSecs),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                }

                // Rest Countdown Card
                val restMins = metrics.nextRestSeconds / 60
                val restSecs = metrics.nextRestSeconds % 60
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard)
                        .padding(12.dp)
                ) {
                    Column {
                        Text("☕ ISTIRAHAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrightCyan)
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", restMins, restSecs),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Pause / Resume and Stop Ride
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (metrics.isPaused) NeonGreen else DarkCard
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (metrics.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = if (metrics.isPaused) Color.Black else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (metrics.isPaused) "Lanjutkan" else "Jeda",
                        color = if (metrics.isPaused) Color.Black else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onStopRide,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Akhiri", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
