package com.thoughtflow.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thoughtflow.ai.ui.theme.PurpleAccent
import com.thoughtflow.ai.ui.theme.PurpleDark
import com.thoughtflow.ai.ui.theme.PurplePrimary

/**
 * Reusable Stylized Glowing Lotus Logo seen across Splash, Login, and Dashboard.
 */
@Composable
fun LotusLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val purpleGradient = Brush.verticalGradient(
            colors = listOf(PurpleAccent, PurplePrimary, PurpleDark)
        )

        // 1. Center Petal (Tallest)
        val centerPetal = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            cubicTo(w * 0.62f, h * 0.40f, w * 0.62f, h * 0.70f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.38f, h * 0.70f, w * 0.38f, h * 0.40f, w * 0.5f, h * 0.12f)
            close()
        }
        drawPath(centerPetal, purpleGradient)

        // 2. Left Inner Petal
        val leftInner = Path().apply {
            moveTo(w * 0.32f, h * 0.28f)
            cubicTo(w * 0.46f, h * 0.46f, w * 0.46f, h * 0.76f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.34f, h * 0.76f, w * 0.22f, h * 0.56f, w * 0.32f, h * 0.28f)
            close()
        }
        drawPath(leftInner, purpleGradient)

        // 3. Right Inner Petal
        val rightInner = Path().apply {
            moveTo(w * 0.68f, h * 0.28f)
            cubicTo(w * 0.78f, h * 0.56f, w * 0.66f, h * 0.76f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.54f, h * 0.76f, w * 0.54f, h * 0.46f, w * 0.68f, h * 0.28f)
            close()
        }
        drawPath(rightInner, purpleGradient)

        // 4. Far Left Outer Petal
        val farLeft = Path().apply {
            moveTo(w * 0.16f, h * 0.48f)
            cubicTo(w * 0.32f, h * 0.60f, w * 0.38f, h * 0.80f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.30f, h * 0.86f, w * 0.10f, h * 0.72f, w * 0.16f, h * 0.48f)
            close()
        }
        drawPath(farLeft, purpleGradient)

        // 5. Far Right Outer Petal
        val farRight = Path().apply {
            moveTo(w * 0.84f, h * 0.48f)
            cubicTo(w * 0.90f, h * 0.72f, w * 0.70f, h * 0.86f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.62f, h * 0.80f, w * 0.68f, h * 0.60f, w * 0.84f, h * 0.48f)
            close()
        }
        drawPath(farRight, purpleGradient)

        // Soft center base glow
        drawCircle(
            color = Color(0x66A855F7),
            radius = w * 0.12f,
            center = Offset(w * 0.5f, h * 0.86f)
        )
    }
}
