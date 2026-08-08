package com.magazineforge.app.ui.theme

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user has turned animations off system-wide (Developer options ->
 * "Animator duration scale: off", or the accessibility "Remove animations"
 * toggle, which sets the same Global setting to 0).
 *
 * Callers should skip straight to the final frame rather than animating.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (e: Exception) {
            1f
        }
        scale == 0f
    }
}

/**
 * The MagazineForge brand mark: a two-tone folded ribbon.
 *
 * [tint1] / [tint2] default to the active theme's icon tints. The splash passes
 * explicit colours and drives the two alphas separately so the mark can draw
 * itself as an outline first and then flood with tint:
 *
 *   [strokeAlpha] opacity of the outlined edges (the "drawn" stage)
 *   [fillAlpha]   opacity of the solid two-tone faces (the "inked" stage)
 *
 * Defaults are the finished mark, so ordinary callers just size it and go.
 */
@Composable
fun IconMark(
    modifier: Modifier = Modifier,
    tint1: Color? = null,
    tint2: Color? = null,
    strokeAlpha: Float = 0f,
    fillAlpha: Float = 1f
) {
    val tokens = LocalThemeTokens.current
    val faceA = tint1 ?: tokens.iconMarkTint1
    val faceB = tint2 ?: tokens.iconMarkTint2
    val strokeA = strokeAlpha.coerceIn(0f, 1f)
    val fillA = fillAlpha.coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Front face of the ribbon.
        val path1 = Path().apply {
            moveTo(width * 0.2f, height * 0.1f)
            lineTo(width * 0.6f, height * 0.1f)
            lineTo(width * 0.8f, height * 0.5f)
            lineTo(width * 0.4f, height * 0.9f)
            close()
        }
        // The fold behind it.
        val path2 = Path().apply {
            moveTo(width * 0.6f, height * 0.1f)
            lineTo(width * 0.8f, height * 0.1f)
            lineTo(width * 0.8f, height * 0.8f)
            lineTo(width * 0.4f, height * 0.9f)
            close()
        }

        if (strokeA > 0f) {
            val edge = Stroke(width = size.minDimension * 0.045f)
            drawPath(path = path1, color = faceA, alpha = strokeA, style = edge)
            drawPath(path = path2, color = faceB, alpha = strokeA, style = edge)
        }
        if (fillA > 0f) {
            drawPath(path = path1, color = faceA, alpha = fillA, style = Fill)
            drawPath(path = path2, color = faceB, alpha = fillA, style = Fill)
        }
    }
}

@Composable
fun ThemeBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val tokens = LocalThemeTokens.current

    Box(modifier = modifier
        .fillMaxSize()
        .background(tokens.welcomeBackground)) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            when (tokens.decorativeMotif) {
                DecorativeMotif.BLOBS -> {
                    // Soft overlapping blobs using primary and secondary accents
                    drawCircle(
                        color = tokens.primaryAccent.copy(alpha = 0.08f),
                        radius = width * 0.6f,
                        center = Offset(0f, 0f)
                    )
                    drawCircle(
                        color = tokens.secondaryAccent.copy(alpha = 0.16f),
                        radius = width * 0.7f,
                        center = Offset(width, height)
                    )
                }
                DecorativeMotif.NEBULA -> {
                    // Cosmic purple / nebula gradient glow
                    val gradient = Brush.radialGradient(
                        colors = listOf(tokens.secondaryAccent.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(width * 0.5f, height * 0.3f),
                        radius = width * 0.8f
                    )
                    drawRect(brush = gradient)
                }
                DecorativeMotif.BOTANICAL -> {
                    // Botanical line art (simple leaves)
                    val leafPath = Path().apply {
                        moveTo(width * 0.8f, height * 0.2f)
                        quadraticBezierTo(width * 0.9f, height * 0.1f, width * 0.95f, height * 0.25f)
                        quadraticBezierTo(width * 0.85f, height * 0.3f, width * 0.8f, height * 0.2f)
                    }
                    drawPath(
                        path = leafPath,
                        color = tokens.primaryAccent.copy(alpha = 0.2f),
                        style = Fill
                    )
                    // Stem
                    drawLine(
                        color = tokens.primaryAccent.copy(alpha = 0.3f),
                        start = Offset(width * 0.8f, height * 0.2f),
                        end = Offset(width * 0.75f, height * 0.4f),
                        strokeWidth = 4f
                    )
                }
                DecorativeMotif.HALFTONE -> {
                    // Halftone dots simulation (drawn as a grid of small circles)
                    val dotSpacing = 20f
                    val columns = (width / dotSpacing).toInt()
                    val rows = (height / dotSpacing).toInt()

                    for (i in 0..columns) {
                        for (j in 0..rows) {
                            // Only draw some dots to make a texture in the corner
                            if (i + j > columns + rows - 30) {
                                drawCircle(
                                    color = tokens.primaryAccent.copy(alpha = 0.15f),
                                    radius = 3f,
                                    center = Offset(i * dotSpacing, j * dotSpacing)
                                )
                            }
                        }
                    }
                    // Scribble star accent
                    val starPath = Path().apply {
                        moveTo(width * 0.2f, height * 0.1f)
                        lineTo(width * 0.22f, height * 0.15f)
                        lineTo(width * 0.28f, height * 0.14f)
                        lineTo(width * 0.24f, height * 0.19f)
                        lineTo(width * 0.26f, height * 0.25f)
                        lineTo(width * 0.2f, height * 0.21f)
                        lineTo(width * 0.14f, height * 0.25f)
                        lineTo(width * 0.16f, height * 0.19f)
                        lineTo(width * 0.12f, height * 0.14f)
                        lineTo(width * 0.18f, height * 0.15f)
                        close()
                    }
                    drawPath(
                        path = starPath,
                        color = tokens.secondaryAccent.copy(alpha = 0.4f), // Blue star
                        style = Stroke(width = 3f)
                    )
                }
                DecorativeMotif.NONE -> {}
            }
        }

        content()
    }
}
