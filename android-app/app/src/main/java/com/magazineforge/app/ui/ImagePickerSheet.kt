package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.magazineforge.app.models.PreviewImageItem
import com.magazineforge.app.ui.theme.LuxeTypography

/**
 * Modal bottom sheet / dialog that lets the user pick a stock photo for a magazine slot.
 *
 * Flow:
 *   1. User taps an image slot in CoAuthorScreen.
 *   2. CoAuthor calls onOpenPicker(slotId, currentQuery, onPicked).
 *   3. ImagePickerSheet opens, pre-fills the query box, fires previewImages().
 *   4. Backend returns up to 12 candidate URLs (Pixabay first, Pexels fills).
 *   5. User taps one -> onPicked(url) -> sheet closes -> schema updated -> compile uses new URL.
 *
 * The picker does NOT modify the schema directly. It calls onPicked(url) and the
 * caller (CoAuthorScreen) writes the URL into the right slot. This keeps the
 * picker stateless w.r.t. schema shape, so it works for cover, back_cover,
 * article images, and ad images alike.
 *
 * Non-intrusive: existing upload flow stays. The picker is one more option
 * alongside "Upload" and "Use Drive Link" on each image slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerSheet(
    initialQuery: String,
    onSearch: (String, (List<PreviewImageItem>, String?) -> Unit) -> Unit,
    onPicked: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    var query by remember { mutableStateOf(initialQuery) }
    var images by remember { mutableStateOf<List<PreviewImageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fire the initial search when the sheet opens, if there's a query.
    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) {
            isLoading = true
            errorMessage = null
            onSearch(initialQuery) { results, err ->
                isLoading = false
                images = results
                errorMessage = err
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = tokens.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pick an image",
                        style = LuxeTypography.headlineSmall,
                        color = tokens.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = tokens.textPrimary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Search bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search stock photos...") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (query.isNotBlank()) {
                                    isLoading = true
                                    errorMessage = null
                                    images = emptyList()
                                    onSearch(query) { results, err ->
                                        isLoading = false
                                        images = results
                                        errorMessage = err
                                    }
                                }
                            },
                            enabled = query.isNotBlank() && !isLoading
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (query.isNotBlank() && !isLoading) tokens.primaryAccent
                                       else tokens.textSecondary
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        focusedBorderColor = tokens.primaryAccent,
                        unfocusedBorderColor = tokens.textSecondary.copy(alpha = 0.3f),
                        cursorColor = tokens.primaryAccent
                    )
                )
                Spacer(Modifier.height(12.dp))

                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = tokens.primaryAccent)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Searching Pixabay & Pexels...",
                                style = LuxeTypography.bodyMedium,
                                color = tokens.textSecondary
                            )
                        }
                    }
                } else if (errorMessage != null && images.isEmpty()) {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                "Couldn't load images",
                                style = LuxeTypography.headlineSmall,
                                color = tokens.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                errorMessage ?: "Unknown error",
                                style = LuxeTypography.bodyMedium,
                                color = tokens.textSecondary
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (query.isNotBlank()) {
                                        isLoading = true
                                        errorMessage = null
                                        onSearch(query) { results, err ->
                                            isLoading = false
                                            images = results
                                            errorMessage = err
                                        }
                                    }
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (images.isEmpty()) {
                    // Empty state — no error, just no results
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                "No images found",
                                style = LuxeTypography.headlineSmall,
                                color = tokens.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Try a different query. Concrete nouns work best\n(e.g. 'red Ferrari', 'mountain lake', 'coffee cup').",
                                style = LuxeTypography.bodyMedium,
                                color = tokens.textSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    // Image grid
                    Text(
                        "${images.size} images • Tap to select",
                        style = LuxeTypography.labelSmall,
                        color = tokens.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(images, key = { it.url }) { item ->
                            ImageCard(
                                item = item,
                                onClick = {
                                    onPicked(item.url)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCard(
    item: PreviewImageItem,
    onClick: () -> Unit
) {
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f)
                .clip(RoundedCornerShape(6.dp))
                .background(tokens.textSecondary.copy(alpha = 0.1f))
        ) {
            AsyncImage(
                model = item.previewUrl,
                contentDescription = item.tags.ifBlank { "Stock photo" },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Source badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                color = tokens.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = item.source,
                    fontSize = 9.sp,
                    color = tokens.textPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // Tags truncated to 1 line for compactness
        if (item.tags.isNotBlank()) {
            Text(
                text = item.tags,
                fontSize = 10.sp,
                color = tokens.textSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
