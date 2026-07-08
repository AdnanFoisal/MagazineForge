package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.magazineforge.app.ui.theme.LocalThemeTokens

@Composable
fun CoverArtImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val tokens = LocalThemeTokens.current
    val context = LocalContext.current
    
    // Create a color matrix for duotone / color grading based on theme accents
    val colorFilter = remember(tokens) {
        val matrix = ColorMatrix()
        // Convert to grayscale first
        matrix.setToSaturation(0f)
        
        // We will blend this with a tint color using BlendMode in AsyncImage
        // Coil's colorFilter can take a ColorMatrix, but to do a true duotone we can use BlendMode tint
        // Let's use a simple tint overlay via ColorFilter.tint
        ColorFilter.tint(
            color = tokens.primaryAccent.copy(alpha = 0.4f),
            blendMode = BlendMode.Color
        )
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            colorFilter = colorFilter
        )
        
        // Additional blend overlay for deeper integration
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.secondaryAccent.copy(alpha = 0.2f))
        )
    }
}
