package com.magazineforge.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.magazineforge.app.models.GenerateBriefResponse
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.ui.theme.LuxeTypography
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest

data class PageBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    var type: String = "article",
    var topic: String = "",
    var imageUrl: String = "",
    var tone: String = "Professional",
    var layoutDensity: String = "Balanced"
)

data class SectionComposerConfig(
    var enableMasthead: Boolean = true,
    var mastheadAngle: String = "",
    var enableSidebar: Boolean = true,
    var sidebarTopic: String = "",
    var enablePullQuote: Boolean = true,
    var enableBackCover: Boolean = true,
    var enableTocTeasers: Boolean = true,
    var enableByline: Boolean = true
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialPrompt: String = "",
    isCompileLoading: Boolean,
    runState: com.magazineforge.app.ui.GenerationRunState? = null,
    briefState: BriefState,
    schemaState: SchemaState,
    latexState: LatexState,
    compileState: CompileState,
    onGenerateBrief: (String, List<String>, Int) -> Unit,
    onCompileFromBrief: (String, SectionComposerConfig, GenerateBriefResponse, String, String, List<String>) -> Unit,
    onCompileClicked: (magazineTopic: String, pages: List<PageBlock>, config: SectionComposerConfig, coverImgUrl: String) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    initialTabIndex: Int = 0
) {
    var prompt by remember { mutableStateOf(initialPrompt) }
    var coverImageUrl by remember { mutableStateOf("") }
    var backCoverImageUrl by remember { mutableStateOf("") }
    var coverTopic by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex) }
    val pages = remember { mutableStateListOf<PageBlock>() }
    var composerConfig by remember { mutableStateOf(SectionComposerConfig()) }
    var showComposer by remember { mutableStateOf(false) }
    var pageSelection by remember { mutableStateOf("Auto") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val referenceImages = remember { mutableStateListOf<String>() }

    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    val obsidian = tokens.editorBackground
    val darkSurface = tokens.surface
    val gold = tokens.primaryAccent
    val borderCol = tokens.secondaryAccent.copy(alpha = 0.3f)
    val ivory = tokens.editorText
    
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var loadingText by remember { mutableStateOf("Analyzing prompt...") }
    val loadingMessages = listOf(
        "Analyzing prompt...",
        "Structuring layout...",
        "Researching topics...",
        "Writing articles...",
        "Polishing content..."
    )

    LaunchedEffect(briefState) {
        if (briefState is BriefState.Loading) {
            loadingProgress = 0f
            var messageIndex = 0
            val startTime = System.currentTimeMillis()
            val estimatedDuration = 15000f // 15 seconds
            
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < estimatedDuration) {
                    loadingProgress = elapsed / estimatedDuration * 0.9f
                }
                
                val newIndex = ((elapsed / 3000) % loadingMessages.size).toInt()
                if (newIndex != messageIndex) {
                    messageIndex = newIndex
                    loadingText = loadingMessages[messageIndex]
                }
                
                kotlinx.coroutines.delay(100)
            }
        }
    }

    if (showComposer) {
        SectionComposerBottomSheet(
            config = composerConfig,
            onDismiss = { showComposer = false },
            onSave = { composerConfig = it }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("The Studio", color = gold, style = LuxeTypography.headlineMedium) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = obsidian)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = obsidian
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = obsidian,
                contentColor = gold,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = gold
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Full AI Mode") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Assisted Mode") }
                )
            }

            if (selectedTabIndex == 0) {
                // FULL AI MODE
                if (briefState is BriefState.Success) {
                    // Phase 2: Brief Review
                    val brief = briefState.brief
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Generation Brief", style = LuxeTypography.headlineSmall.copy(color = gold, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = darkSurface),
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Category: ${brief.category}", style = LuxeTypography.titleMedium.copy(color = tokens.textPrimary))
                                Text("Tone: ${brief.tone}", style = LuxeTypography.titleMedium.copy(color = tokens.textPrimary))
                                Text("Layout Density: ${brief.styleDna}", style = LuxeTypography.titleMedium.copy(color = tokens.textPrimary))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Potential Titles:", style = LuxeTypography.titleMedium.copy(color = tokens.primaryAccent))
                                brief.titles?.forEach { title ->
                                    Text("- $title", style = LuxeTypography.bodyMedium.copy(color = tokens.textPrimary))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Articles:", style = LuxeTypography.titleMedium.copy(color = tokens.primaryAccent))
                                brief.articles?.forEach { article ->
                                    Text("- ${article.topic}", style = LuxeTypography.bodyMedium.copy(color = tokens.textPrimary))
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showComposer = true },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                            border = BorderStroke(1.dp, gold)
                        ) {
                            Text("Customize Sections ▾", style = LuxeTypography.titleSmall)
                        }

                        Button(
                            onClick = {
                                onCompileFromBrief(prompt, composerConfig, brief, coverImageUrl, backCoverImageUrl, referenceImages.toList())
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = obsidian),
                            enabled = !isCompileLoading
                        ) {
                            if (isCompileLoading) {
                                CircularProgressIndicator(color = obsidian, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Generate Full Issue", style = LuxeTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                } else {
                    // Phase 1: Initial Prompt
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var isCoverUploading by remember { mutableStateOf(false) }
                    var isBackCoverUploading by remember { mutableStateOf(false) }

                    // ── Cover image uploader ─────────────────────────────────
                    // Each launcher uploads one image to /upload-asset and stores
                    // the returned URL in coverImageUrl / backCoverImageUrl.
                    // Uses asRequestBody() (the modern OkHttp extension) instead
                    // of the deprecated RequestBody.create(mediatype, file)
                    // which could hang on certain file types.
                    // Local preview URIs — shown instantly before upload finishes
                    var coverPreviewUri by remember { mutableStateOf<android.net.Uri?>(null) }
                    var backCoverPreviewUri by remember { mutableStateOf<android.net.Uri?>(null) }

                    val coverLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            coverPreviewUri = uri
                            isCoverUploading = true
                            coroutineScope.launch {
                                try {
                                    val url = com.magazineforge.app.network.ApiClient.uploadImageWithRetry(context, uri)
                                    coverImageUrl = url
                                    snackbarHostState.showSnackbar("Cover image uploaded")
                                } catch (e: Exception) {
                                    coverPreviewUri = null
                                    snackbarHostState.showSnackbar("Upload failed: ${e.localizedMessage ?: e.message}")
                                } finally {
                                    isCoverUploading = false
                                }
                            }
                        }
                    }

                    val backCoverLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            backCoverPreviewUri = uri
                            isBackCoverUploading = true
                            coroutineScope.launch {
                                try {
                                    val url = com.magazineforge.app.network.ApiClient.uploadImageWithRetry(context, uri)
                                    backCoverImageUrl = url
                                    snackbarHostState.showSnackbar("Back cover image uploaded")
                                } catch (e: Exception) {
                                    backCoverPreviewUri = null
                                    snackbarHostState.showSnackbar("Upload failed: ${e.localizedMessage ?: e.message}")
                                } finally {
                                    isBackCoverUploading = false
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            placeholder = {
                                Text(
                                    "What are we publishing today?\n\ne.g. 'A special edition on the future of electric aviation.'",
                                    style = LuxeTypography.headlineMedium.copy(color = tokens.editorTextSecondary)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            textStyle = LuxeTypography.headlineMedium.copy(color = tokens.editorText),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = tokens.primaryAccent
                            )
                        )
                        
                        // Page Count Selector
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text("Length: $pageSelection", style = LuxeTypography.titleSmall, color = ivory)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val options = listOf("Auto", "Short", "Medium", "Long")
                                options.forEach { option ->
                                    val isSelected = pageSelection == option
                                    OutlinedButton(
                                        onClick = { pageSelection = option },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) gold else Color.Transparent,
                                            contentColor = if (isSelected) obsidian else ivory
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) gold else borderCol),
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Text(option, style = LuxeTypography.bodyMedium, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // ── Cover Image upload slot ─────────────────────────────
                        // Replaces the old 'Add reference images' disclosure.
                        // In Full AI mode the user uploads at most two images:
                        // a cover image and a back cover image. Each slot accepts
                        // one image. If left empty, the backend fetches a random
                        // relevant image from Pixabay/Pexels based on the topic.
                        Card(
                            colors = CardDefaults.cardColors(containerColor = darkSurface),
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cover Image", style = LuxeTypography.titleSmall.copy(color = gold))
                                    val coverThumb: Any? = if (coverImageUrl.isNotEmpty()) coverImageUrl else coverPreviewUri
                                    if (coverThumb != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AsyncImage(
                                            model = coverThumb,
                                            contentDescription = "Cover Image",
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            "Auto — random relevant image",
                                            style = LuxeTypography.bodySmall,
                                            color = tokens.editorTextSecondary
                                        )
                                    }
                                }
                                if (coverImageUrl.isNotEmpty() || coverPreviewUri != null) {
                                    IconButton(onClick = { coverImageUrl = ""; coverPreviewUri = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove cover", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { coverLauncher.launch("image/*") },
                                    enabled = !isCoverUploading,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                    border = BorderStroke(1.dp, gold)
                                ) {
                                    if (isCoverUploading) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = gold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Uploading…", style = LuxeTypography.labelMedium)
                                    } else {
                                        Text(if (coverImageUrl.isNotEmpty() || coverPreviewUri != null) "Change" else "Upload", style = LuxeTypography.labelMedium)
                                    }
                                }
                            }
                        }

                        // ── Back Cover Image upload slot ────────────────────────
                        Card(
                            colors = CardDefaults.cardColors(containerColor = darkSurface),
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Back Cover Image", style = LuxeTypography.titleSmall.copy(color = gold))
                                    val backThumb: Any? = if (backCoverImageUrl.isNotEmpty()) backCoverImageUrl else backCoverPreviewUri
                                    if (backThumb != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AsyncImage(
                                            model = backThumb,
                                            contentDescription = "Back Cover Image",
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            "Auto — random relevant image",
                                            style = LuxeTypography.bodySmall,
                                            color = tokens.editorTextSecondary
                                        )
                                    }
                                }
                                if (backCoverImageUrl.isNotEmpty() || backCoverPreviewUri != null) {
                                    IconButton(onClick = { backCoverImageUrl = ""; backCoverPreviewUri = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove back cover", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { backCoverLauncher.launch("image/*") },
                                    enabled = !isBackCoverUploading,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                    border = BorderStroke(1.dp, gold)
                                ) {
                                    if (isBackCoverUploading) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = gold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Uploading…", style = LuxeTypography.labelMedium)
                                    } else {
                                        Text(if (backCoverImageUrl.isNotEmpty() || backCoverPreviewUri != null) "Change" else "Upload", style = LuxeTypography.labelMedium)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val pagesToPass = when (pageSelection) {
                                    "Auto" -> -1
                                    "Short" -> 3
                                    "Medium" -> 8
                                    "Long" -> 13
                                    else -> -1
                                }
                                onGenerateBrief(prompt, referenceImages.toList(), pagesToPass)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = obsidian),
                            enabled = prompt.isNotBlank() && briefState !is BriefState.Loading
                        ) {
                            if (briefState is BriefState.Loading) {
                                CircularProgressIndicator(color = obsidian, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Generate Brief", style = LuxeTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            } else {
                // ASSISTED MODE
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Magazine Overall Theme") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tokens.editorText,
                            unfocusedTextColor = tokens.editorText,
                            focusedBorderColor = tokens.primaryAccent,
                            unfocusedBorderColor = tokens.editorBorder,
                            focusedLabelColor = tokens.primaryAccent,
                            unfocusedLabelColor = tokens.editorTextSecondary
                        )
                    )
                    
                    // Cover Art Card
                    var isUploadingCover by remember { mutableStateOf(false) }
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val coverLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            isUploadingCover = true
                            coroutineScope.launch {
                                try {
                                    val url = com.magazineforge.app.network.ApiClient.uploadImageWithRetry(context, uri)
                                    coverImageUrl = url
                                    snackbarHostState.showSnackbar("Cover image uploaded successfully")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar("Upload failed: ${e.localizedMessage ?: e.message}")
                                } finally {
                                    isUploadingCover = false
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = darkSurface),
                        border = BorderStroke(1.dp, tokens.secondaryAccent.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("FRONT COVER", style = LuxeTypography.labelMedium.copy(color = gold))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = coverTopic,
                                onValueChange = { coverTopic = it },
                                placeholder = { Text("What should be written on the cover?") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = gold,
                                    unfocusedBorderColor = tokens.editorBorder,
                                    unfocusedTextColor = tokens.editorText,
                                    focusedTextColor = tokens.editorText,
                                    unfocusedPlaceholderColor = tokens.editorTextSecondary,
                                    focusedPlaceholderColor = tokens.editorTextSecondary
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = coverImageUrl,
                                onValueChange = { newValue -> 
                                    // Auto verify and convert Google Drive links
                                    val driveRegex = Regex("https://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)/view.*")
                                    val convertedUrl = driveRegex.replace(newValue) { result ->
                                        "https://drive.google.com/uc?export=download&id=${result.groupValues[1]}"
                                    }
                                    coverImageUrl = convertedUrl
                                },
                                placeholder = { Text("Image URL or Google Drive Link") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = "Image", tint = tokens.editorTextSecondary) },
                                trailingIcon = {
                                    if (isUploadingCover) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = gold)
                                    } else {
                                        IconButton(onClick = { coverLauncher.launch("image/*") }) {
                                            Icon(Icons.Default.Add, contentDescription = "Upload from device", tint = gold)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = gold,
                                    unfocusedBorderColor = tokens.editorBorder,
                                    unfocusedTextColor = tokens.editorText,
                                    focusedTextColor = tokens.editorText,
                                    unfocusedPlaceholderColor = tokens.editorTextSecondary,
                                    focusedPlaceholderColor = tokens.editorTextSecondary
                                )
                            )
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(pages) { page ->
                            PageBlockCard(
                                page = page,
                                onUpdate = { updatedPage ->
                                    val index = pages.indexOfFirst { it.id == updatedPage.id }
                                    if (index != -1) pages[index] = updatedPage
                                },
                                onDelete = {
                                    pages.remove(page)
                                },
                                onShowSnackbar = { message ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        item {
                            val hasBackCover = pages.any { it.type == "back_cover" }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { pages.add(PageBlock(type = "article")) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                    border = BorderStroke(1.dp, gold),
                                    enabled = !hasBackCover
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Article")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Article")
                                }
                                
                                OutlinedButton(
                                    onClick = { pages.add(PageBlock(type = "back_cover")) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                    border = BorderStroke(1.dp, gold),
                                    enabled = !hasBackCover
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Back Cover")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Back Cover")
                                }
                            }
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { showComposer = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                        border = BorderStroke(1.dp, gold)
                    ) {
                        Text("Customize Sections ▾", style = LuxeTypography.titleSmall)
                    }
                    
                    Button(
                        onClick = { 
                            val coverBlock = PageBlock(type = "cover", topic = coverTopic, imageUrl = coverImageUrl)
                            val allPages = listOf(coverBlock) + pages
                            onCompileClicked(prompt, allPages, composerConfig, coverImageUrl) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = obsidian),
                        enabled = prompt.isNotBlank() && coverTopic.isNotBlank() && !isCompileLoading
                    ) {
                        if (isCompileLoading) {
                            CircularProgressIndicator(color = obsidian, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Generate Custom Issue", style = LuxeTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Progress Tracking Dialogs
        val isBriefFlowActive = briefState is BriefState.Loading || briefState is BriefState.Error
        val isCompileFlowActive = schemaState is SchemaState.Loading || schemaState is SchemaState.Error ||
                latexState is LatexState.Loading || latexState is LatexState.Error ||
                compileState is CompileState.Loading || compileState is CompileState.Error

        if (isBriefFlowActive) {
            AlertDialog(
                onDismissRequest = { /* Modal */ },
                containerColor = tokens.surface,
                title = {
                    Text(
                        text = "Generating Brief",
                        style = LuxeTypography.headlineSmall.copy(color = tokens.primaryAccent, fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            if (briefState is BriefState.Loading) {
                                Column(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = tokens.primaryAccent,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = loadingText, color = tokens.textPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = loadingProgress,
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = tokens.primaryAccent,
                                        trackColor = tokens.editorBorder
                                    )
                                }
                            } else if (briefState is BriefState.Error) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Failed",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Content Curation (LLM API)",
                                    color = Color.Red
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = Color.Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Content Curation (LLM API)",
                                    color = tokens.textPrimary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Pending",
                                tint = tokens.editorTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Brief Outline Compilation",
                                color = tokens.editorTextSecondary
                            )
                        }

                        if (briefState is BriefState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = briefState.message,
                                color = Color.Red,
                                style = LuxeTypography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    if (briefState is BriefState.Error) {
                        TextButton(
                            onClick = {
                                val pagesToPass = when (pageSelection) {
                                    "Auto" -> -1
                                    "Short" -> 3
                                    "Medium" -> 8
                                    "Long" -> 13
                                    else -> -1
                                }
                                onGenerateBrief(prompt, referenceImages.toList(), pagesToPass)
                            }
                        ) {
                            Text("Retry", color = tokens.primaryAccent)
                        }
                    }
                },
                dismissButton = {
                    if (briefState is BriefState.Error) {
                        TextButton(
                            onClick = onCancel
                        ) {
                            Text("Cancel", color = tokens.editorTextSecondary)
                        }
                    }
                }
            )
        } else if (isCompileFlowActive) {
            AlertDialog(
                onDismissRequest = { /* Modal */ },
                containerColor = tokens.surface,
                title = {
                    Text(
                        text = "Generating Magazine",
                        style = LuxeTypography.headlineSmall.copy(color = tokens.primaryAccent, fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val step1Status = when (schemaState) {
                            is SchemaState.Loading -> "loading"
                            is SchemaState.Error -> "error"
                            is SchemaState.Success -> "success"
                            else -> "pending"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            when (step1Status) {
                                "loading" -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = tokens.primaryAccent, strokeWidth = 2.dp)
                                "error" -> Icon(Icons.Default.Close, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(20.dp))
                                "success" -> Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Green, modifier = Modifier.size(20.dp))
                                else -> Icon(Icons.Default.Check, contentDescription = "Pending", tint = tokens.editorTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            val curationText = if (runState is com.magazineforge.app.ui.GenerationRunState.Active && runState.status.totalSections > 0) {
                                "Content Curation (${runState.status.completedSections}/${runState.status.totalSections})"
                            } else {
                                "Content Curation (LLM API)"
                            }
                            Text(
                                text = curationText,
                                color = if (step1Status == "error") Color.Red else if (step1Status == "loading") tokens.textPrimary else tokens.textPrimary
                            )
                        }

                        val step2Status = when (latexState) {
                            is LatexState.Loading -> "loading"
                            is LatexState.Error -> "error"
                            is LatexState.Success -> "success"
                            else -> "pending"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            when (step2Status) {
                                "loading" -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = tokens.primaryAccent, strokeWidth = 2.dp)
                                "error" -> Icon(Icons.Default.Close, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(20.dp))
                                "success" -> Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Green, modifier = Modifier.size(20.dp))
                                else -> Icon(Icons.Default.Check, contentDescription = "Pending", tint = tokens.editorTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "LaTeX Code Synthesis",
                                color = if (step2Status == "error") Color.Red else if (step2Status == "loading") tokens.textPrimary else tokens.textPrimary
                            )
                        }

                        val step3Status = when (compileState) {
                            is CompileState.Loading -> "loading"
                            is CompileState.Error -> "error"
                            is CompileState.Success -> "success"
                            else -> "pending"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            when (step3Status) {
                                "loading" -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = tokens.primaryAccent, strokeWidth = 2.dp)
                                "error" -> Icon(Icons.Default.Close, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(20.dp))
                                "success" -> Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Green, modifier = Modifier.size(20.dp))
                                else -> Icon(Icons.Default.Check, contentDescription = "Pending", tint = tokens.editorTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Cloud Compile & Download",
                                color = if (step3Status == "error") Color.Red else if (step3Status == "loading") tokens.textPrimary else tokens.textPrimary
                            )
                        }

                        if (compileState is CompileState.Loading && compileState.message.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = compileState.message,
                                color = tokens.primaryAccent,
                                style = LuxeTypography.bodySmall
                            )
                        }

                        val errorMsg = when {
                            schemaState is SchemaState.Error -> schemaState.message
                            latexState is LatexState.Error -> latexState.message
                            compileState is CompileState.Error -> compileState.message
                            else -> null
                        }
                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMsg,
                                color = Color.Red,
                                style = LuxeTypography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    val hasError = schemaState is SchemaState.Error || latexState is LatexState.Error || compileState is CompileState.Error
                    if (hasError) {
                        TextButton(
                            onClick = {
                                if (selectedTabIndex == 0) {
                                    val brief = (briefState as? BriefState.Success)?.brief
                                    if (brief != null) {
                                        onCompileFromBrief(prompt, composerConfig, brief, coverImageUrl, backCoverImageUrl, referenceImages.toList())
                                    }
                                } else {
                                    onCompileClicked(prompt, pages, composerConfig, coverImageUrl)
                                }
                            }
                        ) {
                            Text("Retry", color = tokens.primaryAccent)
                        }
                    }
                },
                dismissButton = {
                    val hasError = schemaState is SchemaState.Error || latexState is LatexState.Error || compileState is CompileState.Error
                    if (hasError) {
                        TextButton(
                            onClick = onCancel
                        ) {
                            Text("Cancel", color = tokens.editorTextSecondary)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PageBlockCard(
    page: PageBlock, 
    onUpdate: (PageBlock) -> Unit, 
    onDelete: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    val darkSurface = tokens.surface
    val gold = tokens.primaryAccent
    
    var showCustomizer by remember { mutableStateOf(false) }

    if (showCustomizer) {
        CustomizerBottomSheet(
            page = page,
            onDismiss = { showCustomizer = false },
            onSave = { onUpdate(it) }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = darkSurface),
        border = BorderStroke(1.dp, tokens.secondaryAccent.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val blockTitle = when(page.type) {
                    "article" -> "ARTICLE"
                    "back_cover" -> "BACK COVER"
                    else -> page.type.uppercase()
                }
                Text(blockTitle, style = LuxeTypography.labelMedium.copy(color = gold))
                Row {
                    IconButton(onClick = { showCustomizer = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Customize", tint = tokens.editorTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }
            
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = rememberCoroutineScope()
            var isUploading by remember { mutableStateOf(false) }
            val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    isUploading = true
                    scope.launch {
                        try {
                            val fullUrl = com.magazineforge.app.network.ApiClient.uploadImageWithRetry(context, uri)
                            onUpdate(page.copy(imageUrl = fullUrl))
                            onShowSnackbar("Image uploaded successfully!")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onShowSnackbar("Upload failed: ${e.localizedMessage ?: e.message}")
                        } finally {
                            isUploading = false
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = page.topic,
                onValueChange = { onUpdate(page.copy(topic = it)) },
                placeholder = { Text("What is this page about?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = gold,
                    unfocusedBorderColor = tokens.editorBorder,
                    unfocusedTextColor = tokens.editorText,
                    focusedTextColor = tokens.editorText,
                    unfocusedPlaceholderColor = tokens.editorTextSecondary,
                    focusedPlaceholderColor = tokens.editorTextSecondary
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = page.imageUrl,
                onValueChange = { newValue -> 
                    // Auto verify and convert Google Drive links
                    val driveRegex = Regex("https://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)/view.*")
                    val convertedUrl = driveRegex.replace(newValue) { result ->
                        "https://drive.google.com/uc?export=download&id=${result.groupValues[1]}"
                    }
                    onUpdate(page.copy(imageUrl = convertedUrl))
                },
                placeholder = { Text("Image URL or Google Drive Link") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = "Image", tint = tokens.editorTextSecondary) },
                trailingIcon = {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = gold)
                    } else {
                        IconButton(onClick = { launcher.launch("image/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Upload from device", tint = gold)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = gold,
                    unfocusedBorderColor = tokens.editorBorder,
                    unfocusedTextColor = tokens.editorText,
                    focusedTextColor = tokens.editorText,
                    unfocusedPlaceholderColor = tokens.editorTextSecondary,
                    focusedPlaceholderColor = tokens.editorTextSecondary
                )
            )
        }
    }
}
