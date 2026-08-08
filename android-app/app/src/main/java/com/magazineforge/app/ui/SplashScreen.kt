package com.magazineforge.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.ui.theme.IconMark
import com.magazineforge.app.ui.theme.LocalThemeTokens
import com.magazineforge.app.ui.theme.LuxeTypography
import com.magazineforge.app.ui.theme.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Splash timeline, ~1400ms end to end.
private const val MARK_STROKE_MS = 420    // edges draw themselves in
private const val MARK_FILL_MS = 460      // tint floods the faces
private const val MARK_FILL_DELAY_MS = 260
private const val WORDMARK_MS = 520       // letter-spacing opens up
private const val WORDMARK_DELAY_MS = 480
private const val HOLD_MS = 400           // beat on the finished frame
private const val TOTAL_MS =
    WORDMARK_DELAY_MS + WORDMARK_MS + HOLD_MS   // 480 + 520 + 400 = 1400

// Beat held before handing off when animations are disabled.
private const val REDUCED_MOTION_HOLD_MS = 200

private const val LETTER_SPACING_FROM = 1f
private const val LETTER_SPACING_TO = 7f

/**
 * Brand splash.
 *
 * Three overlapping stages, all theme-driven: the mark's edges stroke in, the
 * two-tone tint floods the faces, then the wordmark fades up while its
 * letter-spacing opens from 1sp to 7sp. Total ~1400ms including the system
 * splash handoff.
 *
 * When the system animator duration scale is 0 (developer options, or the
 * accessibility "remove animations" toggle) every value is pinned to its final
 * state and we hand off after a single short beat instead of animating.
 */
@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val tokens = LocalThemeTokens.current
    val reducedMotion = rememberReducedMotion()

    // Under reduced motion these start at their final values, so the very first
    // frame is already the finished composition and nothing ever moves.
    val markStroke = remember(reducedMotion) { Animatable(0f) }
    val markFill = remember(reducedMotion) { Animatable(if (reducedMotion) 1f else 0f) }
    val wordmarkAlpha = remember(reducedMotion) { Animatable(if (reducedMotion) 1f else 0f) }
    val letterSpacing = remember(reducedMotion) {
        Animatable(if (reducedMotion) LETTER_SPACING_TO else LETTER_SPACING_FROM)
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            delay(REDUCED_MOTION_HOLD_MS.toLong())
            onAnimationFinished()
            return@LaunchedEffect
        }

        // Stage 1: the mark draws its own edges.
        launch {
            markStroke.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = MARK_STROKE_MS, easing = LinearOutSlowInEasing)
            )
        }
        // Stage 2: tint floods in, and the outline recedes under it.
        launch {
            delay(MARK_FILL_DELAY_MS.toLong())
            launch {
                markFill.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = MARK_FILL_MS, easing = LinearOutSlowInEasing)
                )
            }
            markStroke.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = MARK_FILL_MS)
            )
        }
        // Stage 3: wordmark fades up as its tracking opens out.
        launch {
            delay(WORDMARK_DELAY_MS.toLong())
            launch {
                wordmarkAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = WORDMARK_MS)
                )
            }
            letterSpacing.animateTo(
                targetValue = LETTER_SPACING_TO,
                animationSpec = tween(durationMillis = WORDMARK_MS, easing = LinearOutSlowInEasing)
            )
        }

        delay(TOTAL_MS.toLong())
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.welcomeBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconMark(
                modifier = Modifier.size(96.dp),
                tint1 = tokens.iconMarkTint1,
                tint2 = tokens.iconMarkTint2,
                strokeAlpha = markStroke.value,
                fillAlpha = markFill.value
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = if (tokens.wordmarkAllCaps) "MAGAZINEFORGE" else "MagazineForge",
                style = LuxeTypography.headlineSmall.copy(
                    fontFamily = tokens.wordmarkFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = letterSpacing.value.sp,
                    color = tokens.textPrimary
                ),
                modifier = Modifier.alpha(wordmarkAlpha.value)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "AI Publishing Platform",
                style = LuxeTypography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    color = tokens.textSecondary
                ),
                modifier = Modifier.alpha(wordmarkAlpha.value)
            )
        }
    }
}
