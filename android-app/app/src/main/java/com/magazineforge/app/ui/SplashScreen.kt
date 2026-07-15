package com.magazineforge.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.R
import com.magazineforge.app.ui.theme.LocalThemeTokens
import com.magazineforge.app.ui.theme.LuxeTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val tokens = LocalThemeTokens.current
    
    val logoScale = remember { Animatable(0.8f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(20f) }
    
    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing)
            )
            // Subtle pulse effect
            logoScale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 800)
            )
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }
        
        // Delay text entrance slightly for stagger effect
        delay(400)
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        launch {
            textOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
            )
        }
        
        delay(2200) // Total presentation time
        onAnimationFinished()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF0F172A), // Midnight blue/slate
                        androidx.compose.ui.graphics.Color(0xFF020617)  // Deep charcoal/black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = "MagazineForge Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "MAGAZINEFORGE",
                style = LuxeTypography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = androidx.compose.ui.graphics.Color(0xFFF1F5F9) // Slate 50
                ),
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffset.value.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI Publishing Platform",
                style = LuxeTypography.labelLarge.copy(
                    letterSpacing = 2.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF94A3B8) // Slate 400
                ),
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = (textOffset.value * 0.5f).dp)
            )
        }
    }
}
