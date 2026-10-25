package com.thoughtflow.ai.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Studio Obsidian Palette (Clean, Neutral, Modern - 0% muddy purple)
val DeepBackground = Color(0xFF090A0D)
val CardSurface = Color(0xFF13151A)
val CardSurfaceElevated = Color(0xFF1C1E24)
val CardBorder = Color(0xFF262830)
val CardBorderFocused = Color(0xFF383C48)

// Clean Studio Sapphire / Ice Brand Accents (Replacing harsh purple)
val PurplePrimary = Color(0xFF3B82F6) // Royal Studio Blue
val PurpleDark = Color(0xFF1D4ED8)
val PurpleDeep = Color(0xFF1E40AF)
val PurpleAccent = Color(0xFF60A5FA) // Ice Blue
val PurpleGlow = Color(0x203B82F6)
val PurpleSurface = Color(0xFF181C26)

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

// Gradient Duos (Sleek, subtle studio gradients)
val GradientViolet = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
val GradientCyan = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
val GradientSunset = listOf(Color(0xFFEA580C), Color(0xFFC2410C))
val GradientEmerald = listOf(Color(0xFF059669), Color(0xFF047857))
val GradientGold = listOf(Color(0xFFD97706), Color(0xFFB45309))
val GradientRose = listOf(Color(0xFFE11D48), Color(0xFFBE123C))
val GradientIndigo = listOf(Color(0xFF4F46E5), Color(0xFF4338CA))

// Neutral Surfaces & Typography (Clean, high contrast, non-purple)
val TextWhite = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val DividerDark = Color(0xFF22242B)

// Backward compatibility aliases
val MintGreenPrimary = StatusGreen
val MintGreenSoft = StatusGreenSoft
val MintGreenDark = Color(0xFF047857)
val MintGreenLight = Color(0xFF34D399)
val CyanFab = PurplePrimary
val DarkPurplePill = CardSurfaceElevated
val BackgroundCanvas = DeepBackground
val SurfaceCard = CardSurface
val SurfaceCardAlt = CardSurfaceElevated
val BorderSubtle = CardBorder
val TextPrimary = TextWhite
val StatusSuccess = StatusGreen
val StatusWarning = StatusAmber
val StatusError = StatusRed
