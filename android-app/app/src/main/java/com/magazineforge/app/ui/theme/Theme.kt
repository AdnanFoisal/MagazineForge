package com.magazineforge.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LuxeDarkColorScheme = darkColorScheme(
    primary = EditorialGold,
    onPrimary = PitchBlack,
    secondary = GoldBright,
    background = DarkSurface,
    surface = Graphite,
    onBackground = GhostWhite,
    onSurface = GhostWhite,
    error = ErrorRed
)

@Composable
fun LuxeEditorialNoirTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LuxeDarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PitchBlack.toArgb()
            window.navigationBarColor = PitchBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LuxeTypography,
        content = content
    )
}
