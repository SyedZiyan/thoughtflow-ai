package com.thoughtflow.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Procedurally generates the Sunset Mountain Lake scenery seen on
 * Splash / Onboarding (Screen 1) and Dashboard "Today's Thought" (Screen 3).
 */
@Composable
fun ScenicLandscape(
    modifier: Modifier = Modifier,
    dimOverlay: Float = 0.35f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Sky Gradient (Deep Violet -> Purple -> Sunset Peach / Gold)
        val skyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF160E33),
                Color(0xFF3B1E54),
                Color(0xFF78286E),
                Color(0xFFC04848),
                Color(0xFFF07241),
                Color(0xFFFF9E64)
            ),
            startY = 0f,
            endY = h * 0.62f
        )
        drawRect(brush = skyBrush)

        // 2. Rising / Setting Glowing Sun
        val sunCenter = Offset(w * 0.62f, h * 0.46f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF7ED), Color(0xFFFFD166), Color(0x66FF8A5B), Color.Transparent),
                center = sunCenter,
                radius = w * 0.35f
            ),
            center = sunCenter,
            radius = w * 0.35f
        )

        // 3. Distant Mountains (Soft Violet Silhouette)
        val distantPath = Path().apply {
            moveTo(0f, h * 0.58f)
            lineTo(w * 0.15f, h * 0.52f)
            lineTo(w * 0.35f, h * 0.44f)
            lineTo(w * 0.52f, h * 0.49f)
            lineTo(w * 0.70f, h * 0.41f)
            lineTo(w * 0.88f, h * 0.47f)
            lineTo(w, h * 0.54f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = distantPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x992B1547), Color(0xCC1A0C2E)),
                startY = h * 0.41f,
                endY = h * 0.62f
            )
        )

        // 4. Midground Mountain Ridges
        val midPath = Path().apply {
            moveTo(0f, h * 0.64f)
            lineTo(w * 0.22f, h * 0.53f)
            lineTo(w * 0.48f, h * 0.60f)
            lineTo(w * 0.78f, h * 0.50f)
            lineTo(w, h * 0.58f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = midPath,
            color = Color(0xDD160B29)
        )

        // 5. Water Surface (Reflections of Sunset)
        val waterBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xEE1E0E38),
                Color(0xCC3D164B),
                Color(0xEE140A26),
                Color(0xFF090615)
            ),
            startY = h * 0.62f,
            endY = h
        )
        drawRect(
            brush = waterBrush,
            topLeft = Offset(0f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.38f)
        )

        // Sun shimmer on water
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FFB074), Color(0x22FF7A59), Color.Transparent),
                center = Offset(w * 0.62f, h * 0.66f),
                radius = w * 0.25f
            ),
            topLeft = Offset(w * 0.42f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.12f)
        )

        // 6. Foreground Cliff Silhouette
        val cliffPath = Path().apply {
            moveTo(0f, h * 0.72f)
            cubicTo(w * 0.12f, h * 0.70f, w * 0.24f, h * 0.74f, w * 0.32f, h * 0.82f)
            lineTo(w * 0.28f, h)
            lineTo(0f, h)
            close()
        }
        drawPath(cliffPath, Color(0xFF0B0616))

        // 7. Dark Dimming Overlay for Text Readability
        if (dimOverlay > 0f) {
            drawRect(Color(0xFF090713).copy(alpha = dimOverlay))
        }
    }
}
