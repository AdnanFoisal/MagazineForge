package com.magazineforge.app.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
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
import com.magazineforge.app.ui.theme.PitchBlack
import com.magazineforge.app.ui.theme.DarkSurface
import com.magazineforge.app.ui.theme.Graphite
import com.magazineforge.app.ui.theme.BorderDark
import com.magazineforge.app.ui.theme.BorderLight
import com.magazineforge.app.ui.theme.EditorialGold
import com.magazineforge.app.ui.theme.GoldBright
import com.magazineforge.app.ui.theme.ErrorRed
import com.magazineforge.app.ui.theme.GhostWhite
import com.magazineforge.app.ui.theme.AshGrey
import com.magazineforge.app.ui.theme.MutedWarm
import com.magazineforge.app.ui.theme.LuxeTypography
import java.util.UUID

data class PageBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "article",
    val topic: String = "",
    val imageUrl: String = ""
)

fun uriToBase64(context: Context, uri: android.net.Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    templateVariant: String,
    isCompileLoading: Boolean,
    onCompileClicked: (String, List<PageBlock>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var magazineTopic by remember { mutableStateOf("") }
    
    val pages = remember { mutableStateListOf(
        PageBlock(type = "cover", topic = "Cover Theme"),
        PageBlock(type = "toc"),
        PageBlock(type = "article", topic = "Main Feature")
    )}

    val obsidian = PitchBlack
    val darkSurface = DarkSurface
    val gold = EditorialGold
    val copper = MutedWarm
    val ivory = GhostWhite
    val mutedGray = AshGrey
    val borderCol = BorderDark

    // Photo Picker launcher
    var activePageIndexForPicker by remember { mutableStateOf<Int?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            activePageIndexForPicker?.let { index ->
                val base64 = uriToBase64(context, it)
                if (base64 != null) {
                    pages[index] = pages[index].copy(imageUrl = base64)
                } else {
                    pages[index] = pages[index].copy(imageUrl = it.toString())
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(obsidian)
    ) {
        // Custom Styled Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = ivory)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Editorial Desk",
                    style = LuxeTypography.headlineSmall.copy(color = ivory)
                )
                Text(
                    text = "Template Variant: ${templateVariant.replace("cover_template_", "").uppercase()}",
                    style = LuxeTypography.labelSmall.copy(color = gold)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Topic OutlinedTextField with Gold borders
            item {
                OutlinedTextField(
                    value = magazineTopic,
                    onValueChange = { magazineTopic = it },
                    label = { Text("Overall Magazine Topic", style = LuxeTypography.bodyMedium) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LuxeTypography.bodyLarge.copy(color = ivory),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        unfocusedBorderColor = borderCol,
                        cursorColor = gold,
                        focusedLabelColor = gold,
                        unfocusedLabelColor = mutedGray
                    )
                )
            }

            item {
                Text(
                    text = "Page Sequencer",
                    style = LuxeTypography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ivory
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Timeline Sequence list
            itemsIndexed(pages) { index, page ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Timeline vertical node representation
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 12.dp, top = 16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (page.type == "cover") gold else copper,
                            modifier = Modifier.size(16.dp)
                        ) {}
                        if (index < pages.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(180.dp)
                                    .background(borderCol)
                            )
                        }
                    }

                    // Card for Page Details
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderCol),
                        colors = CardDefaults.cardColors(containerColor = darkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Page ${index + 1}: ${page.type.uppercase()}",
                                    style = LuxeTypography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = gold
                                    )
                                )
                                if (pages.size > 1) {
                                    IconButton(
                                        onClick = { pages.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = ErrorRed.copy(alpha = 0.8f))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (page.type != "toc") {
                                OutlinedTextField(
                                    value = page.topic,
                                    onValueChange = { newTopic -> pages[index] = page.copy(topic = newTopic) },
                                    label = { Text(if (page.type == "cover") "Cover Title" else "Article Topic", style = LuxeTypography.labelSmall) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = LuxeTypography.bodyMedium.copy(color = ivory),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = copper,
                                        unfocusedBorderColor = borderCol,
                                        focusedLabelColor = copper,
                                        unfocusedLabelColor = mutedGray
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Image Selection Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = {
                                            activePageIndexForPicker = index
                                            photoLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = copper.copy(alpha = 0.15f),
                                            contentColor = gold
                                        ),
                                        border = BorderStroke(1.dp, gold),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (page.imageUrl.isNotEmpty()) "Change Image" else "Select Image",
                                            style = LuxeTypography.labelMedium
                                        )
                                    }
                                    
                                    if (page.imageUrl.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Image Selected",
                                            style = LuxeTypography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = GoldBright
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Table of contents is auto-generated based on the other articles in this magazine compilation.",
                                    style = LuxeTypography.labelSmall.copy(color = mutedGray)
                                )
                            }
                        }
                    }
                }
            }

            // Custom Page Addition Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { pages.add(PageBlock(type = "article")) },
                        border = BorderStroke(1.dp, borderCol),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ivory),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Article", style = LuxeTypography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { pages.add(PageBlock(type = "cover")) },
                        border = BorderStroke(1.dp, borderCol),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ivory),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Cover", style = LuxeTypography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { pages.add(PageBlock(type = "toc")) },
                        border = BorderStroke(1.dp, borderCol),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ivory),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ TOC", style = LuxeTypography.labelMedium)
                    }
                }
            }
        }

        // Submitting Action (Large filled button taking full width and 56dp height)
        Button(
            onClick = { onCompileClicked(magazineTopic, pages.toList()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = gold,
                contentColor = obsidian,
                disabledContainerColor = borderCol,
                disabledContentColor = mutedGray
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = magazineTopic.isNotBlank() && pages.isNotEmpty() && !isCompileLoading
        ) {
            Text(
                text = "Craft Magazine",
                style = LuxeTypography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
