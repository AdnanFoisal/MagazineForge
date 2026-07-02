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
import com.magazineforge.app.ui.theme.EditorialGold
import com.magazineforge.app.ui.theme.GhostWhite
import com.magazineforge.app.ui.theme.LuxeTypography
import com.magazineforge.app.ui.theme.PitchBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizerBottomSheet(
    page: PageBlock,
    onDismiss: () -> Unit,
    onSave: (PageBlock) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // 33 Options as requested
    val tones = listOf("Professional", "Casual", "Dramatic", "Humorous", "Academic", "Poetic", "Journalistic")
    val palettes = listOf("Dark Mode", "Pastel", "Vibrant", "Monochrome", "Neon", "Sepia", "Earth Tones")
    val imageStyles = listOf("Photorealistic", "Minimalist", "Abstract", "Vintage", "Cinematic", "Watercolor", "Cyberpunk")
    val layoutDensities = listOf("Image-heavy", "Text-heavy", "Balanced", "Magazine-style", "Grid", "Poster-style")
    val audiences = listOf("Teens", "Professionals", "General", "Academics", "Kids", "Enthusiasts")

    var selectedTone by remember { mutableStateOf(page.tone) }
    var selectedPalette by remember { mutableStateOf(page.colorPalette) }
    var selectedImageStyle by remember { mutableStateOf(page.imageStyle) }
    var selectedDensity by remember { mutableStateOf(page.layoutDensity) }
    var selectedAudience by remember { mutableStateOf(page.targetAudience) }

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
                text = "AI Page Customization",
                style = LuxeTypography.headlineSmall.copy(color = EditorialGold, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            CustomizerCategory(title = "Writing Tone", options = tones, selected = selectedTone, onSelect = { selectedTone = it })
            CustomizerCategory(title = "Color Palette", options = palettes, selected = selectedPalette, onSelect = { selectedPalette = it })
            CustomizerCategory(title = "Image Style", options = imageStyles, selected = selectedImageStyle, onSelect = { selectedImageStyle = it })
            CustomizerCategory(title = "Layout Density", options = layoutDensities, selected = selectedDensity, onSelect = { selectedDensity = it })
            CustomizerCategory(title = "Target Audience", options = audiences, selected = selectedAudience, onSelect = { selectedAudience = it })

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        page.copy(
                            tone = selectedTone,
                            colorPalette = selectedPalette,
                            imageStyle = selectedImageStyle,
                            layoutDensity = selectedDensity,
                            targetAudience = selectedAudience
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EditorialGold, contentColor = PitchBlack)
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
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = LuxeTypography.titleSmall.copy(color = GhostWhite),
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
                        selectedContainerColor = EditorialGold,
                        selectedLabelColor = PitchBlack,
                        containerColor = PitchBlack,
                        labelColor = GhostWhite
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) EditorialGold else GhostWhite.copy(alpha = 0.3f),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}
