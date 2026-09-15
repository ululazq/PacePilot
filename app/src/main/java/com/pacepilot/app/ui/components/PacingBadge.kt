package com.pacepilot.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepilot.app.data.model.PacingStatus
import com.pacepilot.app.ui.theme.AlertRed
import com.pacepilot.app.ui.theme.BrightCyan
import com.pacepilot.app.ui.theme.NeonGreen
import com.pacepilot.app.ui.theme.TextPrimary
import com.pacepilot.app.ui.theme.WarningAmber

@Composable
fun PacingBadge(
    status: PacingStatus,
    advice: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, icon) = when (status) {
        PacingStatus.ON_PACE -> Triple(
            NeonGreen.copy(alpha = 0.15f),
            NeonGreen,
            Icons.Default.CheckCircle
        )
        PacingStatus.SPEED_UP -> Triple(
            WarningAmber.copy(alpha = 0.15f),
            WarningAmber,
            Icons.Default.ArrowUpward
        )
        PacingStatus.SLOW_DOWN -> Triple(
            BrightCyan.copy(alpha = 0.15f),
            BrightCyan,
            Icons.Default.ArrowDownward
        )
        PacingStatus.BEHIND_COT -> Triple(
            AlertRed.copy(alpha = 0.18f),
            AlertRed,
            Icons.Default.Warning
        )
        PacingStatus.AHEAD_OF_COT -> Triple(
            NeonGreen.copy(alpha = 0.15f),
            NeonGreen,
            Icons.Default.CheckCircle
        )
    }

    val animatedBorderColor by animateColorAsState(targetValue = borderColor, label = "pacingBorder")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, animatedBorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedBorderColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = advice,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
        }
    }
}
