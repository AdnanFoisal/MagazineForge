package com.magazineforge.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.magazineforge.app.models.ArticleSchema
import com.magazineforge.app.models.MagazineSchema
import com.magazineforge.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import com.magazineforge.app.models.GenerateSchemaRequest
import com.magazineforge.app.models.GenerateLatexRequest
import com.magazineforge.app.models.CompileRawRequest
import com.magazineforge.app.models.GenerateBriefRequest
import com.magazineforge.app.models.GenerateBriefResponse
import com.magazineforge.app.models.GenerationRunRequest
import com.magazineforge.app.models.GenerationRunStatus
import com.google.gson.Gson


sealed class GenerationRunState {
    object Idle : GenerationRunState()
    data class Loading(val message: String = "Preparing run...") : GenerationRunState()
    data class Active(val status: GenerationRunStatus) : GenerationRunState()
    data class Error(val message: String) : GenerationRunState()
}

sealed class BriefState {
    object Idle : BriefState()
    object Loading : BriefState()
    data class Success(val brief: GenerateBriefResponse) : BriefState()
    data class Error(val message: String) : BriefState()
}

sealed class SchemaState {
    object Idle : SchemaState()
    object Loading : SchemaState()
    data class Success(val schema: MagazineSchema) : SchemaState()
    data class Error(val message: String) : SchemaState()
}

sealed class LatexState {
    object Idle : LatexState()
    object Loading : LatexState()
    data class Success(val latexCode: String) : LatexState()
    data class Error(val message: String) : LatexState()
}

sealed class CompileState {
    object Idle : CompileState()
    data class Loading(val progress: Int, val message: String = "Generating...") : CompileState()
    data class Success(val pdfFile: File) : CompileState()
    data class Error(val message: String) : CompileState()
}

class EditorViewModel : ViewModel() {
    
    private val _generationRunState = MutableStateFlow<GenerationRunState>(GenerationRunState.Idle)
    val generationRunState = _generationRunState.asStateFlow()
    
    private var currentRunId: String? = null
    private var pollingJob: kotlinx.coroutines.Job? = null

    private val _briefState = MutableStateFlow<BriefState>(BriefState.Idle)
    val briefState = _briefState.asStateFlow()

    private val _schemaState = MutableStateFlow<SchemaState>(SchemaState.Idle)
    val schemaState = _schemaState.asStateFlow()

    private val _latexState = MutableStateFlow<LatexState>(LatexState.Idle)
    val latexState = _latexState.asStateFlow()

    private val _compileState = MutableStateFlow<CompileState>(CompileState.Idle)
    val compileState: StateFlow<CompileState> = _compileState

    private val _aiRawLatexState = MutableStateFlow<LatexState>(LatexState.Idle)
    val aiRawLatexState: StateFlow<LatexState> = _aiRawLatexState.asStateFlow()
    
    private var currentTopic: String = ""
    private var currentVariant: String = ""
    private var isFromShowcase: Boolean = false
    private var currentLiteLLMUrl: String = ""
    private var currentLiteLLMKey: String = ""
    private var currentRawLatex: String = ""
    private var currentCoverUrl: String = ""
    private var currentEditedLatex: String? = null

    var isFullAiMode: Boolean = false
    private var appContext: Context? = null
    
    var pendingArticles: List<com.magazineforge.app.models.BriefArticle> = emptyList()
    var pendingTopic: String = ""
    var pendingTone: String = ""
    var pendingStyleDna: String = ""
    var pendingConfig: SectionComposerConfig? = null

