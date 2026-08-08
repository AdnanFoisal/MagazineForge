package com.magazineforge.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.ui.theme.LocalThemeTokens
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MagazinePdfItem(
    val file: File,
    val name: String,
    val formattedDate: String,
    val formattedSize: String,
    val coverFile: File?,
    val lastModified: Long,
    val sizeBytes: Long
)

enum class MagazineSortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME("Name A–Z"),
    LARGEST("Largest first")
}

private enum class RenameOutcome { SUCCESS, UNCHANGED, EMPTY_NAME, NAME_TAKEN, FAILED }

/**
 * Anything outside this set is dropped rather than escaped: the topic segment is
 * re-parsed straight out of the filename, so keeping it to letters, digits and
 * two separators means no rename can produce a path the reader cannot round-trip
 * (a "." in particular would confuse nameWithoutExtension).
 */
private val UNSAFE_NAME_CHARS = Regex("[^\\p{L}\\p{N} _-]")
private val SEPARATOR_RUN = Regex("[\\s_]+")

private const val MAX_TITLE_LENGTH = 60

/**
 * Flattens separators so search compares like with like: the display name shows
 * spaces where the filename carries underscores, and a user may type either.
 */
private fun toSearchKey(input: String): String =
    input.replace(SEPARATOR_RUN, " ").trim()

/** Locale-aware, case- and accent-insensitive ordering for the Name A-Z sort. */
private val nameCollator: java.text.Collator =
    java.text.Collator.getInstance(Locale.getDefault()).apply {
        strength = java.text.Collator.SECONDARY
    }

/** Inverse of the display transform below: spaces collapse back into "_". */
private fun toTopicSegment(input: String): String =
    input
        .replace(UNSAFE_NAME_CHARS, " ")
        .replace(SEPARATOR_RUN, "_")
        .trim('_', '-')

/**
 * Rewrites only the topic segments of `magazine_<timestamp>_<topic>.pdf`, so the
 * display-name parse and the modification-time sort both survive a rename. A file
 * that never carried that prefix is given one minted from its own mtime -- without
 * it the typed name would come back through the parse with its underscores showing.
 */
