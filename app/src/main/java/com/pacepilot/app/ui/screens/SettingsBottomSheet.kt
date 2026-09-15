package com.pacepilot.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepilot.app.data.model.UserSettings
import com.pacepilot.app.ui.theme.BrightCyan
import com.pacepilot.app.ui.theme.DarkBackground
import com.pacepilot.app.ui.theme.DarkCard
import com.pacepilot.app.ui.theme.NeonGreen
import com.pacepilot.app.ui.theme.TextPrimary
import com.pacepilot.app.ui.theme.TextSecondary
import com.pacepilot.app.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    currentSettings: UserSettings,
    onSave: (UserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var fuelingMins by remember { mutableFloatStateOf(currentSettings.fuelingIntervalMinutes.toFloat()) }
    var restMins by remember { mutableFloatStateOf(currentSettings.restIntervalMinutes.toFloat()) }
    var speedTol by remember { mutableFloatStateOf(currentSettings.speedAlertThresholdKmh.toFloat()) }
    var vibeEnabled by remember { mutableStateOf(currentSettings.enableVibrationAlerts) }
    var audioEnabled by remember { mutableStateOf(currentSettings.enableAudioAlerts) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Pengaturan Gowes & Notifikasi",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Sesuaikan interval pengingat nutrisi, istirahat, dan sensitivitas kecepatan",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Fueling Interval Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Notifikasi Fueling / Nutrisi",
                        fontWeight = FontWeight.SemiBold,
                        color = WarningAmber
                    )
                    Text(
                        text = "Tiap ${fuelingMins.toInt()} Menit",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Slider(
                    value = fuelingMins,
                    onValueChange = { fuelingMins = it },
                    valueRange = 15f..60f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = WarningAmber,
                        activeTrackColor = WarningAmber
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rest Interval Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Notifikasi Istirahat (Rest Stop)",
                        fontWeight = FontWeight.SemiBold,
                        color = BrightCyan
                    )
                    Text(
                        text = "Tiap ${restMins.toInt()} Menit",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Slider(
                    value = restMins,
                    onValueChange = { restMins = it },
                    valueRange = 30f..120f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = BrightCyan,
                        activeTrackColor = BrightCyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vibration switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Getar (Vibration)",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Getar ponsel saat peringatan muncul",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = vibeEnabled,
                    onCheckedChange = { vibeEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonGreen,
                        checkedTrackColor = NeonGreen.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        currentSettings.copy(
                            fuelingIntervalMinutes = fuelingMins.toInt(),
                            restIntervalMinutes = restMins.toInt(),
                            speedAlertThresholdKmh = speedTol.toDouble(),
                            enableVibrationAlerts = vibeEnabled,
                            enableAudioAlerts = audioEnabled
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Simpan Pengaturan",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
