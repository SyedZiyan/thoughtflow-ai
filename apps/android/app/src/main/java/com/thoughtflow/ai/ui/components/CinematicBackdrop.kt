package com.thoughtflow.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thoughtflow.ai.ui.theme.DeepBackground

/**
 * Living Cinematic Backdrop with gentle Ken-Burns ambient slow motion.
 * Loads high-definition imagery via Coil with offline procedural fallback.
 */
@Composable
fun CinematicBackdrop(
    imageUrl: String? = null,
    enableKenBurns: Boolean = true,
    dimAlpha: Float = 0.35f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Subtle Ken Burns slow zoom (1.0x to 1.06x over 12s)
    val scale = if (enableKenBurns) {
        val transition = rememberInfiniteTransition(label = "ken_burns")
        val animatedScale by transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        animatedScale
    } else {
        1.0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(600)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        } else {
            // High-detail procedural scenery fallback
            ScenicLandscape(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                dimOverlay = 0.15f
            )
        }

        // Luxury dark vignette overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x55090713),
                            DeepBackground.copy(alpha = dimAlpha.coerceAtLeast(0.4f))
                        )
                    )
                )
        )
    }
}
