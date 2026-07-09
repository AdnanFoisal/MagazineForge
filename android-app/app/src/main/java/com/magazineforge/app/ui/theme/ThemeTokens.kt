package com.magazineforge.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DecorativeMotif {
    BLOBS,
    NEBULA,
    BOTANICAL,
    HALFTONE,
    NONE
}

data class ThemeTokens(
    val id: String,
    val displayName: String,
    val description: String,
    val welcomeBackground: Color,
    val screenBackground: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val ctaFill: Color,
    val ctaText: Color,
    val iconMarkTint1: Color,
    val iconMarkTint2: Color,
    val cornerRadius: Dp,
    val wordmarkFontFamily: FontFamily,
    val wordmarkAllCaps: Boolean,
    val decorativeMotif: DecorativeMotif,
    val isDark: Boolean,
    val editorBackground: Color,
    val editorText: Color,
    val editorTextSecondary: Color,
    val editorBorder: Color
)

val SunsetEditorial = ThemeTokens(
    id = "sunset_editorial",
    displayName = "Sunset Editorial",
    description = "Warm, elegant, and light-filled. For creators who love natural tones and classic typography.",
    welcomeBackground = Color(0xFFF5EDE0),
    screenBackground = Color(0xFFF5EDE0),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1E1B3A),
    textSecondary = Color(0xFF8A8078),
    primaryAccent = Color(0xFFE8703D),
    secondaryAccent = Color(0xFFC4B5E0),
    ctaFill = Color(0xFF1E1B3A),
    ctaText = Color(0xFFF5EDE0),
    iconMarkTint1 = Color(0xFF1E1B3A),
    iconMarkTint2 = Color(0xFFE8703D),
    cornerRadius = 20.dp,
    wordmarkFontFamily = Fraunces,
    wordmarkAllCaps = false,
    decorativeMotif = DecorativeMotif.BLOBS,
    isDark = false,
    editorBackground = Color(0xFF1E1B3A),
    editorText = Color(0xFFF5EDE0),
    editorTextSecondary = Color(0xFFC4B5E0),
    editorBorder = Color(0xFFC4B5E0)
)

val AmberNoir = ThemeTokens(
    id = "amber_noir",
    displayName = "Amber Noir",
    description = "Dramatic and moody opening, transitioning to clear, readable workspaces.",
    welcomeBackground = Color(0xFF0A0A0A),
    screenBackground = Color(0xFFF7F2E8),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1A1A1A), // Light screens
    textSecondary = Color(0xFF8A8078),
    primaryAccent = Color(0xFFE8703D),
    secondaryAccent = Color(0xFF3D5A52),
    ctaFill = Color(0xFF0A0A0A),
    ctaText = Color(0xFFF5EDE0),
    iconMarkTint1 = Color(0xFFE8703D),
    iconMarkTint2 = Color(0xFFF5EDE0),
    cornerRadius = 20.dp,
    wordmarkFontFamily = Fraunces,
    wordmarkAllCaps = false,
    decorativeMotif = DecorativeMotif.BLOBS,
    isDark = false,
    editorBackground = Color(0xFF1A1A1A),
    editorText = Color(0xFFF7F2E8),
    editorTextSecondary = Color(0xFF8A8078),
    editorBorder = Color(0xFF3D5A52)
)

val CosmicPurple = ThemeTokens(
    id = "cosmic_purple",
    displayName = "Cosmic Purple",
    description = "Bold, futuristic and high-energy. For creators who love to stand out.",
    welcomeBackground = Color(0xFF150826),
    screenBackground = Color(0xFF150826),
    surface = Color(0xFF241640),
    textPrimary = Color(0xFFF0E8FF),
    textSecondary = Color(0xFFB8A3D9),
    primaryAccent = Color(0xFF8B5CF6),
    secondaryAccent = Color(0xFFC4A8F0),
    ctaFill = Color(0xFF8B5CF6),
    ctaText = Color(0xFFFFFFFF),
    iconMarkTint1 = Color(0xFFFFFFFF),
    iconMarkTint2 = Color(0xFF8B5CF6),
    cornerRadius = 20.dp,
    wordmarkFontFamily = Inter,
    wordmarkAllCaps = true,
    decorativeMotif = DecorativeMotif.NEBULA,
    isDark = true,
    editorBackground = Color(0xFF2D2144),
    editorText = Color(0xFFF0E8FF),
    editorTextSecondary = Color(0xFFB8A3D9),
    editorBorder = Color(0xFF8B5CF6)
)

