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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.runtime.LaunchedEffect
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
import com.pacepilot.app.ui.theme.AlertRed
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
    onOpenSettings: () -> Unit,
    onSaveRoute: ((RouteProfile) -> Unit)? = null,
    initialRoute: RouteProfile? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val routingService = remember { RoutingService() }

    // Start location default (current GPS or Monas Jakarta as fallback)
    val startPoint = currentLocation ?: BikePoint(-6.1754, 106.8272)

    var intermediateWaypoints by remember { mutableStateOf<List<BikePoint>>(emptyList()) }
    var destinationPoint by remember { mutableStateOf<BikePoint?>(null) }
    var destinationName by remember { mutableStateOf("") }
    var isRoundTrip by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var calculatedRoute by remember(initialRoute) { mutableStateOf(initialRoute) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    // Sinkronisasi data saat menerima initialRoute dari koleksi rute tersimpan
    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            calculatedRoute = initialRoute
            intermediateWaypoints = initialRoute.userWaypoints
            destinationPoint = initialRoute.waypoints.lastOrNull()
            destinationName = initialRoute.destinationName
            isRoundTrip = initialRoute.isRoundTrip
        }
    }

    // Target COT (in hours, e.g. 2.0 = 2 hours)
    var targetCotHours by remember { mutableFloatStateOf(2.0f) }
    val presetCotOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f)

    // Fungsi pemanggil rute multi-point dan roundtrip
    fun computeMultiRoute(
        start: BikePoint,
        intermediates: List<BikePoint>,
        dest: BikePoint?,
        roundTripEnabled: Boolean
    ) {
        if (dest == null && !roundTripEnabled) {
            calculatedRoute = null
            return
        }

        val points = mutableListOf<BikePoint>()
        points.add(start)
        points.addAll(intermediates)
        if (dest != null) {
            points.add(dest)
        } else if (roundTripEnabled && intermediates.isNotEmpty()) {
            points.add(intermediates.last())
        }

        if (points.size < 2) {
            calculatedRoute = null
            return
        }

        coroutineScope.launch {
            isCalculatingRoute = true
            val targetLabel = if (roundTripEnabled) "Loop Roundtrip" else destinationName.ifBlank { "Tujuan" }
            val res = routingService.getMultiPointRoute(
                points = points,
                startName = "Lokasi Saya",
                destName = targetLabel,
                isRoundTrip = roundTripEnabled
            )
            isCalculatingRoute = false
            res.onSuccess { r ->
                calculatedRoute = r
            }.onFailure {
                Toast.makeText(context, "Gagal membuat rute: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Daftar seluruh titik pending untuk ditampilkan sebagai pin di peta
    val allSelectedPoints = remember(intermediateWaypoints, destinationPoint) {
        val list = mutableListOf<BikePoint>()
        list.addAll(intermediateWaypoints)
        destinationPoint?.let { list.add(it) }
        list
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. OpenStreetMap dengan dukungan multiple waypoints dan roundtrip
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            route = calculatedRoute,
            currentLocation = currentLocation,
            pendingWaypoints = allSelectedPoints,
            onMapTap = { tappedPoint ->
                if (destinationPoint == null) {
                    // Tap pertama menentukan tujuan akhir rute
                    destinationPoint = tappedPoint
                    destinationName = String.format(Locale.US, "%.4f, %.4f", tappedPoint.latitude, tappedPoint.longitude)
                    computeMultiRoute(startPoint, intermediateWaypoints, tappedPoint, isRoundTrip)
                } else {
                    // Tap berikutnya menambahkan titik singgah (intermediate waypoint)
                    val newIntermediates = intermediateWaypoints + destinationPoint!!
                    intermediateWaypoints = newIntermediates
                    destinationPoint = tappedPoint
                    destinationName = String.format(Locale.US, "%.4f, %.4f", tappedPoint.latitude, tappedPoint.longitude)
                    computeMultiRoute(startPoint, newIntermediates, tappedPoint, isRoundTrip)
                    Toast.makeText(context, "Titik jalur ditambahkan", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // 2. Top Bar Search, Roundtrip Toggle, dan Chips Titik Jalur
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
                    placeholder = { Text("Cari lokasi atau tap peta...", fontSize = 13.sp, color = TextSecondary) },
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

            // Tombol cari alamat
            if (searchQuery.isNotEmpty()) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCalculatingRoute = true
                            val results = routingService.searchPlace(searchQuery)
                            if (results.isNotEmpty()) {
                                val topMatch = results.first()
                                if (destinationPoint == null) {
                                    destinationPoint = topMatch.point
                                    destinationName = topMatch.displayName.take(30) + "..."
                                    computeMultiRoute(startPoint, intermediateWaypoints, topMatch.point, isRoundTrip)
                                } else {
                                    val newIntermediates = intermediateWaypoints + destinationPoint!!
                                    intermediateWaypoints = newIntermediates
                                    destinationPoint = topMatch.point
                                    destinationName = topMatch.displayName.take(30) + "..."
                                    computeMultiRoute(startPoint, newIntermediates, topMatch.point, isRoundTrip)
                                }
                                searchQuery = ""
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
                    Text("Cari & Tambah Titik", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Toolbar Titik Jalur & Mode Roundtrip
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Roundtrip (Pulang-Pergi Loop)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isRoundTrip) BrightCyan else DarkCard)
                        .border(1.dp, if (isRoundTrip) BrightCyan else DarkCardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val toggled = !isRoundTrip
                            isRoundTrip = toggled
                            computeMultiRoute(startPoint, intermediateWaypoints, destinationPoint, toggled)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Roundtrip",
                            tint = if (isRoundTrip) Color.Black else BrightCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Roundtrip (PP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRoundTrip) Color.Black else TextPrimary
                        )
                    }
                }

                // Status Jumlah Titik
                val totalWaypointsCount = intermediateWaypoints.size + (if (destinationPoint != null) 1 else 0)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (totalWaypointsCount > 0) "$totalWaypointsCount Titik Rute" else "Tap Peta Buat Rute",
                        fontSize = 12.sp,
                        color = if (totalWaypointsCount > 0) NeonGreen else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Tombol Reset Rute
                if (destinationPoint != null || intermediateWaypoints.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AlertRed.copy(alpha = 0.2f))
                            .border(1.dp, AlertRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                destinationPoint = null
                                destinationName = ""
                                intermediateWaypoints = emptyList()
                                calculatedRoute = null
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reset",
                                tint = AlertRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                fontSize = 12.sp,
                                color = AlertRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chips Sekuens Titik Jalur (Bisa dihapus satu per satu)
            if (destinationPoint != null || intermediateWaypoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Titik Awal
                    item {
                        WaypointChip(label = "Awal: Lokasi", color = NeonGreen)
                    }

                    // Titik-titik Singgah Antara
                    itemsIndexed(intermediateWaypoints) { index, _ ->
                        WaypointRemovableChip(
                            label = "Titik #${index + 1}",
                            color = BrightCyan,
                            onRemove = {
                                val updated = intermediateWaypoints.filterIndexed { i, _ -> i != index }
                                intermediateWaypoints = updated
                                computeMultiRoute(startPoint, updated, destinationPoint, isRoundTrip)
                            }
                        )
                    }

                    // Titik Tujuan
                    if (destinationPoint != null) {
                        item {
                            WaypointRemovableChip(
                                label = if (isRoundTrip) "Putar Balik" else "Tujuan",
                                color = WarningAmber,
                                onRemove = {
                                    if (intermediateWaypoints.isNotEmpty()) {
                                        destinationPoint = intermediateWaypoints.last()
                                        intermediateWaypoints = intermediateWaypoints.dropLast(1)
                                    } else {
                                        destinationPoint = null
                                        destinationName = ""
                                    }
                                    computeMultiRoute(startPoint, intermediateWaypoints, destinationPoint, isRoundTrip)
                                }
                            )
                        }
                    }

                    // Indikator Kembali ke Titik Awal jika Roundtrip aktif
                    if (isRoundTrip && (destinationPoint != null || intermediateWaypoints.isNotEmpty())) {
                        item {
                            WaypointChip(label = "Finish: Awal", color = NeonGreen)
                        }
                    }
                }
            }
        }

        // 3. Loading Indicator Perhitungan Rute
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
                    Text("Menghitung rute multi-titik...", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 4. Panel Bawah: Ringkasan Rute, COT Selector, dan Tombol Mulai
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(DarkBackground)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(20.dp)
        ) {
            if (calculatedRoute != null) {
                val route = calculatedRoute!!
                val cotMillis = (targetCotHours * 3600000L).toLong()
                val targetSpeedKmh = PacingEngine.calculateInitialTargetSpeed(route.totalDistanceMeters, cotMillis)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (route.isRoundTrip) "Rute Sepeda Roundtrip (Loop)" else "Rute Multi-Titik Siap",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                            if (onSaveRoute != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        onSaveRoute(route)
                                        Toast.makeText(context, "Rute berhasil disimpan ke Rute Saya!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = "Simpan Rute",
                                        tint = BrightCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
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
                        text = "Tap di peta untuk menambah titik rute atau roundtrip",
                        fontSize = 13.sp,
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

            // Tombol Mulai Gowes (Real GPS & Simulasi)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        calculatedRoute?.let { route ->
                            val cotMillis = (targetCotHours * 3600000L).toLong()
                            onStartRide(route, cotMillis, false)
                        } ?: Toast.makeText(context, "Silakan buat rute terlebih dahulu", Toast.LENGTH_SHORT).show()
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

                // Tombol Simulasi untuk Pengujian Indoor
                Button(
                    onClick = {
                        calculatedRoute?.let { route ->
                            val cotMillis = (targetCotHours * 3600000L).toLong()
                            onStartRide(route, cotMillis, true)
                        } ?: Toast.makeText(context, "Silakan buat rute terlebih dahulu", Toast.LENGTH_SHORT).show()
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

@Composable
private fun WaypointChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardOverlay)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WaypointRemovableChip(label: String, color: Color, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardOverlay)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hapus Titik",
                tint = TextSecondary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
