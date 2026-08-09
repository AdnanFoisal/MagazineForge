package com.magazineforge.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magazineforge.app.models.ContractSchema
import com.magazineforge.app.ui.theme.LocalThemeTokens
import com.magazineforge.app.ui.theme.LuxeTypography

/**
 * One of the three visual registers the contract can carry. `value` is the wire
 * value the backend understands (visual_register), so it must stay lowercase.
 */
private data class VisualRegisterOption(
    val value: String,
    val label: String,
    val description: String
)

private val VisualRegisterOptions = listOf(
    VisualRegisterOption("editorial", "Editorial", "Formal, long-form, cultural"),
    VisualRegisterOption("modern", "Modern", "Bold, consumer, lifestyle"),
    VisualRegisterOption("technical", "Technical", "Dense, data-heavy, specialist")
)

/**
 * Maps a confirmed contract's visual register onto the cover template variant.
 * Anything unrecognised falls back to "a" rather than throwing — the register is
 * LLM-extracted and this runs on the main thread on the way into generation.
 */
fun templateVariantForRegister(visualRegister: String?): String = when (visualRegister?.lowercase()) {
    "modern" -> "b"
    "technical" -> "c"
    else -> "a"
}

/** Renders a list field as one item per line for editing. */
private fun List<String>?.toLines(): String =
    this.orEmpty().joinToString("\n")

/** Parses a multiline field back into a list: trimmed, blanks dropped. */
private fun String.toListItems(): List<String> =
    lines().map { it.trim() }.filter { it.isNotEmpty() }

