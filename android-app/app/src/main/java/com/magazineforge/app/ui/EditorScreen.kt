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

    val obsidian = Color(0xFF0F0F10)
    val darkSurface = Color(0xFF18181B)
    val gold = Color(0xFFC5A059)
    val copper = Color(0xFFB87333)
    val ivory = Color(0xFFF5F5F7)
    val mutedGray = Color(0xFFA1A1AA)
    val borderCol = Color(0xFF2E2A24)

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
                    style = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = ivory
                    )
                )
                Text(
                    text = "Template Variant: ${templateVariant.replace("cover_template_", "").uppercase()}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.SansSerif,
                        color = gold
                    )
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
                    label = { Text("Overall Magazine Topic", color = gold, fontFamily = FontFamily.SansSerif) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = ivory, fontFamily = FontFamily.SansSerif),
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
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ivory,
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
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = gold,
                                    fontSize = 16.sp
                                )
                                if (pages.size > 1) {
                                    IconButton(
                                        onClick = { pages.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (page.type != "toc") {
                                OutlinedTextField(
                                    value = page.topic,
                                    onValueChange = { newTopic -> pages[index] = page.copy(topic = newTopic) },
                                    label = { Text(if (page.type == "cover") "Cover Title" else "Article Topic", fontFamily = FontFamily.SansSerif) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = LocalTextStyle.current.copy(color = ivory, fontFamily = FontFamily.SansSerif),
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
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                    
                                    if (page.imageUrl.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Image Selected",
                                            color = Color.Green,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Table of contents is auto-generated based on the other articles in this magazine compilation.",
                                    color = mutedGray,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif
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
                        Text("+ Article", fontFamily = FontFamily.SansSerif)
                    }
                    OutlinedButton(
                        onClick = { pages.add(PageBlock(type = "cover")) },
                        border = BorderStroke(1.dp, borderCol),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ivory),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Cover", fontFamily = FontFamily.SansSerif)
                    }
                    OutlinedButton(
                        onClick = { pages.add(PageBlock(type = "toc")) },
                        border = BorderStroke(1.dp, borderCol),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ivory),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ TOC", fontFamily = FontFamily.SansSerif)
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
            enabled = magazineTopic.isNotBlank() && pages.isNotEmpty()
        ) {
            Text(
                text = "Craft Magazine",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