val NatureSage = ThemeTokens(
    id = "nature_sage",
    displayName = "Nature Sage",
    description = "Calm, organic, and grounded. Perfect for lifestyle and wellness content.",
    welcomeBackground = Color(0xFFE8E5D8),
    screenBackground = Color(0xFFE8E5D8),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF2A332A),
    textSecondary = Color(0xFF6B7268),
    primaryAccent = Color(0xFF4A5D48),
    secondaryAccent = Color(0xFF9BAE93),
    ctaFill = Color(0xFF4A5D48),
    ctaText = Color(0xFFF5EDE0),
    iconMarkTint1 = Color(0xFF4A5D48),
    iconMarkTint2 = Color(0xFF9BAE93), // Sage two-tone
    cornerRadius = 20.dp,
    wordmarkFontFamily = Fraunces,
    wordmarkAllCaps = false,
    decorativeMotif = DecorativeMotif.BOTANICAL,
    isDark = false,
    editorBackground = Color(0xFF2A332A),
    editorText = Color(0xFFE8E5D8),
    editorTextSecondary = Color(0xFF9BAE93),
    editorBorder = Color(0xFF4A5D48)
)

val RisoPrint = ThemeTokens(
    id = "riso_print",
    displayName = "Riso Print",
    description = "Playful, textured, and slightly imperfect. For zines and indie vibes.",
    welcomeBackground = Color(0xFFF5EBE0),
    screenBackground = Color(0xFFF5EBE0),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B6560),
    primaryAccent = Color(0xFFFF3E96),
    secondaryAccent = Color(0xFF3D7FB8), // plus yellow in actual draw
    ctaFill = Color(0xFFFF3E96),
    ctaText = Color(0xFFFFFFFF), // White text on pink fill for contrast
    iconMarkTint1 = Color(0xFFFF3E96),
    iconMarkTint2 = Color(0xFF1A1A1A),
    cornerRadius = 12.dp, // Tighter
    wordmarkFontFamily = SpaceGrotesk,
    wordmarkAllCaps = true,
    decorativeMotif = DecorativeMotif.HALFTONE,
    isDark = false,
    editorBackground = Color(0xFF1A1A1A),
    editorText = Color(0xFFF5EBE0),
    editorTextSecondary = Color(0xFF6B6560),
    editorBorder = Color(0xFF3D7FB8)
)

val EmeraldEditorial = ThemeTokens(
    id = "emerald_editorial",
    displayName = "Emerald Editorial",
    description = "Rich greens and warm terracotta. A sophisticated natural palette.",
    welcomeBackground = Color(0xFF16241C),
    screenBackground = Color(0xFFF7F2E8),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1A2B20),
    textSecondary = Color(0xFF7A8577),
    primaryAccent = Color(0xFFC4602E),
    secondaryAccent = Color(0xFF8FA88A),
    ctaFill = Color(0xFFC4602E), // Terracotta
    ctaText = Color(0xFFFFFFFF), // White text on terracotta
    iconMarkTint1 = Color(0xFFFFFFFF),
    iconMarkTint2 = Color(0xFFF5EDE0),
    cornerRadius = 20.dp,
    wordmarkFontFamily = Fraunces,
    wordmarkAllCaps = false,
    decorativeMotif = DecorativeMotif.BLOBS, // Warmer terracotta + sage
    isDark = false,
    editorBackground = Color(0xFF1A2B20),
    editorText = Color(0xFFF7F2E8),
    editorTextSecondary = Color(0xFF8FA88A),
    editorBorder = Color(0xFF8FA88A)
)

val AllThemes = listOf(
    SunsetEditorial,
    AmberNoir,
    CosmicPurple,
    NatureSage,
    RisoPrint,
    EmeraldEditorial
)
