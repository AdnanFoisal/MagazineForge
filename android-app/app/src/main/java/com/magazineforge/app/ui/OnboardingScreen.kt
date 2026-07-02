package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.magazineforge.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onVerifyClicked: (String) -> Unit,
    onOpenEditorClicked: () -> Unit = {},
    isVerifying: Boolean = false,
    verifyError: String? = null
) {
    var apiKey by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Text(
                text = "MAGAZINE FORGE",
                style = MaterialTheme.typography.headlineMedium,
                color = GhostWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Connect your creative engine.",
                style = MaterialTheme.typography.bodyLarge,
                color = AshGrey,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API Key", style = MaterialTheme.typography.labelMedium) },
                placeholder = { Text("AIzaSy...", style = MaterialTheme.typography.bodyMedium, color = BorderLight) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = GhostWhite),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EditorialGold,
                    unfocusedBorderColor = BorderDark,
                    cursorColor = EditorialGold,
                    focusedLabelColor = EditorialGold,
                    unfocusedLabelColor = AshGrey,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            if (verifyError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = verifyError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onVerifyClicked(apiKey) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EditorialGold,
                    contentColor = PitchBlack,
                    disabledContainerColor = BorderDark,
                    disabledContentColor = AshGrey
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = apiKey.isNotBlank() && !isVerifying
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PitchBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Authenticating...",
                        style = MaterialTheme.typography.labelMedium,
                        color = PitchBlack
                    )
                } else {
                    Text(
                        text = "Initialize",
                        style = MaterialTheme.typography.labelMedium,
                        color = PitchBlack
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onOpenEditorClicked) {
                Text("Open Raw Editor", style = MaterialTheme.typography.labelLarge, color = EditorialGold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Get an API key from Google AI Studio",
                style = MaterialTheme.typography.labelSmall,
                color = EditorialGold,
                textAlign = TextAlign.Center
            )
        }
    }
}
