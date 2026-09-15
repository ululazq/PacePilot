package com.pacepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pacepilot.app.data.model.RideMetrics
import com.pacepilot.app.ui.theme.AlertRed
import com.pacepilot.app.ui.theme.BrightCyan
import com.pacepilot.app.ui.theme.DarkBackground
import com.pacepilot.app.ui.theme.DarkCard
import com.pacepilot.app.ui.theme.NeonGreen
import com.pacepilot.app.ui.theme.TextPrimary
import com.pacepilot.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun RideSummaryDialog(
    metrics: RideMetrics,
    onDismiss: () -> Unit
) {
    val isCotAchieved = metrics.elapsedTimeMillis <= metrics.targetCotMillis
    val statusColor = if (isCotAchieved) NeonGreen else AlertRed
    val statusTitle = if (isCotAchieved) "🏆 TARGET COT TERCAPAI!" else "⏱️ MELEBIHI TARGET COT"

    val elapsedHours = metrics.elapsedTimeMillis / 3600000
    val elapsedMinutes = (metrics.elapsedTimeMillis % 3600000) / 60000
    val elapsedSeconds = (metrics.elapsedTimeMillis % 60000) / 1000

    val cotHours = metrics.targetCotMillis / 3600000
    val cotMinutes = (metrics.targetCotMillis % 3600000) / 60000

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = statusTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isCotAchieved) "Luar biasa! Disiplin pacing Anda tepat sasaran." else "Kerja bagus! Setiap kilometer adalah progres.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Grid Statistics
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryRow("Total Jarak", String.format(Locale.US, "%.2f km", metrics.distanceCoveredKm))
                    SummaryRow("Waktu Tempuh", String.format(Locale.US, "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds))
                    SummaryRow("Target COT", String.format(Locale.US, "%02d:%02d:00", cotHours, cotMinutes))
                    SummaryRow("Kecepatan Rata-rata", String.format(Locale.US, "%.1f km/j", metrics.averageSpeedKmh))
                    SummaryRow("Target Wajib Awal", String.format(Locale.US, "%.1f km/j", metrics.requiredSpeedKmh))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Selesai & Buat Rute Baru",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
