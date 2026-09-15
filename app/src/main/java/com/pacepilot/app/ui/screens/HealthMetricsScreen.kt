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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun HealthMetricsScreen(
    repository: ActivityRepository
) {
    val scrollState = rememberScrollState()
    val stats = remember { repository.getSummaryStats() }
    var riderWeight by remember { mutableFloatStateOf(68f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Kesehatan & Kebugaran",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Analisis kalori, hidrasi, pemulihan tubuh, dan zona kayuhan",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Big Cards: Kalori Terbakar & Status Pemulihan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Kalori Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkCard)
                    .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("KALORI AKTIF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stats.totalCaloriesKcal}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text("Kcal terbakar", fontSize = 11.sp, color = WarningAmber)
                }
            }

            // Pemulihan Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkCard)
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PEMULIHAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Optimal",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen
                    )
                    Text("Kondisi tubuh bugar", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hidrasi & Keringat Advice
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panduan Hidrasi & Elektrolit", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Konsumsi rata-rata 500-750 ml cairan per jam saat bersepeda di iklim tropis. Tambahkan elektrolit jika sesi gowes melebihi 90 menit untuk mencegah kram otot.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Zona Latihan Jantung (Heart Rate Zones)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Zona Target Intensitas Gowes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                IntensityRow("Zona 1: Pemulihan Aktif", "< 60% HR Max", "Kayuhan ringan melancarkan asam laktat", NeonGreen)
                IntensityRow("Zona 2: Daya Tahan Dasar", "60-70% HR Max", "Membakar lemak efisien & menjaga stamina COT", BrightCyan)
                IntensityRow("Zona 3: Tempo Gowes", "70-80% HR Max", "Ritme cepat stabil jarak menengah", WarningAmber)
                IntensityRow("Zona 4: Ambang Laktat", "80-90% HR Max", "Maksimal untuk tanjakan dan sprint", AlertRed)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profil Berat Badan untuk Akurasi Kalori
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Berat Badan Pesepeda", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${riderWeight.toInt()} kg", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Digunakan untuk kalkulasi METs dan pembakaran kalori akurat", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = riderWeight,
                    onValueChange = { riderWeight = it },
                    valueRange = 45f..120f,
                    steps = 74,
                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                )
            }
        }
    }
}

@Composable
private fun IntensityRow(title: String, hr: String, desc: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = hr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            Text(text = desc, fontSize = 11.sp, color = TextSecondary)
        }
    }
}
