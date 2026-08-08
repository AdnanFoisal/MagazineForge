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

/**
 * A theme is a complete system, not a recolour. Every theme fills in the same
 * three-step surface hierarchy:
 *
 *   base    -> screenBackground   the page itself
 *   raised  -> surface            cards, sheets, nav bars
 *   overlay -> surfaceOverlay     menus, dialogs, anything floating above a card
 *
 * editorSurface / editorGutter stay dark in every theme, light ones included, so
 * the LaTeX editor always reads as a code surface rather than as a document.
 */
data class ThemeTokens(
    val id: String,
    val displayName: String,
    val description: String,
    val welcomeBackground: Color,
    val screenBackground: Color,
    val surface: Color,
    val surfaceOverlay: Color,
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
    val editorSurface: Color,
    val editorGutter: Color,
    val editorBackground: Color,
    val editorText: Color,
    val editorTextSecondary: Color,
    val editorBorder: Color
)

/**
 * Warm paper stock and iron-gall ink. Terracotta does the pointing, slate blue
 * cools it back down. The light theme for people who like printed things.
 */
val PaperAndInk = ThemeTokens(
    id = "paper_ink",
    displayName = "Paper & Ink",
    description = "Warm paper, iron-gall ink, a terracotta pointer. Classic editorial calm.",
    welcomeBackground = Color(0xFFF4EFE6),
    screenBackground = Color(0xFFF4EFE6),
    surface = Color(0xFFFBF8F2),
    surfaceOverlay = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1B1A17),
    textSecondary = Color(0xFF5F5A50),
    primaryAccent = Color(0xFFB4472B),
    secondaryAccent = Color(0xFF2F4858),
    ctaFill = Color(0xFF1B1A17),
    ctaText = Color(0xFFF4EFE6),
    iconMarkTint1 = Color(0xFF1B1A17),
    iconMarkTint2 = Color(0xFFB4472B),
    cornerRadius = 18.dp,
    wordmarkFontFamily = Fraunces,
    wordmarkAllCaps = false,
    decorativeMotif = DecorativeMotif.BLOBS,
    isDark = false,
    editorSurface = Color(0xFF22201C),
    editorGutter = Color(0xFF2C2925),
    editorBackground = Color(0xFF22201C),
    editorText = Color(0xFFF4EFE6),
    editorTextSecondary = Color(0xFFA39A8C),
    editorBorder = Color(0xFF3D3831)
)

/**
 * Near-black with struck gold. Surfaces step up in tiny increments so the gold
 * is the only thing that ever raises its voice.
 */
val ObsidianGold = ThemeTokens(
    id = "obsidian_gold",
    displayName = "Obsidian & Gold",
    description = "Near-black surfaces, struck-gold accents. Quiet luxury for night work.",
    welcomeBackground = Color(0xFF0B0B0C),
    screenBackground = Color(0xFF0B0B0C),
    surface = Color(0xFF15161A),
    surfaceOverlay = Color(0xFF1E2026),
    textPrimary = Color(0xFFF2EFE9),
    textSecondary = Color(0xFFA9A29A),
    primaryAccent = Color(0xFFC9A227),
    secondaryAccent = Color(0xFFE0C878),
    ctaFill = Color(0xFFC9A227),
    ctaText = Color(0xFF0B0B0C),
    iconMarkTint1 = Color(0xFFC9A227),
    iconMarkTint2 = Color(0xFFF2EFE9),
    cornerRadius = 16.dp,
    wordmarkFontFamily = Fraunces,
    wordmarkAllCaps = false,
    decorativeMotif = DecorativeMotif.BLOBS,
    isDark = true,
    editorSurface = Color(0xFF121316),
    editorGutter = Color(0xFF1A1C21),
    editorBackground = Color(0xFF121316),
    editorText = Color(0xFFF2EFE9),
    editorTextSecondary = Color(0xFF9A948B),
    editorBorder = Color(0xFF2A2C33)
)

/**
 * Cool slate. A blue-grey dark theme with a cyan pointer: technical, even,
 * unfussy. The one that looks least like a brand and most like a tool.
 */
val CoolSlate = ThemeTokens(
    id = "cool_slate",
    displayName = "Cool Slate",
    description = "Blue-grey surfaces with a cyan pointer. Technical, even, unfussy.",
    welcomeBackground = Color(0xFF0E141B),
    screenBackground = Color(0xFF0E141B),
    surface = Color(0xFF17202A),
    surfaceOverlay = Color(0xFF202B38),
    textPrimary = Color(0xFFE6EDF3),
    textSecondary = Color(0xFF9AAAB8),
    primaryAccent = Color(0xFF3FB8C4),
    secondaryAccent = Color(0xFF7C93A8),
    ctaFill = Color(0xFF3FB8C4),
    ctaText = Color(0xFF07222A),
    iconMarkTint1 = Color(0xFF3FB8C4),
    iconMarkTint2 = Color(0xFFE6EDF3),
    cornerRadius = 14.dp,
    wordmarkFontFamily = Inter,
    wordmarkAllCaps = false,
    decorativeMotif = DecorativeMotif.NEBULA,
    isDark = true,
    editorSurface = Color(0xFF121A22),
    editorGutter = Color(0xFF1A242F),
    editorBackground = Color(0xFF121A22),
    editorText = Color(0xFFE6EDF3),
    editorTextSecondary = Color(0xFF8B9BA9),
    editorBorder = Color(0xFF2A3946)
)

