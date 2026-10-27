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

import androidx.compose.material3.lightColorScheme

@Composable
fun ThoughtFlowTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val currentTheme = AppThemeManager.currentTheme
    val colorScheme = if (currentTheme.isLight) {
        lightColorScheme(
            primary = currentTheme.primary,
            onPrimary = PureWhite,
            primaryContainer = currentTheme.surface,
            onPrimaryContainer = currentTheme.primary,
            secondary = currentTheme.accent,
            onSecondary = PureWhite,
            background = currentTheme.background,
            onBackground = currentTheme.textPrimary,
            surface = currentTheme.cardSurface,
            onSurface = currentTheme.textPrimary,
            surfaceVariant = currentTheme.cardSurfaceElevated,
            onSurfaceVariant = currentTheme.textSecondary,
            outline = currentTheme.cardBorder
        )
    } else {
        darkColorScheme(
            primary = currentTheme.primary,
            onPrimary = PureWhite,
            primaryContainer = currentTheme.surface,
            onPrimaryContainer = currentTheme.accent,
            secondary = currentTheme.accent,
            onSecondary = PureWhite,
            background = currentTheme.background,
            onBackground = currentTheme.textPrimary,
            surface = currentTheme.cardSurface,
            onSurface = currentTheme.textPrimary,
            surfaceVariant = currentTheme.cardSurfaceElevated,
            onSurfaceVariant = currentTheme.textSecondary,
            outline = currentTheme.cardBorder
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = currentTheme.background.toArgb()
            window.navigationBarColor = currentTheme.background.toArgb()
            // In light themes, icons must be dark; in dark themes, icons must be light
            val isLightIcons = currentTheme.isLight
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightIcons
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLightIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content
    )
}
