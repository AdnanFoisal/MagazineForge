package com.magazineforge.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.ui.theme.BorderDark
import com.magazineforge.app.ui.theme.DarkSurface
import com.magazineforge.app.ui.theme.EditorialGold
import com.magazineforge.app.ui.theme.GhostWhite
import com.magazineforge.app.ui.theme.LuxeTypography
import com.magazineforge.app.ui.theme.PitchBlack

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

import com.magazineforge.app.models.GenerateBriefResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    templateVariant: String,
    templateName: String = "",
    initialPrompt: String = "",
    isCompileLoading: Boolean,
    briefState: BriefState,
    onGenerateBrief: (String, List<String>) -> Unit,
    onCompileFromBrief: (String, SectionComposerConfig, GenerateBriefResponse) -> Unit,
    onCompileClicked: (String, List<PageBlock>, SectionComposerConfig) -> Unit,
    onBack: () -> Unit
) {
    var prompt by remember { mutableStateOf(initialPrompt) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pages = remember { mutableStateListOf<PageBlock>() }
    var composerConfig by remember { mutableStateOf(SectionComposerConfig()) }
    var showComposer by remember { mutableStateOf(false) }

    val obsidian = PitchBlack
    val darkSurface = DarkSurface
    val gold = EditorialGold
    val borderCol = BorderDark
    val ivory = GhostWhite

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = obsidian)
            )
        },
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
                                Text("Category: ${brief.category}", style = LuxeTypography.titleMedium.copy(color = ivory))
                                Text("Tone: ${brief.tone}", style = LuxeTypography.titleMedium.copy(color = ivory))
                                Text("Layout Density: ${brief.styleDna}", style = LuxeTypography.titleMedium.copy(color = ivory))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Potential Titles:", style = LuxeTypography.titleMedium.copy(color = gold))
                                brief.titles.forEach { title ->
                                    Text("- $title", style = LuxeTypography.bodyMedium.copy(color = ivory))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Articles:", style = LuxeTypography.titleMedium.copy(color = gold))
                                brief.articles.forEach { article ->
                                    Text("- ${article.topic}", style = LuxeTypography.bodyMedium.copy(color = ivory))
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
                                onCompileFromBrief(prompt, composerConfig, brief)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 32.dp),
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
                    val referenceImages = remember { mutableStateListOf<String>() }
                    var showReferenceImages by remember { mutableStateOf(false) }
                    
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
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val tempFile = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                                    val outputStream = java.io.FileOutputStream(tempFile)
                                    inputStream?.copyTo(outputStream)
                                    inputStream?.close()
                                    outputStream.close()
                                    
                                    val requestFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), tempFile)
                                    val body = okhttp3.MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                                    
                                    val response = com.magazineforge.app.network.ApiClient.retrofitService.uploadAsset(body)
                                    if (response.isSuccessful) {
                                        val path = response.body()?.url ?: ""
                                        if (path.isNotEmpty()) {
                                            referenceImages.add(path)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isUploading = false
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
                                    style = LuxeTypography.headlineMedium.copy(color = borderCol)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            textStyle = LuxeTypography.headlineMedium.copy(color = ivory),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = gold
                            )
                        )
                        
                        // Reference Images Disclosure
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showReferenceImages = !showReferenceImages }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Add reference images ${if (showReferenceImages) "▴" else "▾"}", style = LuxeTypography.titleSmall.copy(color = gold))
                            }
                            
                            if (showReferenceImages) {
                                referenceImages.forEachIndexed { index, img ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Image ${index + 1}", color = ivory)
                                        IconButton(onClick = { referenceImages.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = { launcher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    enabled = !isUploading,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                    border = BorderStroke(1.dp, gold)
                                ) {
                                    if (isUploading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = gold)
                                    } else {
                                        Text("Upload Image")
                                    }
                                }
                            }
                        }
                        
                        if (briefState is BriefState.Error) {
                            Text(
                                text = briefState.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = {
                                onGenerateBrief(prompt, referenceImages.toList())
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
                            focusedBorderColor = gold,
                            focusedLabelColor = gold,
                            unfocusedTextColor = ivory,
                            focusedTextColor = ivory
                        )
                    )

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
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        item {
                            OutlinedButton(
                                onClick = { pages.add(PageBlock()) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                border = BorderStroke(1.dp, gold)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Page")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Page Block")
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
                        onClick = { onCompileClicked(prompt, pages, composerConfig) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = obsidian),
                        enabled = prompt.isNotBlank() && pages.isNotEmpty() && !isCompileLoading
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
    }
}

@Composable
fun PageBlockCard(
    page: PageBlock,
    onUpdate: (PageBlock) -> Unit,
    onDelete: () -> Unit
) {
    var showCustomizer by remember { mutableStateOf(false) }

    if (showCustomizer) {
        CustomizerBottomSheet(
            page = page,
            onDismiss = { showCustomizer = false },
            onSave = { onUpdate(it) }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(page.type.uppercase(), style = LuxeTypography.labelMedium.copy(color = EditorialGold))
                Row {
                    IconButton(onClick = { showCustomizer = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Customize", tint = GhostWhite.copy(alpha = 0.7f))
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
                                val path = response.body()?.url ?: ""
                                if (path.isNotEmpty()) {
                                    val fullUrl = "${com.magazineforge.app.network.ApiClient.BASE_URL}$path"
                                    onUpdate(page.copy(imageUrl = fullUrl))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
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
                    focusedBorderColor = EditorialGold,
                    unfocusedTextColor = GhostWhite,
                    focusedTextColor = GhostWhite
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
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = "Image") },
                trailingIcon = {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EditorialGold)
                    } else {
                        IconButton(onClick = { launcher.launch("image/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Upload from device", tint = EditorialGold)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EditorialGold,
                    unfocusedTextColor = GhostWhite,
                    focusedTextColor = GhostWhite
                )
            )
        }
    }
}
