package com.thoughtflow.ai.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.thoughtflow.ai.data.local.LocalCacheManager

data class AppThemeConfig(
    val name: String,
    val background: Color,
    val cardSurface: Color,
    val cardSurfaceElevated: Color,
    val cardBorder: Color,
    val primary: Color,
    val dark: Color,
    val accent: Color,
    val surface: Color,
    val pill: Color
)

object AppThemeManager {
    val THEMES = listOf(
        AppThemeConfig(
            name = "Obsidian Dark Studio",
            background = Color(0xFF090A0D),
            cardSurface = Color(0xFF13151A),
            cardSurfaceElevated = Color(0xFF1C1E24),
            cardBorder = Color(0xFF262830),
            primary = Color(0xFF3B82F6), // Royal Blue
            dark = Color(0xFF1D4ED8),
            accent = Color(0xFF60A5FA),
            surface = Color(0xFF181C26),
            pill = Color(0xFF22252E)
        ),
        AppThemeConfig(
            name = "Cyber Neon Cyan",
            background = Color(0xFF080C14),
            cardSurface = Color(0xFF0F172A),
            cardSurfaceElevated = Color(0xFF1E293B),
            cardBorder = Color(0xFF334155),
            primary = Color(0xFF06B6D4), // Electric Cyan
            dark = Color(0xFF0891B2),
            accent = Color(0xFF38BDF8),
            surface = Color(0xFF162032),
            pill = Color(0xFF1E293B)
        ),
        AppThemeConfig(
            name = "Emerald Zen",
            background = Color(0xFF080F0C),
            cardSurface = Color(0xFF0E1A14),
            cardSurfaceElevated = Color(0xFF16291F),
            cardBorder = Color(0xFF1F3D2E),
            primary = Color(0xFF10B981), // Emerald
            dark = Color(0xFF059669),
            accent = Color(0xFF34D399),
            surface = Color(0xFF14241B),
            pill = Color(0xFF1B2E24)
        ),
        AppThemeConfig(
            name = "Sunset Coral & Gold",
            background = Color(0xFF0F0A08),
            cardSurface = Color(0xFF1C130F),
            cardSurfaceElevated = Color(0xFF2B1C15),
            cardBorder = Color(0xFF3E281E),
            primary = Color(0xFFF97316), // Sunset Coral
            dark = Color(0xFFC2410C),
            accent = Color(0xFFFBBF24),
            surface = Color(0xFF241712),
            pill = Color(0xFF2D1E18)
        ),
        AppThemeConfig(
            name = "Midnight Amethyst",
            background = Color(0xFF0C0A14),
            cardSurface = Color(0xFF151224),
            cardSurfaceElevated = Color(0xFF201B36),
            cardBorder = Color(0xFF2E274D),
            primary = Color(0xFF8B5CF6), // Amethyst Violet
            dark = Color(0xFF6D28D9),
            accent = Color(0xFFA78BFA),
            surface = Color(0xFF1C1730),
            pill = Color(0xFF272140)
        )
    )

    var currentThemeIndex by mutableStateOf(0)

    val currentTheme: AppThemeConfig
        get() = THEMES[currentThemeIndex]

    fun setTheme(name: String) {
        val idx = THEMES.indexOfFirst {
            it.name.equals(name, ignoreCase = true) ||
            name.contains(it.name.substringBefore(" "), ignoreCase = true)
        }
        if (idx != -1) {
            currentThemeIndex = idx
            LocalCacheManager.saveAppTheme(THEMES[idx].name)
        }
    }

    fun init() {
        val saved = LocalCacheManager.getSavedTheme()
        val idx = THEMES.indexOfFirst {
            it.name.equals(saved, ignoreCase = true) ||
            saved.contains(it.name.substringBefore(" "), ignoreCase = true)
        }
        if (idx != -1) {
            currentThemeIndex = idx
        }
    }
}

// Dynamic Theme Color bindings (Re-evaluated on real-time theme change across all composables)
inline val DeepBackground: Color get() = AppThemeManager.currentTheme.background
inline val CardSurface: Color get() = AppThemeManager.currentTheme.cardSurface
inline val CardSurfaceElevated: Color get() = AppThemeManager.currentTheme.cardSurfaceElevated
inline val CardBorder: Color get() = AppThemeManager.currentTheme.cardBorder
val CardBorderFocused = Color(0xFF383C48)

inline val PurplePrimary: Color get() = AppThemeManager.currentTheme.primary
inline val PurpleDark: Color get() = AppThemeManager.currentTheme.dark
inline val PurpleDeep: Color get() = AppThemeManager.currentTheme.dark
inline val PurpleAccent: Color get() = AppThemeManager.currentTheme.accent
val PurpleGlow = Color(0x203B82F6)
inline val PurpleSurface: Color get() = AppThemeManager.currentTheme.surface

// Status & Accent Colors
val StatusGreen = Color(0xFF10B981)
val StatusGreenSoft = Color(0x2010B981)
val StatusAmber = Color(0xFFF59E0B)
val StatusAmberSoft = Color(0x20F59E0B)
val StatusOrange = StatusAmber
val StatusBlue = Color(0xFF3B82F6)
val StatusBlueSoft = Color(0x203B82F6)
val StatusRed = Color(0xFFEF4444)
val StatusRedSoft = Color(0x20EF4444)

// Creative Accents (Refined, professional)
val CyberCyan = Color(0xFF0EA5E9)
val ElectricBlue = Color(0xFF3B82F6)
val SunsetGold = Color(0xFFF59E0B)
val RadiantCoral = Color(0xFFF43F5E)
val EmeraldMint = Color(0xFF10B981)
val RoyalIndigo = Color(0xFF4F46E5)
val WarmAmethyst = Color(0xFF8B5CF6)

// Dynamic Gradient Duos
inline val GradientViolet: List<Color> get() = listOf(PurplePrimary, PurpleDark)
val GradientCyan = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
val GradientSunset = listOf(Color(0xFFEA580C), Color(0xFFC2410C))
val GradientEmerald = listOf(Color(0xFF059669), Color(0xFF047857))
val GradientGold = listOf(Color(0xFFD97706), Color(0xFFB45309))
val GradientRose = listOf(Color(0xFFE11D48), Color(0xFFBE123C))
val GradientIndigo = listOf(Color(0xFF4F46E5), Color(0xFF4338CA))

// Neutral Surfaces & Typography
val TextWhite = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val DividerDark = Color(0xFF22242B)

// Backward compatibility aliases
val MintGreenPrimary = StatusGreen
val MintGreenSoft = StatusGreenSoft
val MintGreenDark = Color(0xFF047857)
val MintGreenLight = Color(0xFF34D399)
inline val CyanFab: Color get() = PurplePrimary
inline val DarkPurplePill: Color get() = CardSurfaceElevated
inline val BackgroundCanvas: Color get() = DeepBackground
inline val SurfaceCard: Color get() = CardSurface
inline val SurfaceCardAlt: Color get() = CardSurfaceElevated
inline val BorderSubtle: Color get() = CardBorder
val TextPrimary = TextWhite
val StatusSuccess = StatusGreen
val StatusWarning = StatusAmber
val StatusError = StatusRed
