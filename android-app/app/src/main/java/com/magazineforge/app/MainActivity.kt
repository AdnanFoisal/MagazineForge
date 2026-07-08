package com.magazineforge.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.magazineforge.app.ui.CompileState
import com.magazineforge.app.ui.SchemaState
import com.magazineforge.app.ui.LatexState
import com.magazineforge.app.ui.CoAuthorScreen
import com.magazineforge.app.ui.LatexNotebookScreen
import com.magazineforge.app.ui.EditorScreen
import com.magazineforge.app.ui.EditorViewModel
import com.magazineforge.app.ui.OnboardingScreen
import com.magazineforge.app.ui.PdfViewerScreen
import com.magazineforge.app.ui.TemplateGalleryScreen
import com.magazineforge.app.ui.MyMagazinesScreen
import com.magazineforge.app.ui.ShowcaseScreen
import com.magazineforge.app.ui.SettingsScreen
import com.magazineforge.app.ui.ProgressTrackerDialog
import com.magazineforge.app.ui.FloatingProgressTracker

import com.magazineforge.app.utils.SecureStorage
import com.magazineforge.app.network.ApiClient
import com.magazineforge.app.models.VerifyKeyRequest
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import com.magazineforge.app.ui.HomeScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import com.magazineforge.app.ui.SettingsScreen
import com.magazineforge.app.ui.theme.MagazineForgeTheme
import com.magazineforge.app.ui.theme.LuxeTypography
import com.magazineforge.app.ui.theme.LocalThemeTokens


class MainActivity : ComponentActivity() {
    private lateinit var viewModel: EditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient { ApiClient.okHttpClient }
            .build()
        Coil.setImageLoader(imageLoader)
        
        viewModel = ViewModelProvider(this)[EditorViewModel::class.java]
        
        val secureStorage = SecureStorage(this)
        val savedKey = secureStorage.getApiKey()
        
        val savedThemeId = secureStorage.getThemeId()
        val initialTheme = com.magazineforge.app.ui.theme.AllThemes.find { it.id == savedThemeId } ?: com.magazineforge.app.ui.theme.SunsetEditorial
        com.magazineforge.app.ui.theme.ThemeState.setTheme(initialTheme)