private fun renameMagazine(dir: File, item: MagazinePdfItem, input: String): RenameOutcome {
    val topic = toTopicSegment(input)
    if (topic.isEmpty()) return RenameOutcome.EMPTY_NAME

    val currentRawName = item.file.nameWithoutExtension
    val parts = currentRawName.split("_")
    val newRawName = if (parts.size >= 3) {
        "${parts[0]}_${parts[1]}_$topic"
    } else {
        "magazine_${item.lastModified}_$topic"
    }
    if (newRawName == currentRawName) return RenameOutcome.UNCHANGED

    val targetPdf = File(dir, "$newRawName.pdf")
    if (targetPdf.exists()) return RenameOutcome.NAME_TAKEN
    if (!item.file.renameTo(targetPdf)) return RenameOutcome.FAILED

    // PDF first, so a failure here can only ever cost the cover -- never the
    // magazine. No pre-delete of the destination: renameTo maps to rename(2),
    // which replaces an existing file atomically, whereas delete-then-rename
    // opens a window where a process kill loses the cover entirely.
    item.coverFile?.let { cover ->
        val targetCover = File(dir, "${newRawName}_cover.jpg")
        if (!cover.renameTo(targetCover)) {
            // The PDF has already moved, so the card now reads targetCover.
            // Anything sitting there is a stale orphan from this magazine's own
            // rename lineage (the NAME_TAKEN check proved no PDF claims it);
            // showing it would caption the wrong image with the new title, so
            // drop it and fall back to the "PDF" placeholder instead.
            targetCover.delete()
        }
    }
    return RenameOutcome.SUCCESS
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyMagazinesScreen(
    onBack: () -> Unit,
    onMagazineSelected: (File) -> Unit
) {
    val tokens = LocalThemeTokens.current
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }
    var itemToDelete by remember { mutableStateOf<MagazinePdfItem?>(null) }
    var itemForActions by remember { mutableStateOf<MagazinePdfItem?>(null) }
    var itemToRename by remember { mutableStateOf<MagazinePdfItem?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(MagazineSortOrder.NEWEST) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val magazinesDir = remember { File(context.filesDir, "magazines") }

    // Retrieve magazines from context.filesDir/magazines
    val magazines = remember(refreshTrigger) {
        val list = mutableListOf<MagazinePdfItem>()
        if (magazinesDir.exists() && magazinesDir.isDirectory) {
            val files = magazinesDir.listFiles { file -> file.isFile && file.extension == "pdf" }
            if (files != null) {
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

                    val sizeBytes = file.length()
                    val sizeKb = sizeBytes / 1024
                    val sizeString = if (sizeKb > 1024) {
                        String.format(Locale.getDefault(), "%.1f MB", sizeKb / 1024.0)
                    } else {
                        "$sizeKb KB"
                    }

                    val coverFilename = "${rawName}_cover.jpg"
                    val coverFile = File(magazinesDir, coverFilename)

                    list.add(
                        MagazinePdfItem(
                            file = file,
                            name = displayName,
                            formattedDate = dateFormat.format(Date(file.lastModified())),
                            formattedSize = sizeString,
                            coverFile = if (coverFile.exists()) coverFile else null,
                            lastModified = file.lastModified(),
                            sizeBytes = sizeBytes
                        )
                    )
                }
            }
        }
        list
    }

    // Ordering lives here rather than in the scan so the sort control can reorder
    // without touching the disk again.
    val visibleMagazines = remember(magazines, searchQuery, sortOrder) {
        val query = toSearchKey(searchQuery)
        val matches = if (query.isEmpty()) {
            magazines.toList()
        } else {
            // Both sides get their separators flattened, so "my_trip" finds the
            // magazine displayed as "my trip" and vice versa.
            magazines.filter { toSearchKey(it.name).contains(query, ignoreCase = true) }
        }
        when (sortOrder) {
            MagazineSortOrder.NEWEST -> matches.sortedByDescending { it.lastModified }
            MagazineSortOrder.OLDEST -> matches.sortedBy { it.lastModified }
            // Collator, not lowercase+ordinal: ordinal puts digits before letters
            // and accented letters after "z", which is wrong in every locale.
            MagazineSortOrder.NAME -> matches.sortedWith(compareBy(nameCollator) { it.name })
            MagazineSortOrder.LARGEST -> matches.sortedByDescending { it.sizeBytes }
        }
    }

    val borderCol = tokens.textSecondary.copy(alpha = 0.25f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.screenBackground)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = tokens.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Library",
                style = LocalTextStyle.current.copy(
                    fontFamily = tokens.wordmarkFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = tokens.textPrimary
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (magazines.isNotEmpty()) {
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = tokens.textPrimary)
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(tokens.surfaceOverlay)
                    ) {
                        MagazineSortOrder.values().forEach { order ->
                            val selected = order == sortOrder
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label,
                                        color = if (selected) tokens.primaryAccent else tokens.textPrimary,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp
                                    )
                                },
                                trailingIcon = {
                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = tokens.primaryAccent
                                        )
                                    }
                                },
                                onClick = {
                                    sortOrder = order
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (magazines.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        text = "Search your library",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = tokens.textPrimary,
                    unfocusedTextColor = tokens.textPrimary,
                    cursorColor = tokens.primaryAccent,
                    focusedBorderColor = tokens.primaryAccent,
                    unfocusedBorderColor = borderCol,
                    focusedContainerColor = tokens.surface,
                    unfocusedContainerColor = tokens.surface,
                    focusedLeadingIconColor = tokens.primaryAccent,
                    unfocusedLeadingIconColor = tokens.textSecondary,
                    focusedTrailingIconColor = tokens.textSecondary,
                    unfocusedTrailingIconColor = tokens.textSecondary,
                    focusedPlaceholderColor = tokens.textSecondary,
                    unfocusedPlaceholderColor = tokens.textSecondary
                )
            )
        }

        if (visibleMagazines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (magazines.isEmpty()) "Your library is empty." else "No magazines match.",
                        color = tokens.textSecondary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (magazines.isEmpty()) {
                            "Craft a magazine to see it here."
                        } else {
                            "Try a different search."
                        },
                        color = tokens.primaryAccent,
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
                items(visibleMagazines) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f)
                            .combinedClickable(
                                onClick = { onMagazineSelected(item.file) },
                                onLongClick = { itemForActions = item }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderCol),
                        colors = CardDefaults.cardColors(containerColor = tokens.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Cover art occupies the flexible top area; the title
                            // and date are siblings below it, not overlaid on top.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(tokens.screenBackground, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.coverFile != null) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                                            .data(item.coverFile)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Cover Image",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "PDF",
                                            color = tokens.secondaryAccent,
                                            fontFamily = tokens.wordmarkFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.formattedSize,
                                            color = tokens.textSecondary,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.name,
                                fontFamily = tokens.wordmarkFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = tokens.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = item.formattedDate,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = tokens.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // Kept outside the empty/non-empty branch so the confirmation dialog is
    // reachable in every state rather than only when the grid is populated.
    val actionTarget = itemForActions
    if (actionTarget != null) {
        AlertDialog(
            onDismissRequest = { itemForActions = null },
            title = {
                Text(
                    text = actionTarget.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column {
                    MagazineActionRow(
                        icon = Icons.Default.Edit,
                        label = "Rename",
                        tint = tokens.textPrimary
                    ) {
                        // Truncated on the way IN. Display names are uncapped
                        // (the generator builds them from the raw prompt), so
                        // seeding an over-length value would leave the field
                        // unable to accept any edit -- see onValueChange below.
                        renameInput = actionTarget.name.take(MAX_TITLE_LENGTH)
                        renameError = null
                        itemToRename = actionTarget
                        itemForActions = null
                    }
                    MagazineActionRow(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    ) {
                        itemToDelete = actionTarget
                        itemForActions = null
                    }
                }
            },
            // Rename/Delete are the actions; this dialog has no affirmative, so
            // Cancel belongs in the dismiss slot.
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { itemForActions = null }) {
                    Text("Cancel", color = tokens.textPrimary)
                }
            },
            containerColor = tokens.surfaceOverlay,
            titleContentColor = tokens.textPrimary,
            textContentColor = tokens.textSecondary
        )
    }

    val renameTarget = itemToRename
    if (renameTarget != null) {
        val sanitized = toTopicSegment(renameInput)
        AlertDialog(
            onDismissRequest = {
                itemToRename = null
                renameError = null
            },
            title = { Text("Rename Magazine") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameInput,
                        // Truncate rather than reject: rejecting the whole edit
                        // freezes the field whenever the current value already
                        // exceeds the cap, since backspacing still leaves it
                        // over the limit and the edit is discarded.
                        onValueChange = {
                            renameInput = it.take(MAX_TITLE_LENGTH)
                            renameError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Magazine title",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            if (renameInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    renameInput = ""
                                    renameError = null
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear name")
                                }
                            }
                        },
                        singleLine = true,
                        isError = renameError != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tokens.textPrimary,
                            unfocusedTextColor = tokens.textPrimary,
                            errorTextColor = tokens.textPrimary,
                            cursorColor = tokens.primaryAccent,
                            focusedBorderColor = tokens.primaryAccent,
                            unfocusedBorderColor = borderCol,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            focusedContainerColor = tokens.surface,
                            unfocusedContainerColor = tokens.surface,
                            errorContainerColor = tokens.surface,
                            focusedTrailingIconColor = tokens.textSecondary,
                            unfocusedTrailingIconColor = tokens.textSecondary,
                            focusedPlaceholderColor = tokens.textSecondary,
                            unfocusedPlaceholderColor = tokens.textSecondary
                        )
                    )
                    if (renameError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = renameError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (renameMagazine(magazinesDir, renameTarget, renameInput)) {
                            RenameOutcome.SUCCESS -> {
                                itemToRename = null
                                renameError = null
                                refreshTrigger++
                            }
                            RenameOutcome.UNCHANGED -> {
                                itemToRename = null
                                renameError = null
                            }
                            RenameOutcome.EMPTY_NAME ->
                                renameError = "Give the magazine a name with letters or numbers."
                            RenameOutcome.NAME_TAKEN ->
                                renameError = "A magazine with that name already exists."
                            RenameOutcome.FAILED ->
                                renameError = "Couldn't rename that file. Try a different name."
                        }
                    },
                    enabled = sanitized.isNotEmpty()
                ) {
                    Text(
                        text = "Rename",
                        color = if (sanitized.isNotEmpty()) tokens.primaryAccent else tokens.textSecondary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    itemToRename = null
                    renameError = null
                }) {
                    Text("Cancel", color = tokens.textPrimary)
                }
            },
            containerColor = tokens.surfaceOverlay,
            titleContentColor = tokens.textPrimary,
            textContentColor = tokens.textSecondary
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Magazine") },
            text = { Text("Are you sure you want to delete '${itemToDelete?.name}'? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { target ->
                        target.file.delete()
                        // Derived from the stem rather than target.coverFile:
                        // that field is a snapshot from the last directory scan,
                        // and saveFilesToDisk writes the PDF before its cover, so
                        // a scan landing between the two records null for a cover
                        // that exists moments later. Deleting by path leaves
                        // nothing behind either way.
                        File(magazinesDir, "${target.file.nameWithoutExtension}_cover.jpg").delete()
                    }
                    itemToDelete = null
                    refreshTrigger++
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = tokens.textPrimary)
                }
            },
            containerColor = tokens.surfaceOverlay,
            titleContentColor = tokens.textPrimary,
            textContentColor = tokens.textSecondary
        )
    }
}

@Composable
private fun MagazineActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = tint,
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp
        )
    }
}