/**
 * The Intent Gate. Sits between the prompt box and brief generation and shows
 * the user what the model understood, as editable fields, before a ten-minute
 * generation run commits to it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentCardScreen(
    contractState: ContractState,
    prompt: String = "",
    onConfirm: (ContractSchema) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val tokens = LocalThemeTokens.current
    val obsidian = tokens.editorBackground
    val darkSurface = tokens.surface
    val gold = tokens.primaryAccent
    val borderCol = tokens.secondaryAccent.copy(alpha = 0.3f)
    val ivory = tokens.editorText

    var subject by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var mustCoverText by remember { mutableStateOf("") }
    var avoidText by remember { mutableStateOf("") }
    var visualRegister by remember { mutableStateOf(VisualRegisterOptions.first().value) }

    // Seed the editable fields from whatever the extractor returned. Keyed on
    // contractState so a retry reseeds, while ordinary recomposition never
    // stomps on what the user has typed.
    //
    // Every read goes through the nullable `contract`: Gson builds these data
    // classes with Unsafe, so a field the backend omitted arrives null even
    // though Kotlin declares it non-null and gives it a default.
    LaunchedEffect(contractState) {
        if (contractState is ContractState.Success) {
            val contract = contractState.contract
            subject = contract?.subject ?: ""
            audience = contract?.audience ?: "general interest"
            language = contract?.language ?: "en"
            mustCoverText = contract?.mustCover.toLines()
            avoidText = contract?.avoid.toLines()
            val extractedRegister = (contract?.visualRegister ?: "").lowercase()
            visualRegister = VisualRegisterOptions
                .firstOrNull { it.value == extractedRegister }?.value
                ?: VisualRegisterOptions.first().value
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Check the Intent", color = gold, style = LuxeTypography.headlineMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = obsidian)
            )
        },
        containerColor = obsidian
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (contractState) {
                is ContractState.Loading, is ContractState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = gold, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Reading your prompt...",
                            style = LuxeTypography.bodyMedium.copy(color = ivory)
                        )
                    }
                }

                is ContractState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Couldn't read your prompt",
                            style = LuxeTypography.headlineSmall.copy(color = gold, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = contractState.message,
                            style = LuxeTypography.bodyMedium.copy(color = tokens.editorTextSecondary)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = obsidian)
                        ) {
                            Text("Retry", style = LuxeTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onBack) {
                            Text("Back to the prompt", color = tokens.editorTextSecondary)
                        }
                    }
                }

                is ContractState.Success -> {
                    // The backend hands back `subject` set to the raw prompt when
                    // parsing failed, which reads exactly like a clean extraction.
                    // A missing contract object means the same thing, so both go
                    // through the banner.
                    val extractionFailed = !contractState.extractionOk || contractState.contract == null

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        if (extractionFailed) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = tokens.surfaceOverlay),
                                border = BorderStroke(1.dp, gold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Extraction failed",
                                        tint = gold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "We couldn't read your prompt automatically",
                                            style = LuxeTypography.titleSmall.copy(
                                                color = tokens.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Nothing below was understood by the AI — please fill these fields in yourself before continuing.",
                                            style = LuxeTypography.bodySmall.copy(color = tokens.textSecondary)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Here's what we understood. Correct anything that's off — this is what the whole issue gets built from.",
                                style = LuxeTypography.bodySmall.copy(color = tokens.editorTextSecondary),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        if (prompt.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = darkSurface),
                                border = BorderStroke(1.dp, borderCol),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "YOUR PROMPT",
                                        style = LuxeTypography.labelSmall.copy(color = gold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        prompt,
                                        style = LuxeTypography.bodyMedium.copy(color = tokens.textPrimary)
                                    )
                                }
                            }
                        }

                        IntentField(
                            label = "Subject",
                            placeholder = "What this issue is about",
                            value = subject,
                            onValueChange = { subject = it }
                        )

                        IntentField(
                            label = "Audience",
                            placeholder = "Who it's written for",
                            value = audience,
                            onValueChange = { audience = it }
                        )

                        IntentField(
                            label = "Language",
                            placeholder = "en",
                            value = language,
                            onValueChange = { language = it }
                        )

                        IntentField(
                            label = "Must cover",
                            placeholder = "One per line",
                            value = mustCoverText,
                            onValueChange = { mustCoverText = it },
                            singleLine = false,
                            supportingText = "One item per line"
                        )

                        IntentField(
                            label = "Avoid",
                            placeholder = "One per line",
                            value = avoidText,
                            onValueChange = { avoidText = it },
                            singleLine = false,
                            supportingText = "One item per line"
                        )

                        Text(
                            "Visual Register",
                            style = LuxeTypography.titleSmall.copy(color = gold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VisualRegisterOptions.forEach { option ->
                                VisualRegisterRow(
                                    option = option,
                                    selected = visualRegister == option.value,
                                    onClick = { visualRegister = option.value }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                onConfirm(
                                    ContractSchema(
                                        subject = subject.trim(),
                                        audience = audience.trim().ifBlank { "general interest" },
                                        mustCover = mustCoverText.toListItems(),
                                        avoid = avoidText.toListItems(),
                                        language = language.trim().ifBlank { "en" },
                                        visualRegister = visualRegister
                                    )
                                )
                            },
                            enabled = subject.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = gold,
                                contentColor = obsidian,
                                disabledContainerColor = tokens.editorBorder,
                                disabledContentColor = tokens.editorTextSecondary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Looks good", style = LuxeTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to the prompt", color = tokens.editorTextSecondary)
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun IntentField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    supportingText: String? = null
) {
    val tokens = LocalThemeTokens.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        supportingText = supportingText?.let {
            { Text(it, style = LuxeTypography.labelSmall.copy(color = tokens.editorTextSecondary)) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tokens.primaryAccent,
            unfocusedBorderColor = tokens.editorBorder,
            focusedTextColor = tokens.editorText,
            unfocusedTextColor = tokens.editorText,
            focusedLabelColor = tokens.primaryAccent,
            unfocusedLabelColor = tokens.editorTextSecondary,
            focusedPlaceholderColor = tokens.editorTextSecondary,
            unfocusedPlaceholderColor = tokens.editorTextSecondary,
            cursorColor = tokens.primaryAccent
        )
    )
}

@Composable
private fun VisualRegisterRow(
    option: VisualRegisterOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalThemeTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) tokens.surfaceOverlay else tokens.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) tokens.primaryAccent else tokens.secondaryAccent.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = tokens.primaryAccent,
                unselectedColor = tokens.textSecondary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                option.label,
                style = LuxeTypography.titleSmall.copy(
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                option.description,
                style = LuxeTypography.bodySmall.copy(color = tokens.textSecondary),
                maxLines = 1
            )
        }
    }
}
