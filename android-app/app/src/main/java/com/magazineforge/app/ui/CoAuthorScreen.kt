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
import androidx.compose.material.icons.filled.Search
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
import okhttp3.RequestBody.Companion.asRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoAuthorScreen(
    initialSchema: MagazineSchema,
    isGeneratingLatex: Boolean,
    runState: GenerationRunState,
    isFullAiMode: Boolean,
    onGenerateRemaining: () -> Unit = {},
    onRetrySection: (String) -> Unit = {},
    onGenerateLatex: (MagazineSchema) -> Unit,
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

    // --- NEW: Image picker state ---------------------------------------------
    // The picker is opened by tapping the "search" icon on any ImageUploadField.
    // pickerQuery pre-fills the search box with the slot's image_query (or the
    // magazine's subject as fallback) so the user sees relevant results
    // immediately. pickerCallback receives the chosen URL and writes it into
    // the right schema slot.
    var pickerOpen by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }
    var pickerCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val viewModel: EditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    if (pickerOpen) {
        ImagePickerSheet(
            initialQuery = pickerQuery,
            onSearch = { query, onResult ->
                viewModel.previewImages(query) { images, err ->
                    onResult(images, err)
                }
            },
            onPicked = { url ->
                pickerCallback?.invoke(url)
                pickerCallback = null
            },
            onDismiss = {
                pickerOpen = false
                pickerCallback = null
            }
        )
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val downloadUrl = com.magazineforge.app.network.ApiClient.uploadImageWithRetry(context, uri, quality = "cover")
                    if (downloadUrl.isNotEmpty()) {
                        onImageUploadedCallback?.invoke(downloadUrl)
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
                        // In Full AI mode, the button generates LaTeX (which then
                        // auto-compiles to PDF via generateLatex's isFullAiMode
                        // auto-chain). Previously this called onNext which just
                        // navigated to the LaTeX notebook WITHOUT generating
                        // LaTeX first — leaving the editor empty and causing
                        // the subsequent compile to fail with "Emergency stop".
                        Button(
                            onClick = { onGenerateLatex(schema) },
                            enabled = isSchemaValid && !isGeneratingLatex,
                            colors = ButtonDefaults.buttonColors(containerColor = gold)
                        ) {
                            if (isGeneratingLatex) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = obsidian)
                            } else {
                                Text("Generate", color = obsidian)
                            }
                        }
                    } else {
                        if (isSchemaValid) {
                            val activeState = runState as? GenerationRunState.Active
                            val allSectionsCompleted = activeState?.status?.sections?.all { it.status == "COMPLETED" } ?: false
                            val isRunPaused = activeState?.status?.status == "PAUSED"
                            val isRunGenerating = activeState?.status?.status == "GENERATING" || activeState?.status?.status == "PREPARING"
                            
                            if (activeState != null && !allSectionsCompleted) {
                                Button(
                                    onClick = onGenerateRemaining,
                                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent),
                                    enabled = !isRunGenerating
                                ) {
                                    if (isRunGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = tokens.ctaText)
                                    } else {
                                        Text(if (isRunPaused) "Resume Generation" else "Generate Remaining (${activeState.status.totalSections - activeState.status.completedSections})", color = tokens.ctaText)
                                    }
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
                OutlinedTextField(
                    value = schema.cover.accentHex,
                    onValueChange = { schema = schema.copy(cover = schema.cover.copy(accentHex = it)) },
                    label = { Text("Title & Accent Color (#Hex)") },
                    modifier = Modifier.fillMaxWidth()
                )
                ImageUploadField(
                    label = "Cover Image URL",
                    value = schema.cover.imageUrl,
                    onValueChange = { schema = schema.copy(cover = schema.cover.copy(imageUrl = it)) },
                    onPickImage = { pickImage { url -> schema = schema.copy(cover = schema.cover.copy(imageUrl = url)) } },
                    onPickFromSearch = {
                        pickerQuery = schema.cover.imageQuery.ifBlank { schema.cover.mainTitle }
                        pickerCallback = { url -> schema = schema.copy(cover = schema.cover.copy(imageUrl = url)) }
                        pickerOpen = true
                    }
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
                            val effectiveImages = if (page.images.isEmpty()) {
                                listOf(com.magazineforge.app.models.ArticleImageSchema(imageUrl = "", imageQuery = page.headline, caption = "", placement = "top"))
                            } else {
                                page.images
                            }
                            effectiveImages.forEachIndexed { imgIndex, img ->
                                ImageUploadField(
                                    label = "Article Image URL ${imgIndex + 1}",
                                    value = img.imageUrl,
                                    onValueChange = { newUrl ->
                                        val newImages = if (page.images.isEmpty()) mutableListOf(img.copy(imageUrl = newUrl)) else page.images.toMutableList()
                                        if (imgIndex < newImages.size) newImages[imgIndex] = newImages[imgIndex].copy(imageUrl = newUrl)
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(images = newImages)
                                        schema = schema.copy(pages = newPages)
                                    },
                                    onPickImage = {
                                        pickImage { url ->
                                            val newImages = if (page.images.isEmpty()) mutableListOf(img.copy(imageUrl = url)) else page.images.toMutableList()
                                            if (imgIndex < newImages.size) newImages[imgIndex] = newImages[imgIndex].copy(imageUrl = url)
                                            val newPages = schema.pages.toMutableList()
                                            newPages[index] = page.copy(images = newImages)
                                            schema = schema.copy(pages = newPages)
                                        }
                                    },
                                    onPickFromSearch = {
                                        pickerQuery = img.imageQuery.ifBlank { page.headline }.ifBlank { schema.cover.mainTitle }
                                        pickerCallback = { url ->
                                            val newImages = if (page.images.isEmpty()) mutableListOf(img.copy(imageUrl = url)) else page.images.toMutableList()
                                            if (imgIndex < newImages.size) newImages[imgIndex] = newImages[imgIndex].copy(imageUrl = url)
                                            val newPages = schema.pages.toMutableList()
                                            newPages[index] = page.copy(images = newImages)
                                            schema = schema.copy(pages = newPages)
                                        }
                                        pickerOpen = true
                                    }
                                )
                            }
                            TextButton(
                                onClick = {
                                    val newImages = page.images.toMutableList()
                                    newImages.add(com.magazineforge.app.models.ArticleImageSchema(imageUrl = "", imageQuery = page.headline, caption = "", placement = "top"))
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(images = newImages)
                                    schema = schema.copy(pages = newPages)
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("+ Add Image Slot", color = tokens.primaryAccent)
                            }
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
                            ImageUploadField(
                                label = "Ad Image URL",
                                value = page.imageUrl,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(imageUrl = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                onPickImage = {
                                    pickImage { url ->
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(imageUrl = url)
                                        schema = schema.copy(pages = newPages)
                                    }
                                },
                                onPickFromSearch = {
                                    pickerQuery = page.imageQuery.ifBlank { page.headline }.ifBlank { page.fakeCompanyName }.ifBlank { schema.cover.mainTitle }
                                    pickerCallback = { url ->
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(imageUrl = url)
                                        schema = schema.copy(pages = newPages)
                                    }
                                    pickerOpen = true
                                }
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
                            val effectiveImages = if (page.images.isEmpty()) {
                                listOf(com.magazineforge.app.models.ArticleImageSchema(imageUrl = "", imageQuery = page.headline, caption = "", placement = "top"))
                            } else {
                                page.images
                            }
                            effectiveImages.forEachIndexed { imgIndex, img ->
                                ImageUploadField(
                                    label = "Essay Image URL ${imgIndex + 1}",
                                    value = img.imageUrl,
                                    onValueChange = { newUrl ->
                                        val newImages = if (page.images.isEmpty()) mutableListOf(img.copy(imageUrl = newUrl)) else page.images.toMutableList()
                                        if (imgIndex < newImages.size) newImages[imgIndex] = newImages[imgIndex].copy(imageUrl = newUrl)
                                        val newPages = schema.pages.toMutableList()
                                        newPages[index] = page.copy(images = newImages)
                                        schema = schema.copy(pages = newPages)
                                    },
                                    onPickImage = {
                                        pickImage { url ->
                                            val newImages = if (page.images.isEmpty()) mutableListOf(img.copy(imageUrl = url)) else page.images.toMutableList()
                                            if (imgIndex < newImages.size) newImages[imgIndex] = newImages[imgIndex].copy(imageUrl = url)
                                            val newPages = schema.pages.toMutableList()
                                            newPages[index] = page.copy(images = newImages)
                                            schema = schema.copy(pages = newPages)
                                        }
                                    },
                                    onPickFromSearch = {
                                        pickerQuery = img.imageQuery.ifBlank { page.headline }.ifBlank { schema.cover.mainTitle }
                                        pickerCallback = { url ->
                                            val newImages = if (page.images.isEmpty()) mutableListOf(img.copy(imageUrl = url)) else page.images.toMutableList()
                                            if (imgIndex < newImages.size) newImages[imgIndex] = newImages[imgIndex].copy(imageUrl = url)
                                            val newPages = schema.pages.toMutableList()
                                            newPages[index] = page.copy(images = newImages)
                                            schema = schema.copy(pages = newPages)
                                        }
                                        pickerOpen = true
                                    }
                                )
                            }
                            TextButton(
                                onClick = {
                                    val newImages = page.images.toMutableList()
                                    newImages.add(com.magazineforge.app.models.ArticleImageSchema(imageUrl = "", imageQuery = page.headline, caption = "", placement = "top"))
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(images = newImages)
                                    schema = schema.copy(pages = newPages)
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("+ Add Image Slot", color = tokens.primaryAccent)
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
            

            val activeState = runState as? GenerationRunState.Active
            if (activeState != null) {
                val incompleteSections = activeState.status.sections.filter { it.status != "COMPLETED" }
                if (incompleteSections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("SECTIONS STATUS", color = gold, style = com.magazineforge.app.ui.theme.LuxeTypography.labelMedium)
                    incompleteSections.forEach { sec ->
                        Card(
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, tokens.secondaryAccent.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sec.topic, color = tokens.textPrimary, style = MaterialTheme.typography.titleMedium)
                                    val statusMsg = when (sec.status) {
                                        "PENDING" -> "Queued"
                                        "PROCESSING" -> "Generating..."
                                        "FAILED" -> "Failed: ${sec.error ?: "Unknown error"}"
                                        else -> sec.status
                                    }
                                    Text("Status: $statusMsg", color = if (sec.status == "FAILED") androidx.compose.ui.graphics.Color.Red else tokens.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                if (sec.status == "FAILED") {
                                    Button(
                                        onClick = { onRetrySection(sec.sectionId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent)
                                    ) {
                                        Text("Retry", color = tokens.ctaText, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
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
                        onPickImage = { pickImage { url -> schema = schema.copy(backCover = backCover.copy(imageUrl = url)) } },
                        onPickFromSearch = {
                            pickerQuery = backCover.imageQuery.ifBlank { backCover.tagline }.ifBlank { schema.cover.mainTitle }
                            pickerCallback = { url -> schema = schema.copy(backCover = backCover.copy(imageUrl = url)) }
                            pickerOpen = true
                        }
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
    onPickImage: () -> Unit,
    onPickFromSearch: (() -> Unit)? = null
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
                // NEW: Search stock photos button (only shown when caller wires it)
                if (onPickFromSearch != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onPickFromSearch,
                        modifier = Modifier.background(com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search stock photos",
                            tint = com.magazineforge.app.ui.theme.LocalThemeTokens.current.primaryAccent
                        )
                    }
                }
            }
        }
    }
}
