package com.magazineforge.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

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
