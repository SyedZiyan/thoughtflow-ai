package com.thoughtflow.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtflow.ai.ui.components.CinematicBackdrop
import com.thoughtflow.ai.ui.components.LotusLogo
import com.thoughtflow.ai.ui.components.bounceClick
import com.thoughtflow.ai.ui.theme.*

/**
 * Screen 1: Splash / Onboarding (Elite Polish Edition)
 * Living cinematic sunset mountain lake with Ken-Burns slow motion,
 * glowing purple Lotus logo, and tactile spring buttons.
 */
@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        // Living Cinematic Backdrop with gentle ambient motion
        CinematicBackdrop(
            enableKenBurns = true,
            dimAlpha = 0.45f,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient vignette from bottom to ensure text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x60090713),
                            DeepBackground.copy(alpha = 0.98f)
                        ),
                        startY = 420f
                    )
                )
        )

        // Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Glowing Lotus Flower Icon
            LotusLogo(size = 68.dp)

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Title
            Text(
                text = "ThoughtFlow AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Turn Daily Thoughts\ninto Inspiring Videos",
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Features dot row
            Text(
                text = "Generate  •  Create  •  Publish  •  Grow",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = PurpleAccent.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // "Get Started" Tactile Purple Gradient Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = Color(0x667C3AED))
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PurplePrimary, PurpleDark)
                        )
                    )
                    .bounceClick(scaleDown = 0.96f, onClick = onGetStarted),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // "Already have an account? Sign In" with bounce
            val annotatedText = buildAnnotatedString {
                append("Already have an account? ")
                withStyle(style = SpanStyle(color = PurplePrimary, fontWeight = FontWeight.SemiBold)) {
                    append("Sign In")
                }
            }

            Text(
                text = annotatedText,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.bounceClick(scaleDown = 0.96f, onClick = onSignIn)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
