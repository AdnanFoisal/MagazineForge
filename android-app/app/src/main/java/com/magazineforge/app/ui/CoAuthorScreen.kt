package com.magazineforge.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.models.MagazineSchema
import com.magazineforge.app.models.ArticleSchema
import com.magazineforge.app.models.TocItemSchema
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoAuthorScreen(
    initialSchema: MagazineSchema,
    isGeneratingLatex: Boolean,
    onGenerateLatex: (MagazineSchema) -> Unit,
    onBack: () -> Unit
) {
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    val surfaceColor = tokens.surface
    var schema by remember { mutableStateOf(initialSchema) }
    val coroutineScope = rememberCoroutineScope()
    
    // We will pass down this helper to any field that wants an image
    // It launches an image picker, uploads to Firebase, and calls onUrlReceived
    var onImageUploadedCallback: ((String) -> Unit)? by remember { mutableStateOf(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    val outputStream = java.io.FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    
                    val requestFile = okhttp3.RequestBody.create("image/*".toMediaTypeOrNull(), tempFile)
                    val body = okhttp3.MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                    
                    val response = com.magazineforge.app.network.ApiClient.retrofitService.uploadAsset(body)
                    if (response.isSuccessful) {
                        val downloadUrl = response.body()?.url ?: ""
                        if (downloadUrl.isNotEmpty()) {
                            onImageUploadedCallback?.invoke(downloadUrl)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val gold = tokens.primaryAccent
    val obsidian = tokens.editorBackground

    val pickImage = { callback: (String) -> Unit ->
        onImageUploadedCallback = callback
        launcher.launch("image/*")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Co-Author Schema", color = gold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = obsidian),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = tokens.textPrimary)
                    }
                },
                actions = {
                    val isSchemaValid = schema.cover.mainTitle.isNotBlank()
                    Button(
                        onClick = { onGenerateLatex(schema) },
                        enabled = !isGeneratingLatex && isSchemaValid,
                        colors = ButtonDefaults.buttonColors(containerColor = gold)
                    ) {
                        Text(if (isGeneratingLatex) "Generating..." else "Next", color = obsidian)
                    }
                }
            )
        },
        containerColor = obsidian
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Expandable sections for Cover, Masthead, TOC, Articles, BackCover
            ExpandableSection("Cover") {
                OutlinedTextField(
                    value = schema.cover.mainTitle,
                    onValueChange = { schema = schema.copy(cover = schema.cover.copy(mainTitle = it)) },
                    label = { Text("Main Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schema.cover.subtitle,
                    onValueChange = { schema = schema.copy(cover = schema.cover.copy(subtitle = it)) },
                    label = { Text("Subtitle") },
                    modifier = Modifier.fillMaxWidth()
                )
                ImageUploadField(
                    label = "Cover Image URL",
                    value = schema.cover.imageUrl,
                    onValueChange = { schema = schema.copy(cover = schema.cover.copy(imageUrl = it)) },
                    onPickImage = { pickImage { url -> schema = schema.copy(cover = schema.cover.copy(imageUrl = url)) } }
                )
            }
            
            ExpandableSection("Masthead") {
                OutlinedTextField(
                    value = schema.masthead.issueTagline,
                    onValueChange = { schema = schema.copy(masthead = schema.masthead.copy(issueTagline = it)) },
                    label = { Text("Issue Tagline") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schema.masthead.editorsNote,
                    onValueChange = { schema = schema.copy(masthead = schema.masthead.copy(editorsNote = it)) },
                    label = { Text("Editor's Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ExpandableSection("Table of Contents") {
                schema.toc.forEachIndexed { index, item ->
                    Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            OutlinedTextField(
                                value = item.sectionTitle,
                                onValueChange = { 
                                    val newToc = schema.toc.toMutableList()
                                    newToc[index] = item.copy(sectionTitle = it)
                                    schema = schema.copy(toc = newToc) 
                                },
                                label = { Text("Section Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = item.teaser,
                                onValueChange = { 
                                    val newToc = schema.toc.toMutableList()
                                    newToc[index] = item.copy(teaser = it)
                                    schema = schema.copy(toc = newToc) 
                                },
                                label = { Text("Teaser") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            schema.articles.forEachIndexed { index, article ->
                ExpandableSection("Article: ${article.headline}") {
                    OutlinedTextField(
                        value = article.headline,
                        onValueChange = {
                            val newArticles = schema.articles.toMutableList()
                            newArticles[index] = article.copy(headline = it)
                            schema = schema.copy(articles = newArticles)
                        },
                        label = { Text("Headline") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = article.bodyCopy,
                        onValueChange = {
                            val newArticles = schema.articles.toMutableList()
                            newArticles[index] = article.copy(bodyCopy = it)
                            schema = schema.copy(articles = newArticles)
                        },
                        label = { Text("Body Copy") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )
                    article.images.forEachIndexed { imgIndex, img ->
                        ImageUploadField(
                            label = "Article Image ${imgIndex + 1} URL",
                            value = img.imageUrl,
                            onValueChange = { newUrl ->
                                val newImages = article.images.toMutableList()
                                newImages[imgIndex] = img.copy(imageUrl = newUrl)
                                val newArticles = schema.articles.toMutableList()
                                newArticles[index] = article.copy(images = newImages)
                                schema = schema.copy(articles = newArticles)
                            },
                            onPickImage = {
                                pickImage { url ->
                                    val newImages = article.images.toMutableList()
                                    newImages[imgIndex] = img.copy(imageUrl = url)
                                    val newArticles = schema.articles.toMutableList()
                                    newArticles[index] = article.copy(images = newImages)
                                    schema = schema.copy(articles = newArticles)
                                }
                            }
                        )
                    }
                }
            }

            ExpandableSection("Back Cover") {
                OutlinedTextField(
                    value = schema.backCover.tagline,
                    onValueChange = { schema = schema.copy(backCover = schema.backCover.copy(tagline = it)) },
                    label = { Text("Tagline") },
                    modifier = Modifier.fillMaxWidth()
                )
                ImageUploadField(
                    label = "Back Cover Image URL",
                    value = schema.backCover.imageUrl ?: "",
                    onValueChange = { schema = schema.copy(backCover = schema.backCover.copy(imageUrl = it)) },
                    onPickImage = { pickImage { url -> schema = schema.copy(backCover = schema.backCover.copy(imageUrl = url)) } }
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = tokens.surface),
        border = BorderStroke(1.dp, tokens.secondaryAccent.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.uppercase(),
                    style = com.magazineforge.app.ui.theme.LuxeTypography.labelMedium.copy(
                        color = tokens.textSecondary,
                        letterSpacing = 2.sp
                    )
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = tokens.textSecondary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ImageUploadField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPickImage: () -> Unit
) {
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    var useDriveLink by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = com.magazineforge.app.ui.theme.LuxeTypography.labelSmall.copy(color = tokens.textPrimary))
            Text(
                text = if (useDriveLink) "Use Local Image" else "Use Drive Link",
                style = com.magazineforge.app.ui.theme.LuxeTypography.labelSmall.copy(color = com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent),
                modifier = Modifier.clickable { useDriveLink = !useDriveLink }
            )
        }

        if (useDriveLink) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Paste Google Drive link here", color = tokens.textPrimary.copy(alpha=0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = com.magazineforge.app.ui.theme.LuxeTypography.bodyMedium.copy(color = tokens.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent,
                    unfocusedBorderColor = tokens.secondaryAccent.copy(alpha = 0.3f),
                    cursorColor = com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent
                )
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text("Or select from gallery", color = tokens.textPrimary.copy(alpha=0.5f)) },
                    modifier = Modifier.weight(1f),
                    textStyle = com.magazineforge.app.ui.theme.LuxeTypography.bodyMedium.copy(color = tokens.textPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent,
                        unfocusedBorderColor = tokens.secondaryAccent.copy(alpha = 0.3f),
                        cursorColor = com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onPickImage,
                    modifier = Modifier.background(com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Pick Image",
                        tint = com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent
                    )
                }
            }
        }
    }
}
