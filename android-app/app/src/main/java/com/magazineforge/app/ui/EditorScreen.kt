package com.magazineforge.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.ui.theme.LuxeTypography
import com.magazineforge.app.ui.theme.PitchBlack
import com.magazineforge.app.ui.theme.DarkSurface
import com.magazineforge.app.ui.theme.EditorialGold
import com.magazineforge.app.ui.theme.BorderDark
import com.magazineforge.app.ui.theme.GhostWhite

data class PageBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String = "article",
    val topic: String = "",
    val imageUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    templateVariant: String,
    isCompileLoading: Boolean,
    onCompileClicked: (String, List<PageBlock>) -> Unit,
    onBack: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val obsidian = PitchBlack
    val darkSurface = DarkSurface
    val gold = EditorialGold
    val borderCol = BorderDark
    val ivory = GhostWhite

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("MagazineForge", color = gold, style = LuxeTypography.headlineMedium) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = gold)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Add functionality */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = obsidian)
            )
        },
        containerColor = obsidian
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top 60%: Massive prompt input field
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { 
                        Text("What are we publishing today?", color = ivory.copy(alpha = 0.4f), style = LuxeTypography.headlineMedium) 
                    },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = LuxeTypography.headlineMedium.copy(color = ivory),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = gold
                    )
                )
            }

            // Bottom 40%: Anchored Tool Panel
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .background(darkSurface)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Tools Icons
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { ToolButton("image", "ATTACH IMAGE") }
                    item { ToolButton("view_quilt", "TEMPLATE") }
                    item { ToolButton("text_fields", "TYPOGRAPHY") }
                    item { ToolButton("palette", "PALETTE") }
                }

                // Row 2: Typography Cards
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item { TypographyCard("Grotesk", true) }
                    item { TypographyCard("Playfair", false) }
                    item { TypographyCard("Mono", false) }
                    item { TypographyCard("Inter", false) }
                }

                // Row 3: Generate Button
                Button(
                    onClick = {
                        val dummyPages = listOf(
                            PageBlock(type = "cover", topic = prompt),
                            PageBlock(type = "toc"),
                            PageBlock(type = "article", topic = "Main Article")
                        )
                        onCompileClicked(prompt, dummyPages)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = obsidian
                    ),
                    enabled = prompt.isNotBlank() && !isCompileLoading
                ) {
                    if (isCompileLoading) {
                        CircularProgressIndicator(color = obsidian, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Generate Issue", style = LuxeTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(iconName: String, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, BorderDark.copy(alpha = 0.3f)),
        color = PitchBlack,
        modifier = Modifier.clickable { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = LuxeTypography.labelSmall.copy(color = GhostWhite))
        }
    }
}

@Composable
fun TypographyCard(name: String, isSelected: Boolean) {
    val borderColor = if (isSelected) EditorialGold.copy(alpha = 0.5f) else BorderDark.copy(alpha = 0.3f)
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        color = PitchBlack,
        modifier = Modifier
            .size(96.dp)
            .clickable { }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Aa", style = LuxeTypography.headlineMedium.copy(color = GhostWhite))
            Text(name.uppercase(), style = LuxeTypography.labelSmall.copy(color = GhostWhite.copy(alpha = 0.6f), fontSize = 10.sp))
        }
    }
}
