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
                    var apiKey by remember { mutableStateOf(savedKey ?: "") }
                    var selectedPdfForViewer by remember { mutableStateOf<String?>(null) }
                    
                    val coroutineScope = rememberCoroutineScope()
                    var isVerifying by remember { mutableStateOf(false) }
                    var verifyError by remember { mutableStateOf<String?>(null) }
                    
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
                                when (currentScreen) {
                                    "onboarding" -> OnboardingScreen(
                                        onVerifyClicked = { key ->
                                            isVerifying = true
                                            verifyError = null
                                            coroutineScope.launch {
                                                try {
                                                    val response = withContext(Dispatchers.IO) {
                                                        ApiClient.retrofitService.verifyKey(VerifyKeyRequest(key))
                                                    }
                                                    if (response.isSuccessful && response.body()?.valid == true) {
                                                        secureStorage.saveApiKey(key)
                                                        apiKey = key
                                                        currentScreen = "showcase"
                                                    } else {
                                                        verifyError = "Invalid API Key"
                                                    }
                                                } catch (e: Exception) {
                                                    verifyError = "Connection Error: ${e.message}"
                                                } finally {
                                                    isVerifying = false
                                                }
                                            }
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
                                        onTemplateSelected = { template ->
                                            selectedTemplate = template
                                            currentScreen = "editor"
                                        },
                                        onPreviewSelected = { templateVariant ->
                                            viewModel.generateSchema(apiKey, "Magazine Preview", templateVariant)
                                        },
                                        onLibraryClicked = {
                                            currentScreen = "library"
                                        }
                                    )
                                    "editor" -> EditorScreen(
                                        templateVariant = selectedTemplate,
                                        isCompileLoading = isCompileLoading,
                                        onCompileClicked = { magazineTopic, pages ->
                                            val finalTopic = if (pages.isEmpty()) {
                                                magazineTopic
                                            } else {
                                                val pagesStr = pages.joinToString("\n") { 
                                                    "Page Type: ${it.type}, Topic: ${it.topic}, Target Image URL: ${it.imageUrl}" 
                                                }
                                                "Theme: $magazineTopic\nRequired Structure:\n$pagesStr\n(CRITICAL INSTRUCTION: You MUST use the exact Target Image URLs provided above for each corresponding page. Do NOT override them with Unsplash URLs unless no Target Image URL was provided.)"
                                            }
                                            viewModel.generateSchema(apiKey, finalTopic, selectedTemplate)
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
                                        if (latexState is LatexState.Success) {
                                            LatexNotebookScreen(
                                                initialLatex = (latexState as LatexState.Success).latexCode,
                                                isCompiling = compileState is CompileState.Loading,
                                                onCompile = { latex ->
                                                    viewModel.compileRaw(this@MainActivity, latex)
                                                },
                                                onBack = {
                                                    currentScreen = "co_author"
                                                }
                                            )
                                        }
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
                                        onSaveApiKey = { newKey ->
                                            isVerifying = true
                                            verifyError = null
                                            coroutineScope.launch {
                                                try {
                                                    val response = withContext(Dispatchers.IO) {
                                                        ApiClient.retrofitService.verifyKey(VerifyKeyRequest(newKey))
                                                    }
                                                    if (response.isSuccessful && response.body()?.valid == true) {
                                                        secureStorage.saveApiKey(newKey)
                                                        apiKey = newKey
                                                    } else {
                                                        verifyError = "Invalid API Key"
                                                    }
                                                } catch (e: Exception) {
                                                    verifyError = "Connection Error"
                                                } finally {
                                                    isVerifying = false
                                                }
                                            }
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
