package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.magazineforge.app.ui.theme.ThemeState
import com.magazineforge.app.ui.theme.AllThemes
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentApiKey: String,
    currentBackupApiKey: String?,
    isVerifying: Boolean,
    verifyError: String?,
    verifySuccess: Boolean?,
    onSaveApiKeys: (String, String) -> Unit,
    onClearFeedback: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var backupApiKeyInput by remember { mutableStateOf(currentBackupApiKey ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    val currentTheme by ThemeState.currentTheme.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground)
        )
        
        // API Key Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Gemini API Keys", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            onClearFeedback()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Primary API Key") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (passwordVisible) "Hide API Key" else "Show API Key"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    var backupPasswordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = backupApiKeyInput,
                        onValueChange = {
                            backupApiKeyInput = it
                            onClearFeedback()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Backup API Key (Optional)") },
                        visualTransformation = if (backupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (backupPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (backupPasswordVisible) "Hide API Key" else "Show API Key"
                            IconButton(onClick = { backupPasswordVisible = !backupPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    if (verifySuccess == true) {
                        Text(
                            text = "Keys saved successfully!",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else if (verifyError != null) {
                        Text(
                            text = verifyError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Button(
                    onClick = { onSaveApiKeys(apiKeyInput, backupApiKeyInput) },
                    enabled = !isVerifying,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...", color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Verify & Save", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        
        // Theme Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            Text(
                "Appearance", 
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(AllThemes) { theme ->
                    val isSelected = theme == currentTheme
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (theme.isDark) Color(0xFF1A1A1A) else Color(0xFFF5F5F5))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { ThemeState.setTheme(theme) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (theme.isDark) Color.White else Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}
