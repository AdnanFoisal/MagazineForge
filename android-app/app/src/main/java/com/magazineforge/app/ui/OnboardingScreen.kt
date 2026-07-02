package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onVerifyClicked: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }

    val obsidian = Color(0xFF0F0F10)
    val darkSurface = Color(0xFF18181B)
    val gold = Color(0xFFC5A059)
    val copper = Color(0xFFB87333)
    val ivory = Color(0xFFF5F5F7)
    val mutedGray = Color(0xFFA1A1AA)
    val borderCol = Color(0xFF2E2A24)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(obsidian, Color(0xFF1A1510), obsidian)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Logo Area
            Text(
                text = "M",
                style = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 64.sp,
                    color = gold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MAGAZINE FORGE",
                style = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = ivory,
                    letterSpacing = 4.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Craft stunning publications with AI",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    color = mutedGray,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // API Key Input Card
            Surface(
                color = darkSurface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Your Gemini API Key",
                        style = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ivory
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your key powers the AI content engine. It is stored locally on this device only.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.SansSerif,
                            color = mutedGray
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Gemini API Key", color = gold, fontFamily = FontFamily.SansSerif) },
                        placeholder = { Text("AIzaSy...", color = mutedGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = ivory, fontFamily = FontFamily.SansSerif),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gold,
                            unfocusedBorderColor = borderCol,
                            cursorColor = gold,
                            focusedLabelColor = gold,
                            unfocusedLabelColor = mutedGray
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onVerifyClicked(apiKey) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gold,
                            contentColor = obsidian,
                            disabledContainerColor = borderCol,
                            disabledContentColor = mutedGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = apiKey.isNotBlank()
                    ) {
                        Text(
                            text = "Verify & Continue",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
