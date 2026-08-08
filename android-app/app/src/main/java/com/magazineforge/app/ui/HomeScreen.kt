package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

/**
 * The subset of run_snapshot.json the Continue Editing card needs.
 *
 * EditorViewModel writes that file on every poll as Gson().toJson of a
 * GenerationRunStatus. It cannot be read back with plain Gson: the schema holds
 * a List<PageSchema>, and PageSchema is a sealed class whose deserializer lives
 * on ApiClient's Retrofit Gson instance, not the default one. Reading the few
 * scalars we need with org.json sidesteps that entirely and cannot throw
 * anything but the JSONException we already catch.
 */
private data class DraftSnapshot(
    val runId: String,
    val title: String,
    val completedSections: Int,
    val totalSections: Int,
    val status: String
)

private fun readDraftSnapshot(filesDir: File): DraftSnapshot? {
    return try {
        val file = File(filesDir, "run_snapshot.json")
        if (!file.exists() || file.length() == 0L) return null
        val root = org.json.JSONObject(file.readText())

        val runId = root.optString("runId").orEmpty()
        val status = root.optString("status").orEmpty()
        val total = root.optInt("totalSections", 0)
        val completed = root.optInt("completedSections", 0)
        val title = root.optJSONObject("schema")
            ?.optJSONObject("cover")
            ?.optString("main_title")
            .orEmpty()
            .trim()

        // A cancelled run has nothing left to continue, and anything missing a
        // run id, a title or a section count would render as partial data.
        if (runId.isBlank() || title.isBlank() || total <= 0) return null
        if (status.equals("CANCELLED", ignoreCase = true)) return null

        DraftSnapshot(
            runId = runId,
            title = title,
            completedSections = completed.coerceIn(0, total),
            totalSections = total,
            status = status
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
fun HomeScreen(
    viewModel: EditorViewModel,
    onMagazineSelected: (String) -> Unit,
    onContinueEditing: () -> Unit,
    onFullAiModeClicked: () -> Unit,
    onAssistedModeClicked: () -> Unit,
    onViewLibrary: () -> Unit,
    onResumeRun: ((String) -> Unit)? = null
) {
    val tokens = LocalThemeTokens.current
    val context = LocalContext.current

    var recentFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var draft by remember { mutableStateOf<DraftSnapshot?>(null) }

    LaunchedEffect(Unit) {
        draft = withContext(Dispatchers.IO) { readDraftSnapshot(context.filesDir) }
    }

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
        val context = androidx.compose.ui.platform.LocalContext.current
        val secureStorage = remember { com.magazineforge.app.utils.SecureStorage(context) }
        val userName = secureStorage.getUserName()

        val greeting = remember {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                in 17..21 -> "Good Evening"
                else -> "Good Night"
            }
        }
        val greetingEmoji = remember {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..11 -> "\uD83C\uDF1E"  // sunrise
                in 12..16 -> "\u2600\uFE0F"  // sun
                in 17..21 -> "\uD83C\uDF05"  // sunset
                else -> "\uD83C\uDF19"        // crescent moon
            }
        }
        Text(
            text = "$greeting, $userName $greetingEmoji",
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
                onClick = onFullAiModeClicked
            )
            // Assisted Mode Tile
            EntryTile(
                modifier = Modifier.weight(1f),
                title = "Assisted Mode",
                description = "Step-by-step guidance",
                icon = Icons.Default.EditNote,
                backgroundColor = tokens.secondaryAccent.copy(alpha = 0.1f),
                iconColor = tokens.secondaryAccent,
                onClick = onAssistedModeClicked
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

        // Continue Editing — driven entirely by run_snapshot.json. The whole
        // section (heading included) is hidden when there is no parseable
        // snapshot, so no placeholder or half-filled card is ever shown.
        val currentDraft = draft
        if (currentDraft != null) {
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
                modifier = Modifier.fillMaxWidth().clickable {
                    val resume = onResumeRun
                    if (resume != null) resume(currentDraft.runId) else onContinueEditing()
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentDraft.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = tokens.textPrimary,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentDraft.completedSections} of ${currentDraft.totalSections} articles completed",
                        fontSize = 14.sp,
                        color = tokens.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = currentDraft.completedSections.toFloat() / currentDraft.totalSections.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = tokens.primaryAccent,
                        trackColor = tokens.primaryAccent.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Run ${currentDraft.runId.take(8)} · ${currentDraft.status}",
                        fontSize = 11.sp,
                        color = tokens.textSecondary
                    )
                }
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
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Start", tint = tokens.textPrimary, modifier = Modifier.size(16.dp).align(Alignment.End))
        }
    }
}

@Composable
fun RecentIssueCard(file: File, onClick: (String) -> Unit) {
    val tokens = LocalThemeTokens.current
    val rawName = file.nameWithoutExtension
    val coverFilename = "${rawName}_cover.jpg"
    val coverFile = File(file.parentFile, coverFilename)
    // Files are magazine_<epochMillis>_<safeTopic>.pdf, and saveFilesToDisk
    // builds safeTopic by replacing every non-alphanumeric char with "_" — so
    // spaces inside the topic are underscores too, indistinguishable from the
    // structural ones. The topic is therefore everything from segment 2 on.
    // substringAfterLast("_") took only the final word, titling "Space
    // Exploration" as "Exploration" here while My Library showed it in full.
    val titleParts = rawName.split("_")
    val title = if (titleParts.size > 2) {
        titleParts.subList(2, titleParts.size).joinToString(" ")
    } else {
        rawName.replace("_", " ")
    }.trim().replace(Regex("\\s+"), " ")
    
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
            text = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
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
