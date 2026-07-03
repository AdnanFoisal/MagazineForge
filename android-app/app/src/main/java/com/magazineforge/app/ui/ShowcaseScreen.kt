package com.magazineforge.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.magazineforge.app.models.ShowcaseItem
import com.magazineforge.app.network.ShowcaseRepository
import com.magazineforge.app.ui.theme.DarkSurface
import com.magazineforge.app.ui.theme.EditorialGold
import com.magazineforge.app.ui.theme.GhostWhite
import com.magazineforge.app.ui.theme.AshGrey
import com.magazineforge.app.ui.theme.LuxeTypography
import kotlinx.coroutines.launch

@Composable
fun ShowcaseScreen(
    viewModel: EditorViewModel,
    onMagazineSelected: (String) -> Unit
) {
    val repository = remember { ShowcaseRepository() }
    val context = LocalContext.current
    var showcaseItems by remember { mutableStateOf<List<ShowcaseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Luxe Noir", "Brutalist", "Minimalist", "Editorial")

    LaunchedEffect(Unit) {
        isLoading = true
        showcaseItems = repository.getShowcaseItems()
        isLoading = false
    }

    val filteredItems = if (selectedFilter == "All") {
        showcaseItems
    } else {
        showcaseItems.filter { it.templateVariant.contains(selectedFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Community Showcase",
            style = LuxeTypography.headlineMedium.copy(color = GhostWhite),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        
        // Filter Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, style = LuxeTypography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EditorialGold,
                        selectedLabelColor = DarkSurface,
                        containerColor = DarkSurface,
                        labelColor = AshGrey
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = AshGrey.copy(alpha = 0.5f),
                        selectedBorderColor = EditorialGold,
                        enabled = true,
                        selected = selectedFilter == filter
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EditorialGold)
            }
        } else if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No magazines found.",
                    style = LuxeTypography.bodyLarge.copy(color = AshGrey)
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalItemSpacing = 16.dp,
                contentPadding = PaddingValues(bottom = 120.dp) // padding for BottomNavigationBar
            ) {
                items(filteredItems) { item ->
                    ShowcaseCard(
                        item = item,
                        onClick = {
                            if (item.latexCode.isNotEmpty()) {
                                viewModel.compileRaw(context, item.latexCode, item.title, item.templateVariant, true)
                            } else if (item.pdfUrl.isNotEmpty()) {
                                onMagazineSelected(item.pdfUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ShowcaseCard(item: ShowcaseItem, onClick: () -> Unit) {
    // Generate an aspect ratio based on title length to create masonry effect
    val aspectRatio = if (item.title.length % 2 == 0) 0.75f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1A1A1A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(4.dp))
                .padding(bottom = 12.dp)
            ) {
                if (item.coverImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(item.coverImageUrl)
                            .addHeader("Authorization", "Bearer ${com.magazineforge.app.BuildConfig.HF_TOKEN}")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val gradientColor1 = remember(item.templateVariant) { 
                        androidx.compose.ui.graphics.Color((0xFF000000..0xFFFFFFFF).random()) 
                    }
                    val gradientColor2 = remember(item.templateVariant) { 
                        androidx.compose.ui.graphics.Color((0xFF000000..0xFFFFFFFF).random()) 
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(gradientColor1, gradientColor2)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.templateVariant.take(2).uppercase(),
                            style = LuxeTypography.displayMedium.copy(
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                            )
                        )
                    }
                }
            }
            Text(
                text = item.title,
                style = LuxeTypography.headlineSmall.copy(color = GhostWhite, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "@${item.templateVariant.replace("_", "")}".uppercase(),
                style = LuxeTypography.labelSmall.copy(
                    color = AshGrey, 
                    letterSpacing = 1.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            )
        }
    }
}
