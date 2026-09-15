package com.pacepilot.app.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepilot.app.data.model.RideRecord
import com.pacepilot.app.data.repository.ActivityRepository
import com.pacepilot.app.ui.theme.AlertRed
import com.pacepilot.app.ui.theme.BrightCyan
import com.pacepilot.app.ui.theme.DarkBackground
import com.pacepilot.app.ui.theme.DarkCard
import com.pacepilot.app.ui.theme.DarkCardBorder
import com.pacepilot.app.ui.theme.NeonGreen
import com.pacepilot.app.ui.theme.TextPrimary
import com.pacepilot.app.ui.theme.TextSecondary
import com.pacepilot.app.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityHistoryScreen(
    repository: ActivityRepository
) {
    val context = LocalContext.current
    var activities by remember { mutableStateOf(repository.getAllActivities()) }
    val summary = remember(activities) { repository.getSummaryStats() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Riwayat & Aktivitas",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Statistik performa gowes dan pencapaian target COT",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Top Summary Dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryStatBox(
                label = "TOTAL JARAK",
                value = String.format(Locale.US, "%.1f km", summary.totalDistanceKm),
                color = NeonGreen,
                modifier = Modifier.weight(1f)
            )
            SummaryStatBox(
                label = "TOTAL GOWES",
                value = "${summary.totalRides}x",
                color = BrightCyan,
                modifier = Modifier.weight(1f)
            )
            SummaryStatBox(
                label = "TOTAL KALORI",
                value = "${summary.totalCaloriesKcal} kcal",
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkCard)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum Ada Catatan Gowes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Selesaikan sesi gowes di tab Navigasi untuk mencatat riwayat dan kalori otomatis.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activities, key = { it.id }) { record ->
                    ActivityCard(
                        record = record,
                        onDelete = {
                            repository.deleteActivity(record.id)
                            activities = repository.getAllActivities()
                            Toast.makeText(context, "Riwayat dihapus", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun ActivityCard(
    record: RideRecord,
    onDelete: () -> Unit
) {
    val dateFormatted = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date(record.dateMillis))
    val hours = record.durationMillis / 3600000
    val minutes = (record.durationMillis % 3600000) / 60000

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = dateFormatted,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // COT Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (record.isCotAchieved) NeonGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (record.isCotAchieved) "Target COT Sukses" else "Melebihi COT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.isCotAchieved) NeonGreen else AlertRed
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = AlertRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("JARAK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(
                        text = String.format(Locale.US, "%.2f km", record.totalDistanceKm),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("DURASI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(
                        text = "${hours}j ${minutes}m",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("AVG SPEED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(
                        text = String.format(Locale.US, "%.1f km/j", record.avgSpeedKmh),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightCyan
                    )
                }

                Column {
                    Text("KALORI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(
                        text = "${record.caloriesKcal} kcal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                }
            }
        }
    }
}
