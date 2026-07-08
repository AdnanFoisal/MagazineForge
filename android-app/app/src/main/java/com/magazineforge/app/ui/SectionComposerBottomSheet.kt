package com.magazineforge.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magazineforge.app.ui.theme.EditorialGold
import com.magazineforge.app.ui.theme.GhostWhite
import com.magazineforge.app.ui.theme.LuxeTypography
import com.magazineforge.app.ui.theme.PitchBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionComposerBottomSheet(
    config: SectionComposerConfig,
    onDismiss: () -> Unit,
    onSave: (SectionComposerConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var enableMasthead by remember { mutableStateOf(config.enableMasthead) }
    var mastheadAngle by remember { mutableStateOf(config.mastheadAngle) }
    
    var enableSidebar by remember { mutableStateOf(config.enableSidebar) }
    var sidebarTopic by remember { mutableStateOf(config.sidebarTopic) }
    
    var enablePullQuote by remember { mutableStateOf(config.enablePullQuote) }
    var enableBackCover by remember { mutableStateOf(config.enableBackCover) }
    var enableTocTeasers by remember { mutableStateOf(config.enableTocTeasers) }
    var enableByline by remember { mutableStateOf(config.enableByline) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PitchBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Section Composer",
                style = LuxeTypography.headlineSmall.copy(color = EditorialGold, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SectionToggle(
                label = "Masthead",
                checked = enableMasthead,
                onCheckedChange = { enableMasthead = it },
                textFieldValue = mastheadAngle,
                onTextFieldChange = { mastheadAngle = it },
                textFieldPlaceholder = "Angle (e.g. Founder's Note)"
            )
            
            SectionToggle(
                label = "Sidebar (Per Article)",
                checked = enableSidebar,
                onCheckedChange = { enableSidebar = it },
                textFieldValue = sidebarTopic,
                onTextFieldChange = { sidebarTopic = it },
                textFieldPlaceholder = "Topic (e.g. Key Takeaways)"
            )
            
            SectionToggle(label = "Pull Quotes", checked = enablePullQuote, onCheckedChange = { enablePullQuote = it })
            SectionToggle(label = "Back Cover", checked = enableBackCover, onCheckedChange = { enableBackCover = it })
            SectionToggle(label = "TOC Teasers", checked = enableTocTeasers, onCheckedChange = { enableTocTeasers = it })
            SectionToggle(label = "Article Bylines", checked = enableByline, onCheckedChange = { enableByline = it })

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        SectionComposerConfig(
                            enableMasthead = enableMasthead,
                            mastheadAngle = mastheadAngle,
                            enableSidebar = enableSidebar,
                            sidebarTopic = sidebarTopic,
                            enablePullQuote = enablePullQuote,
                            enableBackCover = enableBackCover,
                            enableTocTeasers = enableTocTeasers,
                            enableByline = enableByline
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EditorialGold, contentColor = PitchBlack)
            ) {
                Text("Apply Structure", style = LuxeTypography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun SectionToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textFieldValue: String? = null,
    onTextFieldChange: ((String) -> Unit)? = null,
    textFieldPlaceholder: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = LuxeTypography.titleMedium.copy(color = GhostWhite))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PitchBlack,
                    checkedTrackColor = EditorialGold,
                    uncheckedThumbColor = GhostWhite,
                    uncheckedTrackColor = PitchBlack
                )
            )
        }
        if (checked && textFieldValue != null && onTextFieldChange != null && textFieldPlaceholder != null) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = onTextFieldChange,
                placeholder = { Text(textFieldPlaceholder) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EditorialGold,
                    unfocusedTextColor = GhostWhite,
                    focusedTextColor = GhostWhite
                ),
                singleLine = true
            )
        }
    }
}
