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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.foundation.background
import com.magazineforge.app.ui.HomeScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Style
import com.magazineforge.app.ui.SettingsScreen
import androidx.compose.foundation.ExperimentalFoundationApi
import com.magazineforge.app.ui.SplashScreen
import com.magazineforge.app.ui.theme.MagazineForgeTheme
import com.magazineforge.app.ui.theme.LuxeTypography
import com.magazineforge.app.ui.theme.LocalThemeTokens


import coil.ImageLoader
import coil.Coil
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@OptIn(ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: EditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure Coil globally to use our custom OkHttpClient (which injects HF_TOKEN)
        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient(ApiClient.okHttpClient)
            .build()
        Coil.setImageLoader(imageLoader)
        
        viewModel = ViewModelProvider(this)[EditorViewModel::class.java]
        
        val secureStorage = SecureStorage(this)
        val savedUrl = secureStorage.getLiteLLMUrl()
        
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
                    val context = androidx.compose.ui.platform.LocalContext.current
                    
                    var currentScreen by remember { mutableStateOf("splash") }
                    var selectedTemplate by remember { mutableStateOf("") }
                    var selectedTemplateName by remember { mutableStateOf("") }
                    var initialEditorPrompt by remember { mutableStateOf("") }
                    val secureStorage = remember { SecureStorage(context) }
                    var litellmUrl by remember { mutableStateOf(secureStorage.getLiteLLMUrl() ?: "") }
                    var litellmKey by remember { mutableStateOf(secureStorage.getLiteLLMKey() ?: "") }

                    // Reload credentials whenever we enter the editor or AI modes to ensure we have the latest
                    LaunchedEffect(currentScreen) {
                        litellmUrl = secureStorage.getLiteLLMUrl() ?: ""
                        litellmKey = secureStorage.getLiteLLMKey() ?: ""
                    }
                    var selectedPdfForViewer by remember { mutableStateOf<String?>(null) }
                    
                    val coroutineScope = rememberCoroutineScope()
                    var isVerifying by remember { mutableStateOf(false) }
                    var verifyError by remember { mutableStateOf<String?>(null) }
                    var verifySuccess by remember { mutableStateOf<Boolean?>(null) }
                    
                    viewModel.initContext(androidx.compose.ui.platform.LocalContext.current)
                    
                    val briefState by viewModel.briefState.collectAsState()
                    val schemaState by viewModel.schemaState.collectAsState()
                    val latexState by viewModel.latexState.collectAsState()
                    val compileState by viewModel.compileState.collectAsState()
                    var showExitDialog by remember { mutableStateOf(false) }
                    var showProgressCard by remember { mutableStateOf(false) }
                    var editorInitialTab by remember { mutableIntStateOf(0) }
                    
                    LaunchedEffect(schemaState) {
                        if (schemaState is SchemaState.Success) {
                            currentScreen = "co_author"
                        } else if (schemaState is SchemaState.Loading) {
                            showProgressCard = true
                        }
                    }

                    LaunchedEffect(latexState) {
                        if (latexState is LatexState.Success && !viewModel.isFullAiMode) {
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
                            val bottomTabRoutes = listOf("home", "library", "latex_notebook", "templates", "settings")
                            if (currentScreen in bottomTabRoutes && selectedPdfForViewer == null && compileState !is CompileState.Success && compileState !is CompileState.Error) {
                                NavigationBar(
                                    containerColor = LocalThemeTokens.current.surface,
                                    contentColor = LocalThemeTokens.current.textSecondary
                                ) {
                                    val items = listOf(
                                        Triple("home", "Home", Icons.Default.Home),
                                        Triple("library", "Library", Icons.Default.Book),
                                        Triple("latex_notebook", "Editor", Icons.Default.Edit),
                                        Triple("templates", "Templates", Icons.Default.Style),
                                        Triple("settings", "Settings", Icons.Default.Settings)
                                    )
                                    items.forEach { (route, label, icon) ->
                                        val haptic = LocalHapticFeedback.current
                                        NavigationBarItem(
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, style = LocalThemeTokens.current.wordmarkFontFamily.let { LuxeTypography.labelSmall.copy(fontFamily = it) }) },
                                            selected = currentScreen == route,
                                            onClick = { 
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                currentScreen = route 
                                            },
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

                                val bottomTabRoutes = listOf("home", "library", "latex_notebook", "templates", "settings")
                                val isBottomTab = currentScreen in bottomTabRoutes
                                
                                AnimatedContent(
                                    targetState = isBottomTab,
                                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }
                                ) { isMain ->
                                    if (isMain) {
                                        val pagerState = rememberPagerState(
                                            initialPage = bottomTabRoutes.indexOf(currentScreen).coerceAtLeast(0),
                                            pageCount = { 5 }
                                        )
                                        
                                        LaunchedEffect(pagerState.currentPage) {
                                            if (bottomTabRoutes[pagerState.currentPage] != currentScreen) {
                                                currentScreen = bottomTabRoutes[pagerState.currentPage]
                                            }
                                        }
                                        
                                        LaunchedEffect(currentScreen) {
                                            val idx = bottomTabRoutes.indexOf(currentScreen)
                                            if (idx != -1 && pagerState.currentPage != idx && !pagerState.isScrollInProgress) {
                                                pagerState.animateScrollToPage(idx)
                                            }
                                        }
                                        
                                        BackHandler(enabled = pagerState.currentPage != 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(0)
                                            }
                                        }
                                        
                                        HorizontalPager(
                                            state = pagerState, 
                                            modifier = Modifier.fillMaxSize(),
                                            beyondBoundsPageCount = 4 // Keeps all tabs in memory to completely eliminate lag!
                                        ) { page ->
                                            when (page) {
                                                0 -> HomeScreen(
                                                    viewModel = viewModel,
                                                    onMagazineSelected = { url -> selectedPdfForViewer = url },
                                                    onContinueEditing = { currentScreen = "latex_notebook" },
                                                    onFullAiModeClicked = { editorInitialTab = 0; viewModel.resetState(); currentScreen = "templates" },
                                                    onAssistedModeClicked = { editorInitialTab = 1; viewModel.resetState(); currentScreen = "templates" },
                                                    onViewLibrary = { currentScreen = "library" }
                                                )
                                                1 -> MyMagazinesScreen(
                                                    onBack = { currentScreen = "home" },
                                                    onMagazineSelected = { pdfFile -> selectedPdfForViewer = pdfFile.absolutePath }
                                                )
                                                2 -> LatexNotebookScreen(
                                                    initialLatex = viewModel.getLatexCode() ?: (latexState as? LatexState.Success)?.latexCode ?: "",
                                                    isCompiling = isCompileLoading,
                                                    compileState = compileState,
                                                    schemaState = schemaState,
                                                    onCompile = { code -> viewModel.compileRaw(applicationContext, code) },
                                                    onBack = { currentScreen = "home" },
                                                    onCodeChange = { code -> viewModel.updateLatexCode(code) },
                                                    onRewrite = { text, instruction, onResult ->
                                                        coroutineScope.launch {
                                                            viewModel.rewriteSelection(litellmUrl, litellmKey, text, instruction, onResult)
                                                        }
                                                    }
                                                )
                                                3 -> TemplateGalleryScreen(
                                                    onTemplateSelected = { template, description, name ->
                                                        selectedTemplate = template
                                                        initialEditorPrompt = description
                                                        selectedTemplateName = name
                                                        currentScreen = "editor"
                                                    },
                                                    onPreviewSelected = { templateVariant ->
                                                        viewModel.generateSchema(litellmUrl, litellmKey, "Magazine Preview", templateVariant)
                                                    },
                                                    onLibraryClicked = { currentScreen = "library" },
                                                    onPublishClicked = { currentScreen = "showcase" },
                                                    onEditorClicked = { currentScreen = "latex_notebook" }
                                                )
                                                4 -> SettingsScreen(
                                                    currentLiteLLMUrl = litellmUrl,
                                                    currentLiteLLMKey = litellmKey,
                                                    isVerifying = isVerifying,
                                                    verifyError = verifyError,
                                                    verifySuccess = verifySuccess,
                                                    onSaveCredentials = { newUrl, newKey ->
                                                        isVerifying = true
                                                        verifyError = null
                                                        verifySuccess = null
                                                        coroutineScope.launch {
                                                            try {
                                                                val res = com.magazineforge.app.network.ApiClient.retrofitService.verifyKey(
                                                                    com.magazineforge.app.models.VerifyKeyRequest(newUrl, newKey)
                                                                )
                                                                if (res.isSuccessful && res.body()?.valid == true) {
                                                                    secureStorage.saveLiteLLMUrl(newUrl)
                                                                    secureStorage.saveLiteLLMKey(newKey)
                                                                    litellmUrl = newUrl
                                                                    litellmKey = newKey
                                                                    verifySuccess = true
                                                                    verifyError = "Status: ${res.body()?.status ?: "Active"}"
                                                                } else {
                                                                    verifySuccess = false
                                                                    verifyError = "Verification Failed: ${res.body()?.status ?: "Unknown error"}"
                                                                }
                                                            } catch (e: Exception) {
                                                                verifyError = "Backend Connection Error"
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
                                    } else {
                                        AnimatedContent(
                                            targetState = currentScreen,
                                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }
                                        ) { screen ->
                                            when (screen) {
                                                "splash" -> SplashScreen(
                                                    onAnimationFinished = {
                                                        currentScreen = if (savedUrl != null) "home" else "onboarding"
                                                    }
                                                )
                                                "onboarding" -> OnboardingScreen(
                                                    onCreateClicked = { currentScreen = "home" },
                                                    onExploreClicked = { currentScreen = "templates" }
                                                )
                                                "showcase" -> ShowcaseScreen(
                                                    viewModel = viewModel,
                                                    onMagazineSelected = { url -> selectedPdfForViewer = url }
                                                )
                                                "editor" -> EditorScreen(
                                                    templateVariant = selectedTemplate,
                                                    templateName = selectedTemplateName,
                                                    initialPrompt = initialEditorPrompt,
                                                    isCompileLoading = schemaState is SchemaState.Loading || latexState is LatexState.Loading || compileState is CompileState.Loading,
                                                    briefState = briefState,
                                                    schemaState = schemaState,
                                                    latexState = latexState,
                                                    compileState = compileState,
                                                    initialTabIndex = editorInitialTab,
                                                    onGenerateBrief = { prompt, referenceImages, articleCount ->
                                                        viewModel.generateBrief(litellmUrl, litellmKey, prompt, referenceImages, articleCount)
                                                    },
                                                    onCompileFromBrief = { prompt, config, brief ->
                                                        viewModel.isFullAiMode = true
                                                        val firstHalf = if (brief.articles.size > 7) brief.articles.take(7) else brief.articles
                                                        val secondHalf = if (brief.articles.size > 7) brief.articles.drop(7) else emptyList()
                                                        
                                                        viewModel.pendingArticles = secondHalf
                                                        viewModel.pendingTopic = prompt
                                                        viewModel.pendingTone = brief.tone
                                                        viewModel.pendingStyleDna = brief.styleDna
                                                        viewModel.pendingConfig = config
                                                        
                                                        val pagesStr = firstHalf.map { 
                                                            "Page Type: ARTICLE\nTopic: ${it.topic}\n[CUSTOMIZATION CONFIGURATION]:\n- Writing Tone: ${brief.tone}\n- Layout Density: ${brief.styleDna}"
                                                        }.joinToString("\n\n")

                                                        val finalTopic = "Theme: $prompt\n\nRequired Structure:\n$pagesStr\n\n(CRITICAL INSTRUCTION: Generate EXACTLY ${firstHalf.size} articles corresponding to the topics above.)\n(GLOBAL STYLE RULE: Dynamically adapt your writing tone, voice, and pacing to perfectly match the specific magazine topic and target audience. Do not rely on a single default persona; if the topic is serious, be authoritative and measured; if it's pop-culture, be punchy and witty. Regardless of the dynamically chosen tone, you MUST strictly avoid generic AI transitions like 'In conclusion', 'Let's dive into', or 'A testament to'. Emphasize 'show, don't tell'. Focus heavily on rich formatting, using bullet points, bold text, blockquotes, and visual breaks frequently to make the reading experience dynamic.)"
                                                        
                                                        viewModel.generateSchema(
                                                            litellmUrl = litellmUrl, 
                                                            litellmKey = litellmKey,
                                                            magazineTopic = finalTopic, 
                                                            templateName = selectedTemplate,
                                                            config = config,
                                                            tone = brief.tone,
                                                            layoutDensity = brief.styleDna
                                                        )
                                                    },
                                                    onCompileClicked = { magazineTopic, pages, config, coverImgUrl ->
                                                        viewModel.isFullAiMode = false
                                                        val finalTopic = if (pages.isEmpty()) {
                                                            magazineTopic
                                                        } else {
                                                            val pagesStr = pages.joinToString("\n\n") { 
                                                                "Page Type: ${it.type.uppercase()}\nTopic: ${it.topic}\nTarget Image URL: ${it.imageUrl}\n[CUSTOMIZATION CONFIGURATION]:\n- Writing Tone: ${it.tone}\n- Layout Density: ${it.layoutDensity}"
                                                            }
                                                            "Theme: $magazineTopic\n\nRequired Structure:\n$pagesStr\n\n(CRITICAL INSTRUCTION: You MUST strictly adhere to the [CUSTOMIZATION CONFIGURATION] for each page. Adapt your language, formatting, and generation to perfectly match the requested Tone and Layout Density. You MUST also use the exact Target Image URLs provided above for each corresponding page. Do NOT override them with Pollinations URLs unless no Target Image URL was provided.)\n(GLOBAL STYLE RULE: Dynamically adapt your writing tone, voice, and pacing to perfectamente match the specific magazine topic and target audience. Do not rely on a single default persona; if the topic is serious, be authoritative and measured; if it's pop-culture, be punchy and witty. Regardless of the dynamically chosen tone, you MUST strictly avoid generic AI transitions like 'In conclusion', 'Let's dive into', or 'A testament to'. Emphasize 'show, don't tell'. Focus heavily on rich formatting, using bullet points, bold text, blockquotes, and visual breaks frequently to make the reading experience dynamic.)"
                                                        }
                                                        viewModel.generateSchema(
                                                            litellmUrl = litellmUrl, 
                                                            litellmKey = litellmKey, 
                                                            magazineTopic = finalTopic, 
                                                            templateName = selectedTemplate,
                                                            config = config,
                                                            coverImageUrl = coverImgUrl
                                                        )
                                                    },
                                                    onCancel = { viewModel.resetState() },
                                                    onBack = { currentScreen = "home" }
                                                )
                                                "co_author" -> {
                                                    if (schemaState is SchemaState.Success) {
                                                        CoAuthorScreen(
                                                            initialSchema = (schemaState as SchemaState.Success).schema,
                                                            isGeneratingLatex = latexState is LatexState.Loading,
                                                            isFullAiMode = viewModel.isFullAiMode,
                                                            pendingArticlesCount = viewModel.pendingArticles.size,
                                                            onGenerateRemaining = { viewModel.generateRemainingSchema() },
                                                            onGenerateLatex = { schema -> viewModel.generateLatex(schema) },
                                                            onNext = { currentScreen = "latex_notebook" },
                                                            onBack = { viewModel.resetState(); currentScreen = "editor" }
                                                        )
                                                    }
                                                }
                                            }
                                        }
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
                                        "editor" -> {
                                            currentScreen = "templates"
                                        }
                                        "templates", "library", "gallery", "showcase" -> {
                                            currentScreen = "home"
                                        }
                                        "co_author" -> {
                                            viewModel.resetState()
                                            currentScreen = "editor"
                                        }
                                        "latex_notebook" -> {
                                            if (schemaState is SchemaState.Success) {
                                                currentScreen = "co_author"
                                            } else if (schemaState is SchemaState.Idle) {
                                                currentScreen = "home"
                                            } else {
                                                currentScreen = "editor"
                                            }
                                        }
                                        "home", "onboarding" -> {
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

}