    fun initContext(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun getLatexCode(): String? {
        val memoryCode = currentEditedLatex ?: currentRawLatex.takeIf { it.isNotEmpty() }
        if (memoryCode != null) return memoryCode
        
        // Try loading from preferences if memory is empty
        val prefs = appContext?.getSharedPreferences("MagBoyPrefs", Context.MODE_PRIVATE)
        val saved = prefs?.getString("saved_latex", null)
        if (saved != null) {
            currentEditedLatex = saved
        }
        return saved
    }

    fun updateLatexCode(code: String) {
        currentEditedLatex = code
        appContext?.getSharedPreferences("MagBoyPrefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("saved_latex", code)
            ?.apply()
    }

    private fun normalizeTemplateVariant(templateName: String): String {
        val candidate = when {
            templateName.startsWith("cover_template_") -> templateName.removePrefix("cover_template_")
            templateName in setOf("a", "b", "c") -> templateName
            else -> templateName.split("_").lastOrNull().orEmpty()
        }.lowercase()

        return candidate.takeIf { it in setOf("a", "b", "c") } ?: error("Unrecognized template variant: $templateName")
    }
    
    fun resetState() {
        _generationRunState.value = GenerationRunState.Idle
        _briefState.value = BriefState.Idle
        _schemaState.value = SchemaState.Idle
        pollingJob?.cancel()
        _latexState.value = LatexState.Idle
        _compileState.value = CompileState.Idle
    }

    fun resetLatexState() {
        if (_latexState.value is LatexState.Error) {
            _latexState.value = LatexState.Idle
        }
    }

    fun resetCompileState() {
        if (_compileState.value is CompileState.Error) {
            _compileState.value = CompileState.Idle
        }
    }

    fun resetSchemaState() {
        if (_schemaState.value is SchemaState.Error) {
            _schemaState.value = SchemaState.Idle
        }
    }

    fun generateBrief(litellmUrl: String, litellmKey: String, prompt: String, referenceImages: List<String> = emptyList(), articleCount: Int? = null) {
        if (prompt.isBlank()) {
            _briefState.value = BriefState.Error("Prompt can't be empty")
            return
        }
        currentLiteLLMUrl = litellmUrl
        currentLiteLLMKey = litellmKey
        _briefState.value = BriefState.Loading
        
        viewModelScope.launch {
            try {
                ApiClient.ensureSpaceAwake()
                val request = GenerateBriefRequest(prompt = prompt, referenceImages = referenceImages, articleCount = articleCount)
                val response = ApiClient.retrofitService.generateBrief(litellmUrl, litellmKey, request)
                
                if (response.isSuccessful) {
                    val brief = response.body()
                    if (brief != null) {
                        _briefState.value = BriefState.Success(brief)
                    } else {
                        _briefState.value = BriefState.Error("Empty response body")
                    }
                } else {
                    val code = response.code()
                    if (code == 401 || code == 429) {
                        _briefState.value = BriefState.Error("Your API keys may be invalid or rate-limited — check them in Settings")
                    } else {
                        val errorStr = response.errorBody()?.string() ?: "Unknown error"
                        _briefState.value = BriefState.Error("Error $code: $errorStr")
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                _briefState.value = BriefState.Error("Request timed out. The server might be warming up or busy.")
            } catch (e: Exception) {
                _briefState.value = BriefState.Error(e.message ?: "Brief generation failed unexpectedly")
            }
        }
    }

        fun generateChunkedPreview(litellmUrl: String, litellmKey: String, templateVariant: String) {
        currentLiteLLMUrl = litellmUrl
        currentLiteLLMKey = litellmKey
        currentVariant = normalizeTemplateVariant(templateVariant)
        
        _generationRunState.value = GenerationRunState.Loading("Generating brief...")
        
        viewModelScope.launch {
            try {
                ApiClient.ensureSpaceAwake()
                val prompt = "A beautiful lifestyle and design magazine showcasing the template's features"
                val request = GenerateBriefRequest(prompt = prompt, referenceImages = emptyList(), articleCount = 9)
                val response = ApiClient.retrofitService.generateBrief(litellmUrl, litellmKey, request)
                
                if (response.isSuccessful && response.body() != null) {
                    val brief = response.body()!!
                    pendingConfig = SectionComposerConfig(enableMasthead = true, enableTocTeasers = true, enableByline = true, enablePullQuote = true)
                    
                    val safeConfig = pendingConfig!!
                    val runRequest = GenerationRunRequest(
                        prompt = prompt,
                        templateVariant = currentVariant,
                        articles = brief.articles,
                        tone = brief.tone,
                        layoutDensity = brief.styleDna,
                        enableMasthead = safeConfig.enableMasthead,
                        mastheadAngle = safeConfig.mastheadAngle,
                        enableSidebar = safeConfig.enableSidebar,
                        sidebarTopic = safeConfig.sidebarTopic,
                        enablePullQuote = safeConfig.enablePullQuote,
                        enableBackCover = safeConfig.enableBackCover,
                        enableTocTeasers = safeConfig.enableTocTeasers,
                        enableByline = safeConfig.enableByline,
                        coverImageUrl = ""
                    )
                    
                    _generationRunState.value = GenerationRunState.Loading("Starting run...")
                    val runResponse = ApiClient.retrofitService.createGenerationRun(litellmUrl, litellmKey, runRequest)
                    
                    if (runResponse.isSuccessful && runResponse.body() != null) {
                        val runId = runResponse.body()!!.runId
                        currentRunId = runId
                        startPollingRun(runId)
                    } else {
                        _generationRunState.value = GenerationRunState.Error("Failed to create generation run: ${runResponse.code()}")
                    }
                } else {
                    _generationRunState.value = GenerationRunState.Error("Failed to generate brief: ${response.code()}")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _generationRunState.value = GenerationRunState.Error("Request timed out. The server might be warming up.")
            } catch (e: Exception) {
                _generationRunState.value = GenerationRunState.Error(e.message ?: "Run creation failed")
            }
        }
    }

    /**
     * Full AI Mode entry point: brief → generation run → poll → latex → compile.
     * Uses the Generation Runs pipeline so no single HTTP request can timeout.
     * User-uploaded images are passed via articleImageUrls for positional injection.
     */
    fun startFullAiGeneration(
        litellmUrl: String,
        litellmKey: String,
        prompt: String,
        templateVariant: String,
        config: SectionComposerConfig,
        brief: com.magazineforge.app.models.GenerateBriefResponse,
        coverImageUrl: String = "",
        backCoverImageUrl: String = "",
        referenceImageUrls: List<String> = emptyList()
    ) {
        isFullAiMode = true
        currentLiteLLMUrl = litellmUrl
        currentLiteLLMKey = litellmKey
        currentVariant = normalizeTemplateVariant(templateVariant)
        currentTopic = prompt
        pendingConfig = config
        currentRawLatex = ""
        currentEditedLatex = null

        _generationRunState.value = GenerationRunState.Loading("Starting generation run...")
        _schemaState.value = SchemaState.Loading

        viewModelScope.launch {
            try {
                ApiClient.ensureSpaceAwake()

                // In Full AI mode, the user uploads at most two images:
                // a cover image and a back cover image. Any reference images
                // uploaded in Phase 1 are assigned positionally to articles.
                val articleImgUrls = referenceImageUrls

                val runRequest = GenerationRunRequest(
                    prompt = prompt,
                    templateVariant = currentVariant,
                    articles = brief.articles,
                    coverTitle = brief.titles.firstOrNull() ?: "",
                    category = brief.category,
                    tone = brief.tone,
                    layoutDensity = brief.styleDna,
                    enableMasthead = config.enableMasthead,
                    mastheadAngle = config.mastheadAngle,
                    enableSidebar = config.enableSidebar,
                    sidebarTopic = config.sidebarTopic,
                    enablePullQuote = config.enablePullQuote,
                    enableBackCover = config.enableBackCover,
                    enableTocTeasers = config.enableTocTeasers,
                    enableByline = config.enableByline,
                    coverImageUrl = coverImageUrl,
                    backCoverImageUrl = backCoverImageUrl,
                    articleImageUrls = articleImgUrls
                )

                val runResponse = ApiClient.retrofitService.createGenerationRun(
                    litellmUrl, litellmKey, runRequest, generateAll = true
                )

                if (runResponse.isSuccessful && runResponse.body() != null) {
                    val runId = runResponse.body()!!.runId
                    currentRunId = runId
                    // The first call (createGenerationRun with generateAll=true)
                    // already starts the backend worker with generate_all=True.
                    // Calling /continue here is redundant — it either no-ops
                    // (worker still running) or races with a fast-completing
                    // worker and returns 409. Just start polling.
                    startPollingRun(runId)
                } else {
                    _generationRunState.value = GenerationRunState.Error(
                        "Failed to create run: ${runResponse.code()}"
                    )
                    _schemaState.value = SchemaState.Error("Failed to create run: ${runResponse.code()}")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _generationRunState.value = GenerationRunState.Error(
                    "Request timed out. The server might be warming up."
                )
                _schemaState.value = SchemaState.Error("Request timed out. The server might be warming up.")
            } catch (e: Exception) {
                _generationRunState.value = GenerationRunState.Error(
                    e.message ?: "Generation run creation failed"
                )
                _schemaState.value = SchemaState.Error(e.message ?: "Generation run creation failed")
            }
        }
    }
    
    private fun startPollingRun(runId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            // Cap polling at 12 minutes. If the worker hasn't finished by then
            // (e.g. backend silently died, HF Space restarted and killed the
            // asyncio task), surface an error instead of polling 0/N forever.
            val startedAt = System.currentTimeMillis()
            val pollingTimeoutMs = 12L * 60 * 1000
            while (true) {
                if (System.currentTimeMillis() - startedAt > pollingTimeoutMs) {
                    val err = "Generation timed out — the backend did not finish the run within 12 minutes. The Hugging Face Space may have restarted; please try again."
                    _generationRunState.value = GenerationRunState.Error(err)
                    if (_schemaState.value is SchemaState.Loading) {
                        _schemaState.value = SchemaState.Error(err)
                    }
                    break
                }
                try {
                    val response = ApiClient.retrofitService.getGenerationRun(runId)
                    if (response.isSuccessful && response.body() != null) {
                        val status = response.body()!!
                        
                        try {
                            val file = File(appContext?.filesDir, "run_snapshot.json")
                            val json = Gson().toJson(status)
                            FileOutputStream(file).use { it.write(json.toByteArray()) }
                        } catch (e: Exception) { }
                        
                        _generationRunState.value = GenerationRunState.Active(status)
                        
                        // Only COMPLETED advances to SchemaState.Success and auto-chains.
                        // PAUSED means a section permanently failed — surface as Error
                        // so the user sees what went wrong instead of the dialog silently
                        // disappearing (which previously left the user with no PDF and
                        // no clue why).
                        if (status.status == "READY_FOR_CONTENT" || status.status == "COMPLETED") {
                            _schemaState.value = SchemaState.Success(status.schema)
                        }
                        
                        if (status.status == "COMPLETED") {
                            // SchemaState.Success was set above, which triggers
                            // the LaunchedEffect in MainActivity to navigate to
                            // the CoAuthor screen. The user reviews/edits the
                            // schema there, then taps "Generate LaTeX".
                            //
                            // We do NOT auto-chain to generateLatex here — that
                            // would skip the CoAuthor screen entirely. The
                            // auto-compile from LaTeX → PDF still happens inside
                            // generateLatex() when isFullAiMode is true, so the
                            // full pipeline is: schema (CoAuthor) → LaTeX → PDF,
                            // with the user landing on the CoAuthor screen first.
                            break
                        }
                        if (status.status == "PAUSED" || status.status == "INTERRUPTED") {
                            val reason = status.error?.takeIf { it.isNotBlank() }
                                ?: "One or more articles failed to generate (run ${status.status.lowercase()})."
                            _generationRunState.value = GenerationRunState.Error(reason)
                            if (_schemaState.value is SchemaState.Loading) {
                                _schemaState.value = SchemaState.Error(reason)
                            }
                            break
                        }
                        if (status.status == "CANCELLED") {
                            val reason = "Generation was cancelled."
                            _generationRunState.value = GenerationRunState.Error(reason)
                            if (_schemaState.value is SchemaState.Loading) {
                                _schemaState.value = SchemaState.Error(reason)
                            }
                            break
                        }
                    } else {
                        val err = "Failed to poll run: ${response.code()}"
                        _generationRunState.value = GenerationRunState.Error(err)
                        if (_schemaState.value is SchemaState.Loading) {
                            _schemaState.value = SchemaState.Error(err)
                        }
                        break
                    }
                } catch (e: Exception) {
                    val err = "Polling error: ${e.message}"
                    _generationRunState.value = GenerationRunState.Error(err)
                    if (_schemaState.value is SchemaState.Loading) {
                        _schemaState.value = SchemaState.Error(err)
                    }
                    break
                }
                delay(2000)
            }
        }
    }

    fun generateSchema(
        litellmUrl: String, 
        litellmKey: String, 
        magazineTopic: String, 
        templateName: String,
        config: SectionComposerConfig? = null,
        tone: String = "Professional",
        layoutDensity: String = "Balanced",
        coverImageUrl: String = ""
    ) {
        if (magazineTopic.isBlank()) {
            _schemaState.value = SchemaState.Error("Topic can't be empty")
            return
        }
        currentTopic = magazineTopic
        currentLiteLLMUrl = litellmUrl
        currentLiteLLMKey = litellmKey
        currentVariant = normalizeTemplateVariant(templateName)
        currentRawLatex = ""
        currentEditedLatex = null
        
        _schemaState.value = SchemaState.Loading
        
        viewModelScope.launch {
            try {
                ApiClient.ensureSpaceAwake()
                val safeConfig = config ?: SectionComposerConfig()
                val request = GenerateSchemaRequest(
                    topic = magazineTopic, 
                    templateVariant = currentVariant,
                    tone = tone,
                    layoutDensity = layoutDensity,
                    enableMasthead = safeConfig.enableMasthead,
                    mastheadAngle = safeConfig.mastheadAngle,
                    enableSidebar = safeConfig.enableSidebar,
                    sidebarTopic = safeConfig.sidebarTopic,
                    enablePullQuote = safeConfig.enablePullQuote,
                    enableBackCover = safeConfig.enableBackCover,
                    enableTocTeasers = safeConfig.enableTocTeasers,
                    enableByline = safeConfig.enableByline,
                    coverImageUrl = coverImageUrl
                )
                val response = ApiClient.retrofitService.generateSchema(
                    litellmUrl = litellmUrl,
                    litellmKey = litellmKey,
                    request = request
                )
                
                if (response.isSuccessful) {
                    val schema = response.body()
                    if (schema != null) {
                        _schemaState.value = SchemaState.Success(schema)
                        if (isFullAiMode) {
                            generateLatex(schema)
                        }
                    } else {
                        _schemaState.value = SchemaState.Error("Empty response body")
                    }
                } else {
                    val code = response.code()
                    if (code == 401 || code == 429) {
                        _schemaState.value = SchemaState.Error("Your API keys may be invalid or rate-limited — check them in Settings")
                    } else {
                        val errorStr = response.errorBody()?.string() ?: "Unknown error"
                        _schemaState.value = SchemaState.Error("Error $code: $errorStr")
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                _schemaState.value = SchemaState.Error("Request timed out. The backend is busy generating the schema.")
            } catch (e: Exception) {
                _schemaState.value = SchemaState.Error(e.message ?: "Generation failed unexpectedly")
            }
        }
    }

        fun continueGenerationRun() {
        val runId = currentRunId ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.retrofitService.continueGenerationRun(runId, currentLiteLLMUrl, currentLiteLLMKey)
                if (response.isSuccessful) {
                    startPollingRun(runId)
                } else {
                    _generationRunState.value = GenerationRunState.Error("Failed to continue run: ${response.code()}")
                }
            } catch(e: Exception) {
                _generationRunState.value = GenerationRunState.Error("Continue error: ${e.message}")
            }
        }
    }
    
    fun retryGenerationSection(sectionId: String) {
        val runId = currentRunId ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.retrofitService.retryGenerationSection(runId, sectionId, currentLiteLLMUrl, currentLiteLLMKey)
                if (response.isSuccessful) {
                    startPollingRun(runId)
                }
            } catch(e: Exception) {}
        }
    }

    fun generateLatex(schema: MagazineSchema) {
        currentRawLatex = ""
        currentEditedLatex = null
        _latexState.value = LatexState.Loading
        
        viewModelScope.launch {
            try {
                ApiClient.ensureSpaceAwake()
                val request = GenerateLatexRequest(schema = schema, templateVariant = currentVariant)
                val response = ApiClient.retrofitService.generateLatex(currentLiteLLMUrl, currentLiteLLMKey, request)
                
                if (response.isSuccessful) {
                    val latex = response.body()?.latexCode
                    if (latex != null) {
                        currentRawLatex = latex
                        appContext?.getSharedPreferences("MagBoyPrefs", Context.MODE_PRIVATE)
                            ?.edit()
                            ?.putString("saved_latex", latex)
                            ?.apply()
                        _latexState.value = LatexState.Success(latex)
                        // In Full AI mode, the user tapped "Generate Full Issue"
                        // expecting a finished PDF — not just LaTeX code. Auto-chain
                        // into compileRaw so the pipeline actually produces a PDF
                        // without requiring a manual tap on the Compile button.
                        if (isFullAiMode) {
                            val ctx = appContext
                            if (ctx != null) {
                                compileRaw(ctx, latex, topic = currentTopic, variant = currentVariant)
                            }
                        }
                    } else {
                        _latexState.value = LatexState.Error("Received empty latex")
                    }
                } else {
                    val code = response.code()
                    val errorBody = response.errorBody()?.string() ?: ""
                    if (code == 401 || code == 429) {
                        _latexState.value = LatexState.Error("Your API keys may be invalid or rate-limited — check them in Settings")
                    } else {
                        _latexState.value = LatexState.Error("Error: $code - $errorBody")
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                _latexState.value = LatexState.Error("Request timed out during LaTeX generation.")
            } catch (e: Exception) {
                _latexState.value = LatexState.Error(e.message ?: "Generation failed unexpectedly")
            }
        }
    }



    fun rewriteSelection(litellmUrl: String, litellmKey: String, text: String, instruction: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val request = com.magazineforge.app.models.RewriteSelectionRequest(text = text, instruction = instruction)
                val response = ApiClient.retrofitService.rewriteSelection(litellmUrl, litellmKey, request)
                
                if (response.isSuccessful) {
                    onResult(response.body()?.rewrittenText)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun compileRaw(context: Context, latexCode: String, topic: String = "magazine", variant: String = "variant", fromShowcase: Boolean = false) {
        this.currentTopic = topic
        this.currentVariant = variant
        this.isFromShowcase = fromShowcase
        this.currentRawLatex = latexCode
        this.currentCoverUrl = ""
        _compileState.value = CompileState.Loading(0, "Compiling PDF on Cloud...")
        
        viewModelScope.launch {
            try {
                ApiClient.ensureSpaceAwake()
                val request = CompileRawRequest(latexCode = latexCode)
                val response = ApiClient.retrofitService.compileRaw(request)
                if (response.isSuccessful) {
                    val jobId = response.body()?.jobId
                    if (jobId != null) {
                        pollJobStatus(context, jobId)
                    } else {
                        _compileState.value = CompileState.Error("Invalid job ID received")
                    }
                } else {
                    // Extract the server's error detail when available so the
                    // user sees a helpful message (e.g. "LaTeX code is empty.
                    // Tap 'Generate LaTeX' on the Co-Author screen first.")
                    // instead of just "Compile Error: 422".
                    val errorBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    val userMessage = if (errorBody != null) {
                        // Try to parse {"detail": "..."} from FastAPI's error response
                        try {
                            val json = org.json.JSONObject(errorBody)
                            json.optString("detail", "Compile Error: ${response.code()}")
                        } catch (_: Exception) {
                            "Compile Error: ${response.code()}"
                        }
                    } else {
                        "Compile Error: ${response.code()}"
                    }
                    _compileState.value = CompileState.Error(userMessage)
                }
            } catch (e: java.net.SocketTimeoutException) {
                _compileState.value = CompileState.Error("Compilation request timed out.")
            } catch (e: Exception) {
                _compileState.value = CompileState.Error(e.message ?: "Unknown network error")
            }
        }
    }
    
    private suspend fun pollJobStatus(context: Context, jobId: String) {
        var isPolling = true
        val startTime = System.currentTimeMillis()
        while (isPolling) {
            if (System.currentTimeMillis() - startTime > 180_000) {
                _compileState.value = CompileState.Error("Generation timed out after 180 seconds")
                break
            }
            try {
                val response = ApiClient.retrofitService.getJobStatus(jobId)
                if (response.isSuccessful) {
                    val statusObj = response.body()
                    if (statusObj != null) {
                        if (statusObj.status == "COMPLETED") {
                            _compileState.value = CompileState.Loading(100, "Downloading PDF...")
                            currentCoverUrl = statusObj.cover_url ?: ""
                            downloadPdf(context, jobId)
                            isPolling = false
                        } else if (statusObj.status == "FAILED") {
                            _compileState.value = CompileState.Error(statusObj.error ?: "Job failed on server")
                            isPolling = false
                        } else {
                            _compileState.value = CompileState.Loading(statusObj.progress, "Generating... ${statusObj.progress}%")
                            delay(5000) // Poll every 5 seconds
                        }
                    } else {
                        delay(5000) // Poll every 5 seconds if status body is null
                    }
                } else {
                    _compileState.value = CompileState.Error("Error polling job: ${response.code()}")
                    isPolling = false
                }
            } catch (e: Exception) {
                _compileState.value = CompileState.Error("Polling error: ${e.message}")
                isPolling = false
            }
        }
    }
    
    private suspend fun downloadPdf(context: Context, jobId: String) {
        try {
            _compileState.value = CompileState.Loading(100, "Downloading Magazine...")
            val response = ApiClient.retrofitService.downloadJob(jobId)
            
            _compileState.value = CompileState.Loading(100, "Downloading Cover...")
            val coverResponse = ApiClient.retrofitService.downloadCover(jobId)

            if (response.isSuccessful) {
                val pdfBytes = response.body()?.bytes()
                val coverBytes = if (coverResponse.isSuccessful) coverResponse.body()?.bytes() else null
                
                if (pdfBytes != null) {
                    val file = saveFilesToDisk(context, pdfBytes, coverBytes)
                    
                    _compileState.value = CompileState.Loading(100, "Publishing to Showcase...")
                    
                    if (!isFromShowcase) {
                        try {
                            val showcaseItem = com.magazineforge.app.models.ShowcaseItem(
                                title = currentTopic,
                                templateVariant = currentVariant,
                                latexCode = currentRawLatex,
                                pdfUrl = "https://adnanfoisal-magazineforge.hf.space/job/$jobId/download",
                                coverImageUrl = if (coverBytes != null) currentCoverUrl else ""
                            )
                            com.magazineforge.app.network.ShowcaseRepository().publishMagazine(showcaseItem)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // If publishing fails, user can still view their local PDF
                        }
                    }
                    
                    _compileState.value = CompileState.Success(file)
                } else {
                    _compileState.value = CompileState.Error("Received empty bytes")
                }
            } else {
                _compileState.value = CompileState.Error("Download Error: PDF=${response.code()} Cover=${coverResponse.code()}")
            }
        } catch (e: Exception) {
            _compileState.value = CompileState.Error("Download error: ${e.message}")
        }
    }
    
    private suspend fun saveFilesToDisk(context: Context, pdfBytes: ByteArray, coverBytes: ByteArray?): File {
        return withContext(Dispatchers.IO) {
            val previewFile = File(context.cacheDir, "magazine_preview.pdf")
            FileOutputStream(previewFile).use { it.write(pdfBytes) }
            
            try {
                val dir = File(context.filesDir, "magazines")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val safeTopic = currentTopic.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val timestamp = System.currentTimeMillis()
                val filename = "magazine_${timestamp}_${safeTopic}.pdf"
                val persistentFile = File(dir, filename)
                FileOutputStream(persistentFile).use { it.write(pdfBytes) }
                
                if (coverBytes != null) {
                    val coverFilename = "magazine_${timestamp}_${safeTopic}_cover.jpg"
                    val coverFile = File(dir, coverFilename)
                    FileOutputStream(coverFile).use { it.write(coverBytes) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            previewFile
        }
    }
}
