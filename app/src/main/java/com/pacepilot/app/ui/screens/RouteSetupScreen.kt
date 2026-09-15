package com.pacepilot.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepilot.app.data.api.RoutingService
import com.pacepilot.app.data.model.BikePoint
import com.pacepilot.app.data.model.RouteProfile
import com.pacepilot.app.pacing.PacingEngine
import com.pacepilot.app.ui.components.OsmMapView
import com.pacepilot.app.ui.theme.BrightCyan
import com.pacepilot.app.ui.theme.CardOverlay
import com.pacepilot.app.ui.theme.DarkBackground
import com.pacepilot.app.ui.theme.DarkCard
import com.pacepilot.app.ui.theme.DarkCardBorder
import com.pacepilot.app.ui.theme.NeonGreen
import com.pacepilot.app.ui.theme.TextPrimary
import com.pacepilot.app.ui.theme.TextSecondary
import com.pacepilot.app.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun RouteSetupScreen(
    currentLocation: BikePoint?,
    onStartRide: (route: RouteProfile, cotMillis: Long, isSimulation: Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val routingService = remember { RoutingService() }

    // Start location default (current GPS or Monas Jakarta as fallback)
    val startPoint = currentLocation ?: BikePoint(-6.1754, 106.8272)

    var destinationPoint by remember { mutableStateOf<BikePoint?>(null) }
    var destinationName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    var calculatedRoute by remember { mutableStateOf<RouteProfile?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    // Target COT (in hours, e.g. 2.0 = 2 hours)
    var targetCotHours by remember { mutableFloatStateOf(2.0f) }

    val presetCotOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. OpenStreetMap
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            route = calculatedRoute,
            currentLocation = currentLocation,
            onMapTap = { tappedPoint ->
                destinationPoint = tappedPoint
                destinationName = String.format(Locale.US, "%.4f, %.4f", tappedPoint.latitude, tappedPoint.longitude)

                // Auto calculate bike route on tap
                coroutineScope.launch {
                    isCalculatingRoute = true
                    val res = routingService.getBicycleRoute(startPoint, tappedPoint, "Lokasi Saya", destinationName)
                    isCalculatingRoute = false
                    res.onSuccess { r ->
                        calculatedRoute = r
                    }.onFailure {
                        Toast.makeText(context, "Gagal mengambil rute: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        // 2. Top Bar Search & Settings Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari destinasi atau tap di peta...", fontSize = 13.sp, color = TextSecondary) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Cari", tint = BrightCyan)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardOverlay),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CardOverlay)
                        .border(1.dp, DarkCardBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = TextPrimary)
                }
            }

            // Search button trigger if user types text
            if (searchQuery.isNotEmpty()) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCalculatingRoute = true
                            val results = routingService.searchPlace(searchQuery)
                            if (results.isNotEmpty()) {
                                val topMatch = results.first()
                                destinationPoint = topMatch.point
                                destinationName = topMatch.displayName.take(30) + "..."
                                val r = routingService.getBicycleRoute(startPoint, topMatch.point, "Lokasi Saya", destinationName)
                                r.onSuccess { calculatedRoute = it }
                            } else {
                                Toast.makeText(context, "Destinasi tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                            isCalculatingRoute = false
                        }
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cari & Buat Rute", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // 3. Loading Indicator
        if (isCalculatingRoute) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardOverlay)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Menghitung rute sepeda...", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 4. Bottom Setup & COT Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(DarkBackground)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(20.dp)
        ) {
            // Header: Route summary or prompt
            if (calculatedRoute != null) {
                val route = calculatedRoute!!
                val cotMillis = (targetCotHours * 3600000L).toLong()
                val targetSpeedKmh = PacingEngine.calculateInitialTargetSpeed(route.totalDistanceMeters, cotMillis)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rute Sepeda Ditemukan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f km (Est. %d menit)", route.totalDistanceKm, route.estimatedDurationMinutes),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Target Average Speed Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .border(1.dp, BrightCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TARGET SPEED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text(
                                text = String.format(Locale.US, "%.1f km/j", targetSpeedKmh),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = BrightCyan
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tap titik tujuan di peta untuk buat rute",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // COT Selector Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Target Waktu (COT):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                val hours = targetCotHours.toInt()
                val minutes = ((targetCotHours - hours) * 60).toInt()
                Text(
                    text = "${hours}j ${if (minutes > 0) "${minutes}m" else ""}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = WarningAmber
                )
            }

            // Quick Preset Chips for COT
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetCotOptions) { preset ->
                    val isSelected = targetCotHours == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) WarningAmber else DarkCard)
                            .clickable { targetCotHours = preset }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${preset} Jam",
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = targetCotHours,
                onValueChange = { targetCotHours = it },
                valueRange = 0.5f..8.0f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = WarningAmber,
                    activeTrackColor = WarningAmber,
                    inactiveTrackColor = DarkCard
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Start Ride Buttons (Real GPS & Simulation)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        calculatedRoute?.let { route ->
                            val cotMillis = (targetCotHours * 3600000L).toLong()
                            onStartRide(route, cotMillis, false)
                        } ?: Toast.makeText(context, "Silakan pilih rute terlebih dahulu", Toast.LENGTH_SHORT).show()
                    },
                    enabled = calculatedRoute != null,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        disabledContainerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mulai Gowes", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Simulation Button for Testing Indoors
                Button(
                    onClick = {
                        calculatedRoute?.let { route ->
                            val cotMillis = (targetCotHours * 3600000L).toLong()
                            onStartRide(route, cotMillis, true)
                        } ?: Toast.makeText(context, "Silakan pilih rute terlebih dahulu", Toast.LENGTH_SHORT).show()
                    },
                    enabled = calculatedRoute != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCard,
                        disabledContainerColor = DarkCard
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Simulasi", color = BrightCyan, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
