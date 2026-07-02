package com.magazineforge.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MagazinePdfItem(
    val file: File,
    val name: String,
    val formattedDate: String,
    val formattedSize: String
)

@Composable
fun MyMagazinesScreen(
    onBack: () -> Unit,
    onMagazineSelected: (File) -> Unit
) {
    val context = LocalContext.current
    
    // Retrieve magazines from context.filesDir/magazines
    val magazines = remember {
        val dir = File(context.filesDir, "magazines")
        val list = mutableListOf<MagazinePdfItem>()
        if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles { file -> file.isFile && file.extension == "pdf" }
            if (files != null) {
                // Sort by modification time descending (newest first)
                files.sortByDescending { it.lastModified() }
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                for (file in files) {
                    // Extract printable name from filename (e.g. magazine_[timestamp]_[topic].pdf)
                    val rawName = file.nameWithoutExtension
                    val parts = rawName.split("_")
                    val displayName = if (parts.size >= 3) {
                        parts.subList(2, parts.size).joinToString(" ")
                    } else {
                        rawName
                    }
                    
                    val sizeKb = file.length() / 1024
                    val sizeString = if (sizeKb > 1024) {
                        String.format(Locale.getDefault(), "%.1f MB", sizeKb / 1024.0)
                    } else {
                        "$sizeKb KB"
                    }
                    
                    list.add(
                        MagazinePdfItem(
                            file = file,
                            name = displayName,
                            formattedDate = dateFormat.format(Date(file.lastModified())),
                            formattedSize = sizeString
                        )
                    )
                }
            }
        }
        list
    }

    val obsidian = Color(0xFF0F0F10)
    val darkSurface = Color(0xFF18181B)
    val gold = Color(0xFFC5A059)
    val copper = Color(0xFFB87333)
    val ivory = Color(0xFFF5F5F7)
    val mutedGray = Color(0xFFA1A1AA)
    val borderCol = Color(0xFF2E2A24)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(obsidian)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ivory)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Library",
                style = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = ivory
                )
            )
        }

        if (magazines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Your library is empty.",
                        color = mutedGray,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Craft a magazine to see it here.",
                        color = gold,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(magazines) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f)
                            .clickable { onMagazineSelected(item.file) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderCol),
                        colors = CardDefaults.cardColors(containerColor = darkSurface)
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
                                    .weight(1f)
                                    .background(obsidian, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "PDF",
                                        color = copper,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.formattedSize,
                                        color = mutedGray,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.name,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ivory,
                                maxLines = 1
                            )
                            Text(
                                text = item.formattedDate,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = mutedGray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