/**
 * High-contrast mono. Pure black on pure white, one red for emphasis and
 * nothing else. Tight corners, all-caps grotesk. A broadsheet, basically.
 */
val PressMono = ThemeTokens(
    id = "press_mono",
    displayName = "Press Mono",
    description = "Black, white, and one red. Maximum contrast, broadsheet discipline.",
    welcomeBackground = Color(0xFFFFFFFF),
    screenBackground = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F2),
    surfaceOverlay = Color(0xFFE6E6E6),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF4A4A4A),
    primaryAccent = Color(0xFFD8102A),
    secondaryAccent = Color(0xFF000000),
    ctaFill = Color(0xFF000000),
    ctaText = Color(0xFFFFFFFF),
    iconMarkTint1 = Color(0xFF000000),
    iconMarkTint2 = Color(0xFFD8102A),
    cornerRadius = 4.dp,
    wordmarkFontFamily = SpaceGrotesk,
    wordmarkAllCaps = true,
    decorativeMotif = DecorativeMotif.HALFTONE,
    isDark = false,
    editorSurface = Color(0xFF141414),
    editorGutter = Color(0xFF1F1F1F),
    editorBackground = Color(0xFF141414),
    editorText = Color(0xFFFAFAFA),
    editorTextSecondary = Color(0xFF9E9E9E),
    editorBorder = Color(0xFF303030)
)

/**
 * Saturated jewel tone. Deep aubergine surfaces under a hot magenta and a
 * jade counterweight. The loud one, kept legible by very dark bases.
 */
val JewelAubergine = ThemeTokens(
    id = "jewel_aubergine",
    displayName = "Jewel Aubergine",
    description = "Deep aubergine, hot magenta, a jade counterweight. The loud one.",
    welcomeBackground = Color(0xFF17091F),
    screenBackground = Color(0xFF17091F),
    surface = Color(0xFF23122E),
    surfaceOverlay = Color(0xFF2F1A3D),
    textPrimary = Color(0xFFF6EAFB),
    textSecondary = Color(0xFFB9A0C6),
    primaryAccent = Color(0xFFE0409B),
    secondaryAccent = Color(0xFF35C4A0),
    ctaFill = Color(0xFFE0409B),
    ctaText = Color(0xFF1B0A24),
    iconMarkTint1 = Color(0xFFE0409B),
    iconMarkTint2 = Color(0xFF35C4A0),
    cornerRadius = 22.dp,
    wordmarkFontFamily = SpaceGrotesk,
    wordmarkAllCaps = true,
    decorativeMotif = DecorativeMotif.NEBULA,
    isDark = true,
    editorSurface = Color(0xFF1B0D24),
    editorGutter = Color(0xFF25142F),
    editorBackground = Color(0xFF1B0D24),
    editorText = Color(0xFFF6EAFB),
    editorTextSecondary = Color(0xFFAE94BC),
    editorBorder = Color(0xFF3A2247)
)

val AllThemes = listOf(
    PaperAndInk,
    ObsidianGold,
    CoolSlate,
    PressMono,
    JewelAubergine
)

/** The theme a fresh install, or an unrecognised saved id, resolves to. */
val DefaultTheme = PaperAndInk

/**
 * Compatibility alias. MainActivity still falls back to `SunsetEditorial` by
 * name when a saved id is unknown, and that file is not ours to change; keeping
 * the symbol pointed at [DefaultTheme] means that fallback keeps working and
 * lands on the new default. Prefer [themeForId] for new call sites.
 */
@Deprecated(
    message = "Replaced by the five-theme set. Use themeForId(savedId) or DefaultTheme.",
    replaceWith = ReplaceWith("DefaultTheme")
)
val SunsetEditorial = DefaultTheme

/**
 * Resolve a persisted theme id to a live theme.
 *
 * Ids from the previous six-theme set (sunset_editorial, amber_noir,
 * cosmic_purple, nature_sage, riso_print, emerald_editorial) are no longer in
 * [AllThemes]. Rather than dropping those users on the default, map each old id
 * to whichever new theme is closest in mood; anything else, including null,
 * falls back to [DefaultTheme]. Nobody upgrading is ever left themeless.
 */
fun themeForId(id: String?): ThemeTokens {
    AllThemes.firstOrNull { it.id == id }?.let { return it }
    return when (id) {
        "sunset_editorial" -> PaperAndInk
        "amber_noir" -> ObsidianGold
        "cosmic_purple" -> JewelAubergine
        "nature_sage" -> PaperAndInk
        "riso_print" -> PressMono
        "emerald_editorial" -> CoolSlate
        else -> DefaultTheme
    }
}
