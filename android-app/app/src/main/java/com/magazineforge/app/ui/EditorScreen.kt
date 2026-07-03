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
    var colorPalette: String = "Dark Mode",
    var imageStyle: String = "Photorealistic",
    var layoutDensity: String = "Balanced",
    var targetAudience: String = "General"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    templateVariant: String,
    templateName: String = "",
    initialPrompt: String = "",
    isCompileLoading: Boolean,
    onCompileClicked: (String, List<PageBlock>) -> Unit,
    onBack: () -> Unit
) {
    var prompt by remember { mutableStateOf(initialPrompt) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pages = remember { mutableStateListOf<PageBlock>() }
    
    val obsidian = PitchBlack
    val darkSurface = DarkSurface
    val gold = EditorialGold
    val borderCol = BorderDark
    val ivory = GhostWhite

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
                Column(modifier = Modifier.weight(1f).padding(24.dp)) {
                    TextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { 
                            Text("What are we publishing today?", color = ivory.copy(alpha = 0.4f), style = LuxeTypography.headlineMedium) 
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        textStyle = LuxeTypography.headlineMedium.copy(color = ivory),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = gold
                        )
                    )
                    
                    Button(
                        onClick = {
                            val dummyPages = listOf(
                                PageBlock(type = "cover", topic = prompt),
                                PageBlock(type = "toc"),
                                PageBlock(type = "article", topic = "Main Feature")
                            )
                            onCompileClicked(prompt, dummyPages)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = obsidian),
                        enabled = prompt.isNotBlank() && !isCompileLoading
                    ) {
                        if (isCompileLoading) {
                            CircularProgressIndicator(color = obsidian, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Generate Issue", style = LuxeTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
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
                    
                    Button(
                        onClick = { onCompileClicked(prompt, pages) },
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
