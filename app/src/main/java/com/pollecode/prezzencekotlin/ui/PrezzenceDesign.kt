package com.pollecode.prezzencekotlin.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PrezzenceColors {
    val Background = Color(0xFF07070C)
    val Surface = Color(0xFF0F0F18)
    val Card = Color(0xFF161524)
    val CardElevated = Color(0xFF1E1C30)
    val Accent = Color(0xFF7C3AED)
    val AccentAlt = Color(0xFF06B6D4)
    val AccentGlow = Color(0xFF8B5CF6)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val Border = Color(0xFF232238)
    val BorderGlow = Color(0x337C3AED)
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 600f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(PrezzenceShape.Pill)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        PrezzenceColors.Accent.copy(alpha = if (enabled) 1f else 0.5f),
                        PrezzenceColors.AccentAlt.copy(alpha = if (enabled) 1f else 0.5f),
                    ),
                ),
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.10f),
                    ),
                ),
                PrezzenceShape.Pill,
            )
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = PrezzenceColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
fun PrezzenceSecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 600f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(PrezzenceShape.Pill)
            .background(PrezzenceColors.Card)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        PrezzenceColors.BorderGlow,
                        Color(0x3306B6D4),
                    ),
                ),
                PrezzenceShape.Pill,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = PrezzenceColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PrezzenceNavButton(
    icon: PrezzenceNavIcon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 600f),
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(PrezzenceColors.Card.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .a11yIconButton(if (icon == PrezzenceNavIcon.Close) "Close" else "Back")
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val strokeWidth = 3.0f
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
