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
import com.magazineforge.app.ui.AuthScreen
import com.google.firebase.auth.FirebaseAuth
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
import com.magazineforge.app.ui.theme.PitchBlack
import com.magazineforge.app.ui.theme.DarkSurface
import com.magazineforge.app.ui.theme.EditorialGold
import com.magazineforge.app.ui.theme.GoldBright
import com.magazineforge.app.ui.theme.GhostWhite
import com.magazineforge.app.ui.theme.AshGrey
import com.magazineforge.app.ui.theme.ErrorRed
import com.magazineforge.app.ui.theme.BorderDark
import com.magazineforge.app.ui.theme.BorderLight

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

        setContent {
            MagazineForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(if (savedKey != null) "showcase" else "onboarding") }
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
                                    containerColor = DarkSurface,
                                    contentColor = GhostWhite
                                ) {
                                    val items = listOf(
                                        Triple("showcase", "Home", Icons.Default.Home),
                                        Triple("editor", "Studio", Icons.Default.Brush),
                                        Triple("gallery", "Gallery", Icons.Default.PhotoLibrary),
                                        Triple("library", "Library", Icons.Default.Collections),
                                        Triple("settings", "Settings", Icons.Default.Settings)
                                    )
                                    items.forEach { (route, label, icon) ->
                                        NavigationBarItem(
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, style = LuxeTypography.labelSmall) },
                                            selected = currentScreen == route,
                                            onClick = { currentScreen = route },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = DarkSurface,
                                                unselectedIconColor = AshGrey,
                                                selectedTextColor = EditorialGold,
                                                unselectedTextColor = AshGrey,
                                                indicatorColor = EditorialGold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                                        onVerifyClicked = { key ->
                                            isVerifying = true
                                            verifyError = null
                                            coroutineScope.launch {
                                                try {
                                                    if (key.startsWith("AQ") || key.startsWith("AIza")) {
                                                        secureStorage.saveApiKey(key)
                                                        apiKey = key
                                                        currentScreen = "showcase"
                                                        return@launch
                                                    }

                                                    val response = withContext(Dispatchers.IO) {
                                                        ApiClient.retrofitService.verifyKey(VerifyKeyRequest(key))
                                                    }
                                                    if (response.isSuccessful) {
                                                        if (response.body()?.valid == true) {
                                                            secureStorage.saveApiKey(key)
                                                            apiKey = key
                                                            currentScreen = "showcase"
                                                        } else {
                                                            verifyError = "Invalid API Key"
                                                        }
                                                    } else {
                                                        if (response.code() in 400..499) {
                                                            verifyError = "Invalid API Key"
                                                        } else {
                                                            verifyError = "Server Booting... Try again."
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    verifyError = "Connection Error: ${e.message}"
                                                } finally {
                                                    isVerifying = false
                                                }
                                            }
                                        },
                                        onOpenEditorClicked = {
                                            currentScreen = "latex_notebook"
                                        },
                                        isVerifying = isVerifying,
                                        verifyError = verifyError
                                    )
                                    "showcase" -> ShowcaseScreen(
                                        viewModel = viewModel,
                                        onMagazineSelected = { url ->
                                            selectedPdfForViewer = url
                                        }
                                    )
                                    "gallery" -> TemplateGalleryScreen(
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
                                            if (FirebaseAuth.getInstance().currentUser != null) {
                                                // Ideally, upload PDF to Firebase Storage here.
                                                // For v2.0.0 frontend UI, we route to Showcase.
                                                currentScreen = "showcase"
                                            } else {
                                                currentScreen = "auth"
                                            }
                                        },
                                        onEditorClicked = {
                                            currentScreen = "latex_notebook"
                                        }
                                    )
                                    "auth" -> AuthScreen(
                                        onAuthSuccess = {
                                            currentScreen = "showcase"
                                        },
                                        onBack = {
                                            currentScreen = "gallery"
                                        }
                                    )
                                    "editor" -> EditorScreen(
                                        templateVariant = selectedTemplate,
                                        templateName = selectedTemplateName,
                                        initialPrompt = initialEditorPrompt,
                                        isCompileLoading = schemaState is SchemaState.Loading,
                                        briefState = briefState,
                                        onGenerateBrief = { prompt ->
                                            viewModel.generateBrief(apiKey, backupApiKey, prompt)
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
                                            onCompile = { code ->
                                                viewModel.compileRaw(applicationContext, code)
                                            },
                                            onBack = { currentScreen = "gallery" },
                                            onCodeChange = { code ->
                                                viewModel.updateLatexCode(code)
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
                                                    val response = withContext(Dispatchers.IO) {
                                                        ApiClient.retrofitService.verifyKey(VerifyKeyRequest(newKey))
                                                    }
                                                    if (response.isSuccessful && response.body()?.valid == true) {
                                                        secureStorage.saveApiKey(newKey)
                                                        apiKey = newKey
                                                        secureStorage.saveBackupApiKey(newBackupKey)
                                                        backupApiKey = newBackupKey
                                                        verifySuccess = true
                                                    } else {
                                                        verifyError = "Invalid Primary API Key"
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
                                            containerColor = DarkSurface
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
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
                                                color = EditorialGold,
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Compiling... ${loadingState.progress}%",
                                                    style = LuxeTypography.labelMedium.copy(color = GhostWhite)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = loadingState.message,
                                                    style = LuxeTypography.labelSmall.copy(color = AshGrey),
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
                                                    tint = GhostWhite
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
                                            Text("Exit", style = LuxeTypography.labelMedium.copy(color = ErrorRed))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showExitDialog = false }) {
                                            Text("Cancel", style = LuxeTypography.labelMedium.copy(color = GhostWhite))
                                        }
                                    },
                                    containerColor = DarkSurface,
                                    titleContentColor = GhostWhite,
                                    textContentColor = AshGrey
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
