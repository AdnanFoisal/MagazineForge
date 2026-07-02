package com.magazineforge.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray

data class TemplateModel(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val thumbnailUrl: String,
    val texTemplate: String
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
                texTemplate = obj.getString("texTemplate")
            )
        )
    }
    return templates
}

@Composable
fun TemplateGalleryScreen(
    onTemplateSelected: (String) -> Unit,
    onLibraryClicked: () -> Unit
) {
    val context = LocalContext.current
    val templates = remember { loadTemplates(context) }
    val categories = templates.map { it.category }.distinct()
    
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTemplates = templates.filter { 
        (categories.isEmpty() || it.category == categories[selectedCategoryIndex]) && 
        (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
    }

    Surface(
        color = Color(0xFF0F0F10),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = Color(0xFF18181B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Template Gallery",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = Color(0xFFF5F5F7)
                        )
                        IconButton(onClick = onLibraryClicked) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Library",
                                tint = Color(0xFFC5A059)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search templates...", color = Color(0xFFA1A1AA)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFC5A059)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC5A059),
                            unfocusedBorderColor = Color(0xFF2E2A24),
                            focusedTextColor = Color(0xFFF5F5F7),
                            unfocusedTextColor = Color(0xFFF5F5F7),
                            focusedContainerColor = Color(0xFF18181B),
                            unfocusedContainerColor = Color(0xFF18181B)
                        )
                    )
                }
            }
            
            if (categories.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    edgePadding = 8.dp,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFC5A059),
                    indicator = { tabPositions ->
                        if (selectedCategoryIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                                color = Color(0xFFC5A059)
                            )
                        }
                    }
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Text(
                                    text = category,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategoryIndex == index) Color(0xFFC5A059) else Color(0xFFA1A1AA)
                                )
                            }
                        )
                    }
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTemplates) { template ->
                    TemplateCard(template = template, onClick = { onTemplateSelected(template.texTemplate) })
                }
            }
        }
    }
}

@Composable
fun TemplateCard(template: TemplateModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2A24)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF0F0F10),
                    border = BorderStroke(1.5.dp, Color(0xFFC5A059)),
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = template.category.uppercase(),
                            color = Color(0xFFC5A059),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color(0xFFF5F5F7),
                maxLines = 1
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFFA1A1AA),
                maxLines = 2
            )
        }
    }
}
