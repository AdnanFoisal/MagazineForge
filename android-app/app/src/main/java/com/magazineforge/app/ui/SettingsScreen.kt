package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.magazineforge.app.models.ImageKeyResult
import com.magazineforge.app.models.VerifyImageKeysRequest
import com.magazineforge.app.network.ApiClient
import kotlinx.coroutines.launch

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
            ImageApiKeysCard()
        }

        item {
            var selectedVisualRegister by remember { mutableStateOf(secureStorage.getVisualRegister()) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Visual Style", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                    Text(
                        "Auto lets the AI pick the register from what you asked for. Choose one to override it.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    // Four chips do not fit across a 360dp phone once the screen and
                    // card padding are taken out, and Material3 1.2.0's FilterChip has
                    // no contentPadding to trim, so the row scrolls instead of
                    // squeezing "Editorial"/"Technical" onto two lines.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (register in SecureStorage.VISUAL_REGISTERS) {
                            val isSelected = selectedVisualRegister == register
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedVisualRegister = register
                                    secureStorage.saveVisualRegister(register)
                                },
                                label = {
                                    Text(
                                        text = register.replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
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

/**
 * "Image Sources" — the user's own Pixabay and Pexels keys.
 *
 * Each provider is verified with one small live search and saved on its own, so
 * a working Pixabay key is kept even when the Pexels box holds a typo. A box
 * left empty, or a key that fails verification, simply leaves that provider on
 * the app's shared key — nothing here can break image search.
 *
 * Self-contained on purpose: the LLM card's state is hoisted into MainActivity
 * because generation needs those credentials, whereas these keys are only ever
 * read by the interceptor, so hoisting them would add a parameter to every
 * SettingsScreen call site for no benefit.
 */
@Composable
private fun ImageApiKeysCard() {
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    val scope = rememberCoroutineScope()

    var pixabayInput by remember { mutableStateOf(secureStorage.getPixabayKey()) }
    var pexelsInput by remember { mutableStateOf(secureStorage.getPexelsKey()) }
    var keysVisible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    // null = not checked this session; the pair is (ok, message).
    var pixabayResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var pexelsResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var transportError by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Image Sources",
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                "Optional. Add your own free API keys to get your own image quota. " +
                    "Leave a box empty to keep using the built-in key for that service.",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            ImageKeyField(
                value = pixabayInput,
                onValueChange = {
                    pixabayInput = it
                    pixabayResult = null
                    transportError = null
                },
                label = "Pixabay API Key",
                placeholder = "pixabay.com/api/docs",
                visible = keysVisible,
                onToggleVisibility = { keysVisible = !keysVisible },
                result = pixabayResult
            )

            ImageKeyField(
                value = pexelsInput,
                onValueChange = {
                    pexelsInput = it
                    pexelsResult = null
                    transportError = null
                },
                label = "Pexels API Key",
                placeholder = "pexels.com/api",
                visible = keysVisible,
                onToggleVisibility = { keysVisible = !keysVisible },
                result = pexelsResult
            )

            if (transportError != null) {
                Text(
                    text = transportError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isVerifying = true
                        transportError = null
                        pixabayResult = null
                        pexelsResult = null
                        val outcome = verifyAndSaveImageKeys(
                            secureStorage, pixabayInput.trim(), pexelsInput.trim()
                        )
                        pixabayResult = outcome.pixabay
                        pexelsResult = outcome.pexels
                        transportError = outcome.transportError
                        isVerifying = false
                    }
                },
                enabled = !isVerifying,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing keys...", color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Verify & Save Keys", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

/** One key box with its own inline verdict. */
@Composable
private fun ImageKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    result: Pair<Boolean, String>?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = image,
                        contentDescription = if (visible) "Hide $label" else "Show $label"
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        if (result != null) {
            val (ok, message) = result
            Text(
                text = (if (ok) "✓ " else "• ") + message,
                color = if (ok) VERIFIED_GREEN else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Matches the LLM card's success colour rather than colorScheme.primary, which
// is the accent in some themes and would read as neutral rather than "passed".
private val VERIFIED_GREEN = Color(0xFF4CAF50)

private data class ImageKeyOutcome(
    val pixabay: Pair<Boolean, String>?,
    val pexels: Pair<Boolean, String>?,
    val transportError: String?
)

/**
 * Verifies both keys in one round trip and persists only the ones that passed.
 *
 * A rejected or blank key is CLEARED rather than left in place, so the app falls
 * back to the built-in key for that provider — the behaviour the field's own
 * message promises. Storage is updated before [ApiClient]'s cached copies so a
 * generation started immediately after saving cannot pick up a key that failed
 * to persist.
 */
private suspend fun verifyAndSaveImageKeys(
    secureStorage: SecureStorage,
    pixabayKey: String,
    pexelsKey: String
): ImageKeyOutcome {
    if (pixabayKey.isEmpty() && pexelsKey.isEmpty()) {
        secureStorage.clearPixabayKey()
        secureStorage.clearPexelsKey()
        ApiClient.userPixabayKey = ""
        ApiClient.userPexelsKey = ""
        val message = "Using the built-in key"
        return ImageKeyOutcome(false to message, false to message, null)
    }

    val response = try {
        ApiClient.retrofitService.verifyImageKeys(
            VerifyImageKeysRequest(pixabay_key = pixabayKey, pexels_key = pexelsKey)
        )
    } catch (e: Exception) {
        return ImageKeyOutcome(null, null, "Could not reach the server. Keys were not changed.")
    }

    val body = response.body()
    if (!response.isSuccessful || body == null) {
        return ImageKeyOutcome(null, null, "Server error (${response.code()}). Keys were not changed.")
    }

    fun persist(entered: String, result: ImageKeyResult?, save: (String) -> Unit, clear: () -> Unit): Pair<Boolean, String> {
        val ok = result?.valid == true
        if (ok) save(entered) else clear()
        val fallbackMessage = if (entered.isEmpty()) "Using the built-in key"
                              else "Not saved — using the built-in key instead"
        return ok to (if (ok) (result?.status ?: "Verified") else (result?.status ?: fallbackMessage))
    }

    val pixabayVerdict = persist(
        pixabayKey, body.pixabay, secureStorage::savePixabayKey, secureStorage::clearPixabayKey
    )
    val pexelsVerdict = persist(
        pexelsKey, body.pexels, secureStorage::savePexelsKey, secureStorage::clearPexelsKey
    )

    ApiClient.userPixabayKey = secureStorage.getPixabayKey()
    ApiClient.userPexelsKey = secureStorage.getPexelsKey()

    return ImageKeyOutcome(pixabayVerdict, pexelsVerdict, null)
}
