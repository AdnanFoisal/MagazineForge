package com.magazineforge.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magazineforge.app.ui.theme.LuxeTypography
import com.magazineforge.app.ui.theme.LocalThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizerBottomSheet(
    page: PageBlock,
    onDismiss: () -> Unit,
    onSave: (PageBlock) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tokens = LocalThemeTokens.current

    val tones = listOf("Professional", "Casual", "Playful", "Dramatic/Literary", "Academic", "Inspirational", "Witty")
    val layoutDensities = listOf("Image-heavy", "Balanced", "Text-heavy")

    var selectedTone by remember { mutableStateOf(page.tone) }
    var selectedDensity by remember { mutableStateOf(page.layoutDensity) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.editorBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "AI Page Customization",
                style = LuxeTypography.headlineSmall.copy(color = tokens.primaryAccent, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            CustomizerCategory(title = "Writing Tone", options = tones, selected = selectedTone, onSelect = { selectedTone = it })
            CustomizerCategory(title = "Layout Density", options = layoutDensities, selected = selectedDensity, onSelect = { selectedDensity = it })

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        page.copy(
                            tone = selectedTone,
                            layoutDensity = selectedDensity
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.editorBackground)
            ) {
                Text("Apply Customizations", style = LuxeTypography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizerCategory(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val tokens = LocalThemeTokens.current
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = LuxeTypography.titleSmall.copy(color = tokens.textPrimary),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tokens.primaryAccent,
                        selectedLabelColor = tokens.editorBackground,
                        containerColor = tokens.editorBackground,
                        labelColor = tokens.textPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) tokens.primaryAccent else tokens.textSecondary.copy(alpha = 0.3f),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}
