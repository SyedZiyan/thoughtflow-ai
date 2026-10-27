package com.thoughtflow.ai.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.thoughtflow.ai.data.local.LocalCacheManager

data class AppThemeConfig(
    val name: String,
    val isLight: Boolean = false,
    val background: Color,
    val cardSurface: Color,
    val cardSurfaceElevated: Color,
    val cardBorder: Color,
    val primary: Color,
    val dark: Color,
    val accent: Color,
    val surface: Color,
    val pill: Color,
    val textPrimary: Color = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC),
    val textSecondary: Color = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
    val textMuted: Color = if (isLight) Color(0xFF64748B) else Color(0xFF64748B)
)

object AppThemeManager {
    val THEMES = listOf(
        // 0. Default Theme: Pure Studio Porcelain (Crisp Executive White Theme)
        AppThemeConfig(
            name = "Pure Studio Porcelain",
            isLight = true,
            background = Color(0xFFF8FAFC), // Pure subtle porcelain white
            cardSurface = Color(0xFFFFFFFF), // Crisp pure white
            cardSurfaceElevated = Color(0xFFF1F5F9), // Light elevated surface
            cardBorder = Color(0xFFE2E8F0), // Subtle hairline border
            primary = Color(0xFF2563EB), // Royal Studio Blue
            dark = Color(0xFF1D4ED8),
            accent = Color(0xFF3B82F6),
            surface = Color(0xFFEFF6FF),
            pill = Color(0xFFE2E8F0),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            textMuted = Color(0xFF64748B)
        ),
        // 1. Obsidian Dark Studio
        AppThemeConfig(
            name = "Obsidian Dark Studio",
            isLight = false,
            background = Color(0xFF090A0D),
            cardSurface = Color(0xFF13151A),
            cardSurfaceElevated = Color(0xFF1C1E24),
            cardBorder = Color(0xFF262830),
            primary = Color(0xFF3B82F6), // Royal Blue
            dark = Color(0xFF1D4ED8),
            accent = Color(0xFF60A5FA),
            surface = Color(0xFF181C26),
            pill = Color(0xFF22252E),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF64748B)
        ),
        // 2. Cyber Neon Cyan
        AppThemeConfig(
            name = "Cyber Neon Cyan",
            isLight = false,
            background = Color(0xFF080C14),
            cardSurface = Color(0xFF0F172A),
            cardSurfaceElevated = Color(0xFF1E293B),
            cardBorder = Color(0xFF334155),
            primary = Color(0xFF06B6D4), // Electric Cyan
            dark = Color(0xFF0891B2),
            accent = Color(0xFF38BDF8),
            surface = Color(0xFF162032),
            pill = Color(0xFF1E293B),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF64748B)
        ),
        // 3. Emerald Zen
        AppThemeConfig(
            name = "Emerald Zen",
            isLight = false,
            background = Color(0xFF080F0C),
            cardSurface = Color(0xFF0E1A14),
            cardSurfaceElevated = Color(0xFF16291F),
            cardBorder = Color(0xFF1F3D2E),
            primary = Color(0xFF10B981), // Emerald
            dark = Color(0xFF059669),
            accent = Color(0xFF34D399),
            surface = Color(0xFF14241B),
            pill = Color(0xFF1B2E24),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF64748B)
        ),
        // 4. Sunset Coral & Gold
        AppThemeConfig(
            name = "Sunset Coral & Gold",
            isLight = false,
            background = Color(0xFF0F0A08),
            cardSurface = Color(0xFF1C130F),
            cardSurfaceElevated = Color(0xFF2B1C15),
            cardBorder = Color(0xFF3E281E),
            primary = Color(0xFFF97316), // Sunset Coral
            dark = Color(0xFFC2410C),
            accent = Color(0xFFFBBF24),
            surface = Color(0xFF241712),
            pill = Color(0xFF2D1E18),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF64748B)
        ),
        // 5. Midnight Amethyst
        AppThemeConfig(
            name = "Midnight Amethyst",
            isLight = false,
            background = Color(0xFF0C0A14),
            cardSurface = Color(0xFF151224),
            cardSurfaceElevated = Color(0xFF201B36),
            cardBorder = Color(0xFF2E274D),
            primary = Color(0xFF8B5CF6), // Amethyst Violet
            dark = Color(0xFF6D28D9),
            accent = Color(0xFFA78BFA),
            surface = Color(0xFF1C1730),
            pill = Color(0xFF272140),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF64748B)
        ),
        // 6. Rose Gold Champagne (Warm Luxury Light Theme)
        AppThemeConfig(
            name = "Rose Gold Champagne",
            isLight = true,
            background = Color(0xFFFAF7F5),
            cardSurface = Color(0xFFFFFFFF),
            cardSurfaceElevated = Color(0xFFF5EBE6),
            cardBorder = Color(0xFFE8DAD4),
            primary = Color(0xFFE11D48), // Rose crimson
            dark = Color(0xFFBE123C),
            accent = Color(0xFFF43F5E),
            surface = Color(0xFFFFF1F2),
            pill = Color(0xFFF5EBE6),
            textPrimary = Color(0xFF1C1315),
            textSecondary = Color(0xFF5C474B),
            textMuted = Color(0xFF886E73)
        ),
        // 7. Nordic Titanium Slate (Minimalist Cool Gray Light Theme)
        AppThemeConfig(
            name = "Nordic Titanium Slate",
            isLight = true,
            background = Color(0xFFF3F4F6),
            cardSurface = Color(0xFFFFFFFF),
            cardSurfaceElevated = Color(0xFFE5E7EB),
            cardBorder = Color(0xFFD1D5DB),
            primary = Color(0xFF0D9488), // Nordic Teal
            dark = Color(0xFF0F766E),
            accent = Color(0xFF14B8A6),
            surface = Color(0xFFCCFBF1),
            pill = Color(0xFFE5E7EB),
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xFF4B5563),
            textMuted = Color(0xFF6B7280)
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
        currentThemeIndex = if (idx != -1) idx else 0
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

// Dynamic Specular Glass Border
inline val SpecularBorder: Brush
    get() = if (AppThemeManager.currentTheme.isLight) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    }

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

// Dynamic Neutral Surfaces & Typography
inline val TextWhite: Color get() = AppThemeManager.currentTheme.textPrimary
inline val TextPrimary: Color get() = AppThemeManager.currentTheme.textPrimary
inline val TextSecondary: Color get() = AppThemeManager.currentTheme.textSecondary
inline val TextMuted: Color get() = AppThemeManager.currentTheme.textMuted
val DividerDark = Color(0xFF22242B)

// Fixed contrast values for components with permanent dark/light fills
val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)

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
val StatusSuccess = StatusGreen
val StatusWarning = StatusAmber
val StatusError = StatusRed
