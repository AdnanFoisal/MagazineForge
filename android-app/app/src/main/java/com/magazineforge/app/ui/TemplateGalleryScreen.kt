package com.magazineforge.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.magazineforge.app.ui.theme.*
import org.json.JSONArray

data class TemplateModel(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val thumbnailUrl: String,
    val texTemplate: String,
    val samplePdfUrl: String
)

fun loadTemplates(context: Context): List<TemplateModel> {
    val jsonString = context.assets.open("template_config.json").bufferedReader().use { it.readText() }
    val jsonArray = JSONArray(jsonString)
    val templates = mutableListOf<TemplateModel>()
    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        templates.add(
            TemplateModel(
                id = obj.getString("id"),
                name = obj.getString("name"),
                category = obj.getString("category"),
                description = obj.getString("description"),
                thumbnailUrl = obj.getString("thumbnailUrl"),
                texTemplate = obj.getString("texTemplate"),
                samplePdfUrl = obj.optString("samplePdfUrl", "")
            )
        )
    }
    return templates
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateGalleryScreen(
    onTemplateSelected: (String) -> Unit,
    onLibraryClicked: () -> Unit
) {
    val context = LocalContext.current
    val templates = remember { loadTemplates(context) }
    
    // We categorize them but to match the "Newsstand" masonry look, we can show all or provide simple filter chips.
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All") + templates.map { it.category }.distinct()
    val filteredTemplates = if (selectedCategory == "All") templates else templates.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MagazineForge", style = LuxeTypography.headlineMedium, color = EditorialGold) },
                navigationIcon = {
                    IconButton(onClick = onLibraryClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = EditorialGold)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = EditorialGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack)
            )
        },
        containerColor = PitchBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Chips
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = PitchBlack,
                contentColor = EditorialGold,
                edgePadding = 16.dp,
                divider = {}
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = { 
                            Text(
                                category.uppercase(), 
                                style = LuxeTypography.labelMedium.copy(color = if (selectedCategory == category) EditorialGold else GhostWhite.copy(alpha=0.6f))
                            ) 
                        }
                    )
                }
            }

            // Masonry Grid
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalItemSpacing = 16.dp
            ) {
                items(filteredTemplates) { template ->
                    TemplateMasonryCard(template, onClick = { onTemplateSelected(template.texTemplate) })
                }
            }
        }
    }
}

@Composable
fun TemplateMasonryCard(template: TemplateModel, onClick: () -> Unit) {
    // Generate a somewhat random aspect ratio based on ID length to simulate masonry
    val aspectRatio = if (template.id.length % 2 == 0) 0.75f else 1.2f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, SurfaceContainerHigh, RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(template.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = template.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 100f
                    )
                )
        )
        
        // Text Info at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = template.name,
                style = LuxeTypography.titleMedium.copy(color = GhostWhite, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "@${template.id.replace("_", "")}",
                style = LuxeTypography.labelSmall.copy(color = GhostWhite.copy(alpha = 0.7f), letterSpacing = 1.sp)
            )
        }
    }
}