        setContent {
            MagazineForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val tokens = LocalThemeTokens.current
                    
                    var currentScreen by remember { mutableStateOf(if (savedKey != null) "home" else "onboarding") }
                    var selectedTemplate by remember { mutableStateOf("") }
                    var selectedTemplateName by remember { mutableStateOf("") }
                    var initialEditorPrompt by remember { mutableStateOf("") }
                    var apiKey by remember { mutableStateOf(savedKey ?: "") }
                    var backupApiKey by remember { mutableStateOf(secureStorage.getBackupApiKey() ?: "") }
                    var selectedPdfForViewer by remember { mutableStateOf<String?>(null) }
                    
                    val coroutineScope = rememberCoroutineScope()
                    var isVerifying by remember { mutableStateOf(false) }
                    var verifyError by remember { mutableStateOf<String?>(null) }
                    var verifySuccess by remember { mutableStateOf<Boolean?>(null) }
                    
                    val briefState by viewModel.briefState.collectAsState()
                    val schemaState by viewModel.schemaState.collectAsState()
                    val latexState by viewModel.latexState.collectAsState()
                    val compileState by viewModel.compileState.collectAsState()
                    var showExitDialog by remember { mutableStateOf(false) }
                    var showProgressCard by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(schemaState) {
                        if (schemaState is SchemaState.Success) {
                            currentScreen = "co_author"
                        } else if (schemaState is SchemaState.Loading) {
                            showProgressCard = true
                        }
                    }

                    LaunchedEffect(latexState) {
                        if (latexState is LatexState.Success) {
                            showProgressCard = false
                            currentScreen = "latex_notebook"
                        } else if (latexState is LatexState.Loading) {
                            showProgressCard = true
                        }
                    }

                    LaunchedEffect(compileState) {
                        if (compileState is CompileState.Loading) {
                            showProgressCard = true
                        }
                    }

                    LaunchedEffect(currentScreen) {
                        verifyError = null
                        verifySuccess = null
                    }

                    Scaffold(
                        bottomBar = {
                            if (currentScreen != "onboarding" && selectedPdfForViewer == null && compileState !is CompileState.Success && compileState !is CompileState.Error) {
                                NavigationBar(
                                    containerColor = LocalThemeTokens.current.surface,
                                    contentColor = LocalThemeTokens.current.textSecondary
                                ) {
                                    val items = listOf(
                                        Triple("home", "Home", Icons.Default.Home),
                                        Triple("library", "Library", Icons.Default.Collections),
                                        Triple("showcase", "Showcase", Icons.Default.PhotoLibrary),
                                        Triple("templates", "Templates", Icons.Default.Brush),
                                        Triple("settings", "Settings", Icons.Default.Settings)
                                    )
                                    items.forEach { (route, label, icon) ->
                                        NavigationBarItem(
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, style = LocalThemeTokens.current.wordmarkFontFamily.let { LuxeTypography.labelSmall.copy(fontFamily = it) }) },
                                            selected = currentScreen == route,
                                            onClick = { currentScreen = route },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = LocalThemeTokens.current.surface,
                                                unselectedIconColor = LocalThemeTokens.current.textSecondary,
                                                selectedTextColor = LocalThemeTokens.current.primaryAccent,
                                                unselectedTextColor = LocalThemeTokens.current.textSecondary,
                                                indicatorColor = LocalThemeTokens.current.primaryAccent
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(LocalThemeTokens.current.screenBackground)) {
                            // FloatingProgressTracker added to the Box holding NavHost


                            if (selectedPdfForViewer != null) {
                                PdfViewerScreen(
                                    pdfUrlOrPath = selectedPdfForViewer!!,
                                    onBack = {
                                        selectedPdfForViewer = null
                                    }
                                )
                            } else if (compileState is CompileState.Success) {
                                PdfViewerScreen(
                                    pdfUrlOrPath = (compileState as CompileState.Success).pdfFile.absolutePath,
                                    onBack = {
                                        viewModel.resetState()
                                    }
                                )
                            } else if (compileState is CompileState.Error) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                        Text("Error: ${(compileState as CompileState.Error).message}", color = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { viewModel.resetState() }) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                            } else {
                                val isCompileLoading = compileState is CompileState.Loading
                                // State-driven navigation
                            LaunchedEffect(schemaState) {
                                if (schemaState is SchemaState.Success) {
                                    currentScreen = "co_author"
                                }
                            }

                            LaunchedEffect(latexState) {
                                if (latexState is LatexState.Success) {
                                    currentScreen = "latex_notebook"
                                }
                            }

                            when (currentScreen) {
                                    "onboarding" -> OnboardingScreen(
                                        onCreateClicked = {
                                            currentScreen = "home"
                                        },
                                        onExploreClicked = {
                                            currentScreen = "templates"
                                        }
                                    )
                                    "home" -> HomeScreen(
                                        viewModel = viewModel,
                                        onMagazineSelected = { url ->
                                            selectedPdfForViewer = url
                                        },
                                        onContinueEditing = {
                                            currentScreen = "editor"
                                        },
                                        onViewLibrary = {
                                            currentScreen = "library"
                                        }
                                    )
                                    "showcase" -> ShowcaseScreen(
                                        viewModel = viewModel,
                                        onMagazineSelected = { url ->
                                            selectedPdfForViewer = url
                                        }
                                    )
                                    "templates" -> TemplateGalleryScreen(
                                        onTemplateSelected = { template, description, name ->
                                            selectedTemplate = template
                                            initialEditorPrompt = description
                                            selectedTemplateName = name
                                            currentScreen = "editor"
                                        },
                                        onPreviewSelected = { templateVariant ->
                                            viewModel.generateSchema(apiKey, backupApiKey, "Magazine Preview", templateVariant)
                                        },
                                        onLibraryClicked = {
                                            currentScreen = "library"
                                        },
                                        onPublishClicked = {
                                            currentScreen = "showcase"
                                        },
                                        onEditorClicked = {
                                            currentScreen = "latex_notebook"
                                        }
                                    )
                                    "editor" -> EditorScreen(
                                        templateVariant = selectedTemplate,
                                        templateName = selectedTemplateName,
                                        initialPrompt = initialEditorPrompt,
                                        isCompileLoading = schemaState is SchemaState.Loading,
                                        briefState = briefState,
                                        onGenerateBrief = { prompt, referenceImages ->
                                            viewModel.generateBrief(apiKey, backupApiKey, prompt, referenceImages)
                                        },
                                        onCompileFromBrief = { prompt, config, brief ->
                                            viewModel.generateSchema(
                                                geminiKey = apiKey, 
                                                backupKey = backupApiKey, 
                                                magazineTopic = prompt, 
                                                templateName = selectedTemplate,
                                                config = config,
                                                tone = brief.tone,
                                                layoutDensity = brief.styleDna
                                            )
                                        },
                                        onCompileClicked = { magazineTopic, pages, config ->
                                            val finalTopic = if (pages.isEmpty()) {
                                                magazineTopic
                                            } else {
                                                val pagesStr = pages.joinToString("\n\n") { 
                                                    """
                                                    Page Type: ${it.type.uppercase()}
                                                    Topic: ${it.topic}
                                                    Target Image URL: ${it.imageUrl}
                                                    [CUSTOMIZATION CONFIGURATION]:
                                                    - Writing Tone: ${it.tone}
                                                    - Layout Density: ${it.layoutDensity}
                                                    """.trimIndent()
                                                }
                                                "Theme: $magazineTopic\n\nRequired Structure:\n$pagesStr\n\n(CRITICAL INSTRUCTION: You MUST strictly adhere to the [CUSTOMIZATION CONFIGURATION] for each page. Adapt your language, formatting, and generation to perfectly match the requested Tone and Layout Density. You MUST also use the exact Target Image URLs provided above for each corresponding page. Do NOT override them with Unsplash URLs unless no Target Image URL was provided.)"
                                            }
                                            viewModel.generateSchema(
                                                geminiKey = apiKey, 
                                                backupKey = backupApiKey, 
                                                magazineTopic = finalTopic, 
                                                templateName = selectedTemplate,
                                                config = config
                                            )
                                        },
                                        onBack = {
                                            currentScreen = "gallery"
                                        }
                                    )
                                    "co_author" -> {
                                        if (schemaState is SchemaState.Success) {
                                            CoAuthorScreen(
                                                initialSchema = (schemaState as SchemaState.Success).schema,
                                                isGeneratingLatex = latexState is LatexState.Loading,
                                                onGenerateLatex = { schema ->
                                                    viewModel.generateLatex(schema)
                                                },
                                                onBack = {
                                                    viewModel.resetState()
                                                    currentScreen = "editor"
                                                }
                                            )
                                        }
                                    }
                                    "latex_notebook" -> {
                                        LatexNotebookScreen(
                                            initialLatex = viewModel.getLatexCode() ?: (latexState as? LatexState.Success)?.latexCode ?: "",
                                            isCompiling = isCompileLoading,
                                            compileState = compileState,
                                            schemaState = schemaState,
                                            onCompile = { code ->
                                                viewModel.compileRaw(applicationContext, code)
                                            },
                                            onBack = { currentScreen = "gallery" },
                                            onCodeChange = { code ->
                                                viewModel.updateLatexCode(code)
                                            },
                                            onRewrite = { text, instruction, onResult ->
                                                viewModel.rewriteSelection(apiKey, backupApiKey.ifBlank { null }, text, instruction, onResult)
                                            }
                                        )
                                    }
                                    "library" -> MyMagazinesScreen(
                                        onBack = {
                                            currentScreen = "gallery"
                                        },
                                        onMagazineSelected = { pdfFile ->
                                            selectedPdfForViewer = pdfFile.absolutePath
                                        }
                                    )
                                    "settings" -> SettingsScreen(
                                        currentApiKey = apiKey,
                                        currentBackupApiKey = backupApiKey,
                                        isVerifying = isVerifying,
                                        verifyError = verifyError,
                                        verifySuccess = verifySuccess,
                                        onSaveApiKeys = { newKey, newBackupKey ->
                                            isVerifying = true
                                            verifyError = null
                                            verifySuccess = null
                                            coroutineScope.launch {
                                                try {
                                                    val primaryValid = withContext(Dispatchers.IO) {
                                                        try {
                                                            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models?key=$newKey")
                                                            val connection = url.openConnection() as java.net.HttpURLConnection
                                                            connection.requestMethod = "GET"
                                                            connection.responseCode == 200
                                                        } catch (e: Exception) {
                                                            false
                                                        }
                                                    }
                                                    val backupValid = withContext(Dispatchers.IO) {
                                                        if (newBackupKey.isBlank()) true else {
                                                            try {
                                                                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models?key=$newBackupKey")
                                                                val connection = url.openConnection() as java.net.HttpURLConnection
                                                                connection.requestMethod = "GET"
                                                                connection.responseCode == 200
                                                            } catch (e: Exception) {
                                                                false
                                                            }
                                                        }
                                                    }
                                                    
                                                    if (primaryValid && backupValid) {
                                                        secureStorage.saveApiKey(newKey)
                                                        apiKey = newKey
                                                        secureStorage.saveBackupApiKey(newBackupKey)
                                                        backupApiKey = newBackupKey
                                                        verifySuccess = true
                                                    } else {
                                                        verifyError = if (!primaryValid) "Invalid Primary API Key" else "Invalid Backup API Key"
                                                        verifySuccess = false
                                                    }
                                                } catch (e: Exception) {
                                                    verifyError = "Connection Error"
                                                    verifySuccess = false
                                                } finally {
                                                    isVerifying = false
                                                }
                                            }
                                        },
                                        onClearFeedback = {
                                            verifyError = null
                                            verifySuccess = null
                                        }
                                    )
                                }
                            }

                            // Floating Progress Card Overlay
                            if (compileState is CompileState.Loading && showProgressCard) {
                                val loadingState = compileState as CompileState.Loading
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = tokens.surface
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, tokens.secondaryAccent.copy(alpha = 0.3f)),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                progress = loadingState.progress / 100f,
                                                color = tokens.primaryAccent,
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Compiling... ${loadingState.progress}%",
                                                    style = LuxeTypography.labelMedium.copy(color = tokens.textPrimary)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = loadingState.message,
                                                    style = LuxeTypography.labelSmall.copy(color = tokens.textSecondary),
                                                    maxLines = 1
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { showProgressCard = false },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = tokens.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            FloatingProgressTracker(
                                schemaState = schemaState,
                                latexState = latexState,
                                compileState = compileState
                            )

                            // Back handler for root and sub-screens
                            BackHandler(enabled = true) {
                                if (compileState is CompileState.Success) {
                                    viewModel.resetState()
                                } else if (compileState is CompileState.Error) {
                                    viewModel.resetState()
                                } else if (selectedPdfForViewer != null) {
                                    selectedPdfForViewer = null
                                } else {
                                    when (currentScreen) {
                                        "editor", "library", "gallery" -> {
                                            currentScreen = "showcase"
                                        }
                                        "co_author" -> {
                                            viewModel.resetState()
                                            currentScreen = "editor"
                                        }
                                        "latex_notebook" -> {
                                            currentScreen = "co_author"
                                        }
                                        "showcase", "onboarding" -> {
                                            showExitDialog = true
                                        }
                                    }
                                }
                            }

                            // Exit confirmation dialog
                            if (showExitDialog) {
                                AlertDialog(
                                    onDismissRequest = { showExitDialog = false },
                                    title = { Text("Exit MagazineForge", style = LuxeTypography.headlineSmall) },
                                    text = { Text("Are you sure you want to exit the application?", style = LuxeTypography.bodyMedium) },
                                    confirmButton = {
                                        TextButton(onClick = { finish() }) {
                                            Text("Exit", style = LuxeTypography.labelMedium.copy(color = tokens.primaryAccent))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showExitDialog = false }) {
                                            Text("Cancel", style = LuxeTypography.labelMedium.copy(color = tokens.textPrimary))
                                        }
                                    },
                                    containerColor = tokens.surface,
                                    titleContentColor = tokens.textPrimary,
                                    textContentColor = tokens.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
