package com.magazineforge.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeVariant(val displayName: String, val isDark: Boolean) {
    LUXE_NOIR("Luxe Noir", true),
    BRUTALIST_DARK("Brutalist Dark", true),
    NEO_TOKYO("Neo Tokyo", true),
    MIDNIGHT_VELVET("Midnight Velvet", true),
    OBSIDIAN_MINIMAL("Obsidian Minimal", true),
    EDITORIAL_LIGHT("Editorial Light", false),
    PARCHMENT_IVORY("Parchment Ivory", false),
    SWISS_MINIMAL("Swiss Minimalist", false),
    ROSE_GOLD("Rose Gold", false),
    GALLERY_WHITE("Gallery White", false)
}

object ThemeState {
    private val _currentTheme = MutableStateFlow(ThemeVariant.LUXE_NOIR)
    val currentTheme: StateFlow<ThemeVariant> = _currentTheme
    
    fun setTheme(theme: ThemeVariant) {
        _currentTheme.value = theme
    }
}

private val LuxeNoirScheme = darkColorScheme(
    primary = EditorialGold, onPrimary = PitchBlack, background = DarkSurface, surface = SurfaceContainerLow, onBackground = GhostWhite, onSurface = GhostWhite, outline = BorderDark
)
private val BrutalistDarkScheme = darkColorScheme(
    primary = BrutalAccent, onPrimary = BrutalText, background = BrutalBlack, surface = BrutalGrey, onBackground = BrutalText, onSurface = BrutalText, outline = BrutalAccent
)
private val NeoTokyoScheme = darkColorScheme(
    primary = NeoCyan, onPrimary = NeoDark, secondary = NeoPink, background = NeoDark, surface = NeoSurface, onBackground = NeoText, onSurface = NeoText, outline = NeoPink
)
private val MidnightVelvetScheme = darkColorScheme(
    primary = VelvetGold, onPrimary = VelvetDark, background = VelvetDark, surface = VelvetSurface, onBackground = VelvetText, onSurface = VelvetText, outline = VelvetGold
)
private val ObsidianMinimalScheme = darkColorScheme(
    primary = ObsAccent, onPrimary = ObsDark, background = ObsDark, surface = ObsSurface, onBackground = ObsText, onSurface = ObsText, outline = ObsSurface
)
private val EditorialLightScheme = lightColorScheme(
    primary = EdLightAccent, onPrimary = EdLightSurface, background = EdLightBg, surface = EdLightSurface, onBackground = EdLightText, onSurface = EdLightText, outline = EdLightAccent
)
private val ParchmentIvoryScheme = lightColorScheme(
    primary = ParchAccent, onPrimary = ParchSurface, background = ParchBg, surface = ParchSurface, onBackground = ParchText, onSurface = ParchText, outline = ParchAccent
)
private val SwissMinimalScheme = lightColorScheme(
    primary = SwissAccent, onPrimary = SwissSurface, background = SwissBg, surface = SwissSurface, onBackground = SwissText, onSurface = SwissText, outline = SwissAccent
)
private val RoseGoldScheme = lightColorScheme(
    primary = RoseAccent, onPrimary = RoseSurface, background = RoseBg, surface = RoseSurface, onBackground = RoseText, onSurface = RoseText, outline = RoseAccent
)
private val GalleryWhiteScheme = lightColorScheme(
    primary = GalAccent, onPrimary = GalSurface, background = GalBg, surface = GalSurface, onBackground = GalText, onSurface = GalText, outline = GalAccent
)

@Composable
fun MagazineForgeTheme(content: @Composable () -> Unit) {
    val currentTheme by ThemeState.currentTheme.collectAsState()
    
    val colorScheme = when(currentTheme) {
        ThemeVariant.LUXE_NOIR -> LuxeNoirScheme
        ThemeVariant.BRUTALIST_DARK -> BrutalistDarkScheme
        ThemeVariant.NEO_TOKYO -> NeoTokyoScheme
        ThemeVariant.MIDNIGHT_VELVET -> MidnightVelvetScheme
        ThemeVariant.OBSIDIAN_MINIMAL -> ObsidianMinimalScheme
        ThemeVariant.EDITORIAL_LIGHT -> EditorialLightScheme
        ThemeVariant.PARCHMENT_IVORY -> ParchmentIvoryScheme
        ThemeVariant.SWISS_MINIMAL -> SwissMinimalScheme
        ThemeVariant.ROSE_GOLD -> RoseGoldScheme
        ThemeVariant.GALLERY_WHITE -> GalleryWhiteScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !currentTheme.isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LuxeTypography,
        content = content
    )
}
