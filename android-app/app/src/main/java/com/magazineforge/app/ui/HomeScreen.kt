package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.magazineforge.app.ui.theme.LocalThemeTokens
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    viewModel: EditorViewModel,
    onMagazineSelected: (String) -> Unit,
    onContinueEditing: () -> Unit,
    onViewLibrary: () -> Unit
) {
    val tokens = LocalThemeTokens.current
    val context = LocalContext.current
    
    var recentFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val dir = File(context.filesDir, "magazines")
        if (dir.exists()) {
            val files = withContext(Dispatchers.IO) {
                dir.listFiles { _, name -> name.endsWith(".pdf") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.take(3)
            }
            if (files != null) {
                recentFiles = files
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
            .padding(horizontal = 24.dp)
    ) {
        // Greeting
        Text(
            text = "Good morning, Maker \uD83D\uDC4B",
            fontFamily = tokens.wordmarkFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = tokens.textPrimary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Entry tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Full AI Mode Tile
            EntryTile(
                modifier = Modifier.weight(1f),
                title = "Full AI Mode",
                description = "End-to-end generation",
                icon = Icons.Default.AutoAwesome,
                backgroundColor = tokens.primaryAccent.copy(alpha = 0.1f),
                iconColor = tokens.primaryAccent,
                onClick = onContinueEditing
            )
            // Assisted Mode Tile
            EntryTile(
                modifier = Modifier.weight(1f),
                title = "Assisted Mode",
                description = "Step-by-step guidance",
                icon = Icons.Default.EditNote,
                backgroundColor = tokens.secondaryAccent.copy(alpha = 0.1f),
                iconColor = tokens.secondaryAccent,
                onClick = onContinueEditing
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Your Library
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = tokens.textPrimary
            )
            Text(
                text = "View all",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.primaryAccent,
                modifier = Modifier.clickable(onClick = onViewLibrary)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (recentFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(tokens.surface, RoundedCornerShape(tokens.cornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent magazines.", color = tokens.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recentFiles) { file ->
                    RecentIssueCard(file, onMagazineSelected)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Continue Editing
        Text(
            text = "Continue Editing",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = tokens.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            shape = RoundedCornerShape(tokens.cornerRadius),
            colors = CardDefaults.cardColors(containerColor = tokens.surface),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onContinueEditing)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Draft: Summer Travel Guide",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = tokens.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "3 of 5 articles completed",
                    fontSize = 14.sp,
                    color = tokens.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = 0.6f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = tokens.primaryAccent,
                    trackColor = tokens.primaryAccent.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun EntryTile(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val tokens = LocalThemeTokens.current
    Card(
        shape = RoundedCornerShape(tokens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = tokens.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = tokens.textSecondary,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = tokens.textPrimary,
                modifier = Modifier.size(16.dp).align(Alignment.End)
            )
        }
    }
}

@Composable
fun RecentIssueCard(file: File, onClick: (String) -> Unit) {
    val tokens = LocalThemeTokens.current
    val rawName = file.nameWithoutExtension
    val coverFilename = "${rawName}_cover.jpg"
    val coverFile = File(file.parentFile, coverFilename)
    val title = rawName.substringAfterLast("_").replace("-", " ")
    
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick(file.absolutePath) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(tokens.cornerRadius.value * 0.5f))
                .background(tokens.surface)
        ) {
            if (coverFile.exists()) {
                CoverArtImage(
                    model = coverFile,
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Status Chip
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(tokens.surface.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Finished", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = tokens.textPrimary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title.capitalize(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = tokens.textPrimary,
            maxLines = 1
        )
        Text(
            text = "Edited recently",
            fontSize = 12.sp,
            color = tokens.textSecondary
        )
    }
}
