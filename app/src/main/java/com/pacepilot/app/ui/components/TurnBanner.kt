package com.pacepilot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepilot.app.data.model.ManeuverType
import com.pacepilot.app.data.model.RouteStep
import com.pacepilot.app.ui.theme.BrightCyan
import com.pacepilot.app.ui.theme.DarkBackground
import com.pacepilot.app.ui.theme.DarkCard
import com.pacepilot.app.ui.theme.DarkCardBorder
import com.pacepilot.app.ui.theme.NeonGreen
import com.pacepilot.app.ui.theme.TextPrimary
import com.pacepilot.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TurnBanner(
    step: RouteStep?,
    distanceToManeuverMeters: Double,
    modifier: Modifier = Modifier
) {
    if (step == null) return

    val icon: ImageVector = when (step.maneuver) {
        ManeuverType.DEPART -> Icons.Default.Navigation
        ManeuverType.TURN_LEFT, ManeuverType.SHARP_LEFT, ManeuverType.SLIGHT_LEFT -> Icons.Default.ArrowBack
        ManeuverType.TURN_RIGHT, ManeuverType.SHARP_RIGHT, ManeuverType.SLIGHT_RIGHT -> Icons.Default.ArrowForward
        ManeuverType.U_TURN -> Icons.Default.Undo
        ManeuverType.ROUNDABOUT -> Icons.Default.Refresh
        ManeuverType.ARRIVE -> Icons.Default.CheckCircle
        else -> Icons.Default.ArrowUpward
    }

    val distanceText = if (distanceToManeuverMeters >= 1000.0) {
        String.format(Locale.US, "%.1f km", distanceToManeuverMeters / 1000.0)
    } else {
        String.format(Locale.US, "%d m", distanceToManeuverMeters.toInt())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkBackground.copy(alpha = 0.95f))
            .border(1.5.dp, NeonGreen.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Maneuver Icon Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NeonGreen)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Distance & Next Instruction
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = distanceText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen
                )
                Text(
                    text = step.instruction,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2
                )
            }
        }
    }
}
