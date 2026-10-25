package com.thoughtflow.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Specular Glassmorphic Card.
 * Gives surfaces an Apple/Linear luxury finish with top rim lighting,
 * subtle glass depth, and ambient violet glow.
 */
@Composable
fun SpecularCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 10.dp,
    hasGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val specularBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            Color(0x338B5CF6),
            Color.White.copy(alpha = 0.04f)
        )
    )

    val surfaceGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1C1636),
            Color(0xFF120E25)
        )
    )

    val spotGlow = if (hasGlow) Color(0x408B5CF6) else Color(0x30000000)

    val baseModifier = modifier
        .shadow(elevation, shape, spotColor = spotGlow, ambientColor = Color(0x20000000))
        .clip(shape)
        .background(surfaceGradient)
        .border(1.dp, specularBorder, shape)

    val clickableModifier = if (onClick != null) {
        baseModifier.bounceClick(scaleDown = 0.97f, onClick = onClick)
    } else {
        baseModifier
    }

    Box(
        modifier = clickableModifier,
        content = content
    )
}
