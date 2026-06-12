package com.pollecode.prezzencekotlin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PrezzenceColors {
    val Background = Color(0xFF0A0A0F)
    val Surface = Color(0xFF12121A)
    val Card = Color(0xFF1C1C2E)
    val Accent = Color(0xFF6C63FF)
    val AccentAlt = Color(0xFF8E7DFF)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFF8A8A9A)
    val Border = Color(0xFF2A2A3E)
    val Success = Color(0xFF00D68F)
    val Danger = Color(0xFFFF4757)
}

object PrezzenceShape {
    val Field = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(999.dp)
    val Card = RoundedCornerShape(24.dp)
}

enum class PrezzenceNavIcon { Back, Close }

@Composable
fun PrezzencePrimaryButton(
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(PrezzenceShape.Pill)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        PrezzenceColors.Accent.copy(alpha = if (enabled) 1f else 0.65f),
                        PrezzenceColors.AccentAlt.copy(alpha = if (enabled) 1f else 0.65f),
                    ),
                ),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = PrezzenceColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrezzenceSecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(PrezzenceShape.Pill)
            .background(PrezzenceColors.Card)
            .border(1.dp, Color(0x1AFFFFFF), PrezzenceShape.Pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = PrezzenceColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrezzenceNavButton(
    icon: PrezzenceNavIcon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(PrezzenceColors.Card.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val strokeWidth = 3.2f
            if (icon == PrezzenceNavIcon.Close) {
                drawLine(PrezzenceColors.TextPrimary, Offset(size.width * 0.28f, size.height * 0.28f), Offset(size.width * 0.72f, size.height * 0.72f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(PrezzenceColors.TextPrimary, Offset(size.width * 0.72f, size.height * 0.28f), Offset(size.width * 0.28f, size.height * 0.72f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            } else {
                drawLine(PrezzenceColors.TextPrimary, Offset(size.width * 0.62f, size.height * 0.20f), Offset(size.width * 0.34f, size.height * 0.50f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(PrezzenceColors.TextPrimary, Offset(size.width * 0.34f, size.height * 0.50f), Offset(size.width * 0.62f, size.height * 0.80f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
        }
    }
}
