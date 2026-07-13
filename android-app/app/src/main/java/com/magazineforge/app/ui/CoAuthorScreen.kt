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
import androidx.compose.ui.text.font.FontWeight
import com.magazineforge.app.models.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoAuthorScreen(
    initialSchema: MagazineSchema,
    isGeneratingLatex: Boolean,
    isFullAiMode: Boolean,
    pendingArticles: List<com.magazineforge.app.models.BriefArticle> = emptyList(),
    onGenerateRemaining: () -> Unit = {},
    onGenerateLatex: (MagazineSchema) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    val surfaceColor = tokens.surface
    var schema by remember { mutableStateOf(initialSchema) }
    LaunchedEffect(initialSchema) {
        schema = initialSchema
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var onImageUploadedCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    
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
                        // Prepend BASE_URL so the schema + Coil preview use a
                        // consistent absolute URL (matches EditorScreen). The
                        // backend resolves /assets/ paths from local disk at
                        // compile time, so the uploaded image lands in the PDF.
                        val downloadUrl = response.body()?.url
                            ?.let { "${com.magazineforge.app.network.ApiClient.BASE_URL}$it" } ?: ""
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
                    if (isFullAiMode) {
                        Button(
                            onClick = onNext,
                            enabled = isSchemaValid,
                            colors = ButtonDefaults.buttonColors(containerColor = gold)
                        ) {
                            Text("Open Editor", color = obsidian)
                        }
                    } else {
                        if (isSchemaValid) {
                            if (pendingArticles.size > 0) {
                                Button(
                                    onClick = onGenerateRemaining,
                                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent),
                                    enabled = !isGeneratingLatex
                                ) {
                                    Text("Generate All Remaining (${pendingArticles.size})", color = tokens.ctaText)
                                }
                            } else {
                                Button(
                                    onClick = { onGenerateLatex(schema) },
                                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent),
                                    enabled = !isGeneratingLatex
                                ) {
                                    if (isGeneratingLatex) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = tokens.ctaText)
                                    } else {
                                        Text("Generate LaTeX", color = tokens.ctaText)
                                    }
                                }
                            }
                        }
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
            
            schema.masthead?.let { masthead ->
                ExpandableSection("Masthead") {
                    OutlinedTextField(
                        value = masthead.issueTagline,
                        onValueChange = { schema = schema.copy(masthead = masthead.copy(issueTagline = it)) },
                        label = { Text("Issue Tagline") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = masthead.editorsNote,
                        onValueChange = { schema = schema.copy(masthead = masthead.copy(editorsNote = it)) },
                        label = { Text("Editor's Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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


            schema.pages.forEachIndexed { index, page ->
                when (page) {
                    is ArticleSchema -> {
                        ExpandableSection("Article: ${page.headline}") {
                            OutlinedTextField(
                                value = page.headline,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(headline = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = page.bodyCopy,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(bodyCopy = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Body Copy") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5
                            )
                        }
                    }
                    is AdSchema -> {
                        ExpandableSection("Ad: ${page.fakeCompanyName}") {
                            OutlinedTextField(
                                value = page.headline,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(headline = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Ad Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is QnASchema -> {
                        ExpandableSection("Q&A: ${page.headline}") {
                            OutlinedTextField(
                                value = page.headline,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(headline = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Q&A Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = page.interviewer,
                                    onValueChange = {
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(interviewer = it)
                                        schema = schema.copy(pages = newPages)
                                    },
                                    label = { Text("Interviewer") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = page.interviewee,
                                    onValueChange = {
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(interviewee = it)
                                        schema = schema.copy(pages = newPages)
                                    },
                                    label = { Text("Interviewee") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            page.qnaItems.forEachIndexed { qnaIndex, qnaItem ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Q&A Item ${qnaIndex + 1}", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = qnaItem.question,
                                    onValueChange = {
                                        val newItems = page.qnaItems.toMutableList()
                                        newItems[qnaIndex] = qnaItem.copy(question = it)
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(qnaItems = newItems)
                                        schema = schema.copy(pages = newPages)
                                    },
                                    label = { Text("Question") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = qnaItem.answer,
                                    onValueChange = {
                                        val newItems = page.qnaItems.toMutableList()
                                        newItems[qnaIndex] = qnaItem.copy(answer = it)
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(qnaItems = newItems)
                                        schema = schema.copy(pages = newPages)
                                    },
                                    label = { Text("Answer") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )
                            }
                        }
                    }
                    is ChartSchema -> {
                        ExpandableSection("Chart: ${page.headline}") {
                            OutlinedTextField(
                                value = page.headline,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(headline = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Chart Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = page.xLabel,
                                    onValueChange = {
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(xLabel = it)
                                        schema = schema.copy(pages = newPages)
                                    },
                                    label = { Text("X-Axis Label") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = page.yLabel,
                                    onValueChange = {
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(yLabel = it)
                                        schema = schema.copy(pages = newPages)
                                    },
                                    label = { Text("Y-Axis Label") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text("Data Points Editing Coming Soon...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    is PhotoEssaySchema -> {
                        ExpandableSection("Photo Essay: ${page.headline}") {
                            OutlinedTextField(
                                value = page.headline,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(headline = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Essay Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = page.closingText,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(closingText = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Closing Text") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }
                    else -> {
                    }
                }
            }
            
            if (pendingArticles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("QUEUED FOR GENERATION", color = gold, style = com.magazineforge.app.ui.theme.LuxeTypography.labelMedium)
                pendingArticles.forEach { article ->
                    Card(
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, tokens.secondaryAccent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⏳ ${article.topic}", color = tokens.textPrimary, style = MaterialTheme.typography.titleMedium)
                            Text("This section is pending generation.", color = tokens.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            schema.backCover?.let { backCover ->
                ExpandableSection("Back Cover") {
                    OutlinedTextField(
                        value = backCover.tagline,
                        onValueChange = { schema = schema.copy(backCover = backCover.copy(tagline = it)) },
                        label = { Text("Tagline") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ImageUploadField(
                        label = "Back Cover Image URL",
                        value = backCover.imageUrl ?: "",
                        onValueChange = { schema = schema.copy(backCover = backCover.copy(imageUrl = it)) },
                        onPickImage = { pickImage { url -> schema = schema.copy(backCover = backCover.copy(imageUrl = url)) } }
                    )
                }
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
