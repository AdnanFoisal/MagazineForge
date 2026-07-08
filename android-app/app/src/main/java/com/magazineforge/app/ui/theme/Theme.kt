package com.magazineforge.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemeState {
    private val _currentTheme = MutableStateFlow(SunsetEditorial)
    val currentTheme: StateFlow<ThemeTokens> = _currentTheme
    
    fun setTheme(theme: ThemeTokens) {
        _currentTheme.value = theme
    }
}

val LocalThemeTokens = staticCompositionLocalOf<ThemeTokens> {
    error("No ThemeTokens provided")
}

@Composable
fun MagazineForgeTheme(content: @Composable () -> Unit) {
    val currentTheme by ThemeState.currentTheme.collectAsState()
    
    // Animate all token properties for seamless transitions
    val animationSpec = tween<Color>(durationMillis = 300)
    
    val welcomeBackground by animateColorAsState(currentTheme.welcomeBackground, animationSpec)
    val screenBackground by animateColorAsState(currentTheme.screenBackground, animationSpec)
    val surface by animateColorAsState(currentTheme.surface, animationSpec)
    val textPrimary by animateColorAsState(currentTheme.textPrimary, animationSpec)
    val textSecondary by animateColorAsState(currentTheme.textSecondary, animationSpec)
    val primaryAccent by animateColorAsState(currentTheme.primaryAccent, animationSpec)
    val secondaryAccent by animateColorAsState(currentTheme.secondaryAccent, animationSpec)
    val ctaFill by animateColorAsState(currentTheme.ctaFill, animationSpec)
    val ctaText by animateColorAsState(currentTheme.ctaText, animationSpec)
    val iconMarkTint1 by animateColorAsState(currentTheme.iconMarkTint1, animationSpec)
    val iconMarkTint2 by animateColorAsState(currentTheme.iconMarkTint2, animationSpec)
    val cornerRadius by animateDpAsState(currentTheme.cornerRadius, tween(durationMillis = 300))
    val editorBackground by animateColorAsState(currentTheme.editorBackground, animationSpec)
    val editorText by animateColorAsState(currentTheme.editorText, animationSpec)
    val editorTextSecondary by animateColorAsState(currentTheme.editorTextSecondary, animationSpec)
    val editorBorder by animateColorAsState(currentTheme.editorBorder, animationSpec)
    
    // Construct the animated tokens object
    val animatedTokens = ThemeTokens(
        id = currentTheme.id,
        displayName = currentTheme.displayName,
        description = currentTheme.description,
        welcomeBackground = welcomeBackground,
        screenBackground = screenBackground,
        surface = surface,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent,
        ctaFill = ctaFill,
        ctaText = ctaText,
        iconMarkTint1 = iconMarkTint1,
        iconMarkTint2 = iconMarkTint2,
        cornerRadius = cornerRadius,
        wordmarkFontFamily = currentTheme.wordmarkFontFamily,
        wordmarkAllCaps = currentTheme.wordmarkAllCaps,
        decorativeMotif = currentTheme.decorativeMotif,
        isDark = currentTheme.isDark,
        editorBackground = editorBackground,
        editorText = editorText,
        editorTextSecondary = editorTextSecondary,
        editorBorder = editorBorder
    )

    // Map to MaterialTheme just for basic compatibility, but we prefer LocalThemeTokens
    val colorScheme = if (currentTheme.isDark) {
        darkColorScheme(
            primary = animatedTokens.primaryAccent,
            background = animatedTokens.screenBackground,
            surface = animatedTokens.surface,
            onBackground = animatedTokens.textPrimary,
            onSurface = animatedTokens.textPrimary
        )
    } else {
        lightColorScheme(
            primary = animatedTokens.primaryAccent,
            background = animatedTokens.screenBackground,
            surface = animatedTokens.surface,
            onBackground = animatedTokens.textPrimary,
            onSurface = animatedTokens.textPrimary
        )
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

    CompositionLocalProvider(LocalThemeTokens provides animatedTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LuxeTypography,
            content = content
        )
    }
}
