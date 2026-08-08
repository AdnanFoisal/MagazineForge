package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.ui.theme.IconMark
import com.magazineforge.app.ui.theme.LocalThemeTokens
import com.magazineforge.app.ui.theme.ThemeBackground

@Composable
fun OnboardingScreen(
    onExploreClicked: () -> Unit,
    onCreateClicked: () -> Unit
) {
    val tokens = LocalThemeTokens.current

    ThemeBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Icon mark
            IconMark(modifier = Modifier.size(64.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Wordmark
            val wordmarkStr = buildAnnotatedString {
                if (tokens.wordmarkAllCaps) {
                    withStyle(SpanStyle(color = tokens.textPrimary)) {
                        append("MAGAZINE")
                    }
                    withStyle(SpanStyle(color = tokens.primaryAccent)) {
                        append("FORGE")
                    }
                } else {
                    withStyle(SpanStyle(color = tokens.textPrimary)) {
                        append("Magazine")
                    }
                    withStyle(SpanStyle(color = tokens.primaryAccent)) {
                        append("Forge")
                    }
                }
            }
            
            Text(
                text = wordmarkStr,
                fontFamily = tokens.wordmarkFontFamily,
                fontWeight = if (tokens.wordmarkAllCaps) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tagline
            Text(
                text = "AI-powered magazines. Made beautifully yours.",
                color = tokens.textSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Primary CTA
            Button(
                onClick = onCreateClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.ctaFill,
                    contentColor = tokens.ctaText
                ),
                shape = RoundedCornerShape(tokens.cornerRadius)
            ) {
                Text(
                    text = "Create Your First Magazine",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Secondary CTA
            OutlinedButton(
                onClick = onExploreClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = tokens.textPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.textSecondary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(tokens.cornerRadius)
            ) {
                Text(
                    text = "Explore Templates",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(tokens.primaryAccent))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(tokens.textSecondary.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(tokens.textSecondary.copy(alpha = 0.3f)))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
