import re
import sys

file_path = r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\java\com\magazineforge\app\MainActivity.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add imports
imports_to_add = """
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
"""
if "import androidx.compose.animation.AnimatedContent" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", imports_to_add.strip() + "\nimport androidx.compose.ui.Modifier")

# 2. Change initial currentScreen to "splash"
content = re.sub(
    r'var currentScreen by remember \{ mutableStateOf\(if \(savedUrl != null\) "home" else "onboarding"\) \}',
    r'var currentScreen by remember { mutableStateOf("splash") }',
    content
)

# 3. Add haptic feedback to bottom navigation items
nav_bar_item = """                                        NavigationBarItem(
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, style = LocalThemeTokens.current.wordmarkFontFamily.let { LuxeTypography.labelSmall.copy(fontFamily = it) }) },
                                            selected = currentScreen == route,
                                            onClick = { currentScreen = route },"""

nav_bar_item_new = """                                        val haptic = LocalHapticFeedback.current
                                        NavigationBarItem(
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, style = LocalThemeTokens.current.wordmarkFontFamily.let { LuxeTypography.labelSmall.copy(fontFamily = it) }) },
                                            selected = currentScreen == route,
                                            onClick = { 
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                currentScreen = route 
                                            },"""
content = content.replace(nav_bar_item, nav_bar_item_new)

# 4. Replace the `when(currentScreen)` block with the Pager and AnimatedContent.
# First, find the "val isCompileLoading = compileState is CompileState.Loading" line.
split_point = "                                val isCompileLoading = compileState is CompileState.Loading"
if split_point in content:
    parts = content.split(split_point)
    before = parts[0] + split_point
    
    # We need to extract the bodies of the `when` branches to re-insert them.
    # It's actually safer to just do a string replacement on the whole `when (currentScreen)` block.
    # The `when (currentScreen)` block ends at line 442 where `// Floating Progress Card Overlay` starts.
    after_when_idx = parts[1].find("                            // Floating Progress Card Overlay")
    
    if after_when_idx != -1:
        when_block = parts[1][:after_when_idx]
        after_block = parts[1][after_when_idx:]
        
        # Build the new routing structure
        new_routing = """

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
                                            if (idx != -1 && pagerState.currentPage != idx) {
                                                pagerState.animateScrollToPage(idx)
                                            }
                                        }
                                        
                                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
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
                                                            "Page Type: ARTICLE\\nTopic: ${it.topic}\\n[CUSTOMIZATION CONFIGURATION]:\\n- Writing Tone: ${brief.tone}\\n- Layout Density: ${brief.styleDna}"
                                                        }.joinToString("\\n\\n")

                                                        val finalTopic = "Theme: $prompt\\n\\nRequired Structure:\\n$pagesStr\\n\\n(CRITICAL INSTRUCTION: Generate EXACTLY ${firstHalf.size} articles corresponding to the topics above.)\\n(GLOBAL STYLE RULE: Dynamically adapt your writing tone, voice, and pacing to perfectly match the specific magazine topic and target audience. Do not rely on a single default persona; if the topic is serious, be authoritative and measured; if it's pop-culture, be punchy and witty. Regardless of the dynamically chosen tone, you MUST strictly avoid generic AI transitions like 'In conclusion', 'Let's dive into', or 'A testament to'. Emphasize 'show, don't tell'. Focus heavily on rich formatting, using bullet points, bold text, blockquotes, and visual breaks frequently to make the reading experience dynamic.)"
                                                        
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
                                                            val pagesStr = pages.joinToString("\\n\\n") { 
                                                                "Page Type: ${it.type.uppercase()}\\nTopic: ${it.topic}\\nTarget Image URL: ${it.imageUrl}\\n[CUSTOMIZATION CONFIGURATION]:\\n- Writing Tone: ${it.tone}\\n- Layout Density: ${it.layoutDensity}"
                                                            }
                                                            "Theme: $magazineTopic\\n\\nRequired Structure:\\n$pagesStr\\n\\n(CRITICAL INSTRUCTION: You MUST strictly adhere to the [CUSTOMIZATION CONFIGURATION] for each page. Adapt your language, formatting, and generation to perfectly match the requested Tone and Layout Density. You MUST also use the exact Target Image URLs provided above for each corresponding page. Do NOT override them with Pollinations URLs unless no Target Image URL was provided.)\\n(GLOBAL STYLE RULE: Dynamically adapt your writing tone, voice, and pacing to perfectamente match the specific magazine topic and target audience. Do not rely on a single default persona; if the topic is serious, be authoritative and measured; if it's pop-culture, be punchy and witty. Regardless of the dynamically chosen tone, you MUST strictly avoid generic AI transitions like 'In conclusion', 'Let's dive into', or 'A testament to'. Emphasize 'show, don't tell'. Focus heavily on rich formatting, using bullet points, bold text, blockquotes, and visual breaks frequently to make the reading experience dynamic.)"
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
"""

        content = before + new_routing + after_block

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
