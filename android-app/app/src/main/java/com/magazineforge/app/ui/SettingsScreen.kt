package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import com.magazineforge.app.utils.SecureStorage

@Composable
fun SettingsScreen(
    currentLiteLLMUrl: String,
    currentLiteLLMKey: String,
    isVerifying: Boolean,
    verifyError: String?,
    verifySuccess: Boolean?,
    onSaveCredentials: (String, String) -> Unit,
    onClearFeedback: () -> Unit
) {
    var urlInput by remember { mutableStateOf(currentLiteLLMUrl) }
    var keyInput by remember { mutableStateOf(currentLiteLLMKey) }
    var passwordVisible by remember { mutableStateOf(false) }
    val currentTheme by ThemeState.currentTheme.collectAsState()
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground)
            )
        }
        
        item {
            var userNameInput by remember { mutableStateOf(secureStorage.getUserName()) }
            var isSaved by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("User Profile", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                    OutlinedTextField(
                        value = userNameInput,
                        onValueChange = {
                            if (it.length <= 7) {
                                userNameInput = it
                                isSaved = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your Name (Max 7 chars)") },
                        placeholder = { Text("e.g. Adam") },
                        singleLine = true,
                        supportingText = { Text("${userNameInput.length}/7 characters — fits cleanly on greeting line") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Button(
                        onClick = {
                            secureStorage.saveUserName(userNameInput)
                            isSaved = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (isSaved) "Profile Saved!" else "Save Profile")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("LLM Configuration (LiteLLM)", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                onClearFeedback()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("LiteLLM Base URL") },
                            placeholder = { Text("http://YOUR_IP:4000/v1") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = {
                                keyInput = it
                                onClearFeedback()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("LiteLLM Master Key") },
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
                        
                        if (verifySuccess == true) {
                            Column {
                                Text(
                                    text = "Credentials saved successfully!",
                                    color = Color(0xFF4CAF50),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                if (verifyError != null) {
                                    Text(
                                        text = verifyError,
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
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
                        onClick = { onSaveCredentials(urlInput, keyInput) },
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
        }
        
        item {
            Text(
                "Appearance", 
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground)
            )
        }
        
        val chunkedThemes = AllThemes.chunked(2)
        items(chunkedThemes) { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (theme in rowThemes) {
                    val isSelected = theme == currentTheme
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.screenBackground)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) theme.primaryAccent else theme.textSecondary.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                ThemeState.setTheme(theme)
                                secureStorage.saveThemeId(theme.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Preview each theme in its own colours rather than a
                        // generic light/dark swatch.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(theme.primaryAccent)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(theme.secondaryAccent)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(theme.surface)
                                )
                            }
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = theme.textPrimary
                                )
                            )
                        }
                    }
                }
                if (rowThemes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
