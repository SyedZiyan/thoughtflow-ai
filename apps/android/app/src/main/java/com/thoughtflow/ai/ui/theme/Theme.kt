package com.thoughtflow.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun ThoughtFlowTheme(
    darkTheme: Boolean = true, // Default to Dark luxury theme per UI design plan
    content: @Composable () -> Unit
) {
    val currentTheme = AppThemeManager.currentTheme
    val colorScheme = darkColorScheme(
        primary = currentTheme.primary,
        onPrimary = TextWhite,
        primaryContainer = currentTheme.surface,
        onPrimaryContainer = currentTheme.accent,
        secondary = currentTheme.accent,
        onSecondary = TextWhite,
        background = currentTheme.background,
        onBackground = TextWhite,
        surface = currentTheme.cardSurface,
        onSurface = TextWhite,
        surfaceVariant = currentTheme.cardSurfaceElevated,
        onSurfaceVariant = TextSecondary,
        outline = currentTheme.cardBorder
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = currentTheme.background.toArgb()
            window.navigationBarColor = currentTheme.background.toArgb()
            // Set status bar icons to light (white icons)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content
    )
}
