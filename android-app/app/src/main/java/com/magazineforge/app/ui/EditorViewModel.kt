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
    private var currentApiKey: String = ""
    private var currentBackupKey: String? = null
    private var currentRawLatex: String = ""
    private var currentCoverUrl: String = ""
    private var currentEditedLatex: String? = null

    fun getLatexCode(): String? = currentEditedLatex

    fun updateLatexCode(code: String) {
        currentEditedLatex = code
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
        _briefState.value = BriefState.Idle
        _schemaState.value = SchemaState.Idle
        _latexState.value = LatexState.Idle
        _compileState.value = CompileState.Idle
    }

    fun generateBrief(geminiKey: String, backupKey: String?, prompt: String, referenceImages: List<String> = emptyList()) {
        if (prompt.isBlank()) {
            _briefState.value = BriefState.Error("Prompt can't be empty")
            return
        }
        currentApiKey = geminiKey
        currentBackupKey = backupKey
        _briefState.value = BriefState.Loading
        
        viewModelScope.launch {
            try {
                val request = GenerateBriefRequest(prompt = prompt, referenceImages = referenceImages)
                var response = ApiClient.retrofitService.generateBrief(geminiKey, request)
                
                if (!response.isSuccessful && (response.code() == 401 || response.code() == 429)) {
                    if (!backupKey.isNullOrBlank()) {
                        response = ApiClient.retrofitService.generateBrief(backupKey, request)
                    }
                }
                
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
                        _briefState.value = BriefState.Error("Your API key may be invalid or rate-limited — check it in Settings")
                    } else {
                        _briefState.value = BriefState.Error("Error: $code")
                    }
                }
            } catch (e: Exception) {
                _briefState.value = BriefState.Error(e.message ?: "Brief generation failed unexpectedly")
            }
        }
    }

    fun generateSchema(
        geminiKey: String, 
        backupKey: String?, 
        magazineTopic: String, 
        templateName: String,
        config: SectionComposerConfig? = null,
        tone: String = "Professional",
        layoutDensity: String = "Balanced"
    ) {
        if (magazineTopic.isBlank()) {
            _schemaState.value = SchemaState.Error("Topic can't be empty")
            return
        }
        currentTopic = magazineTopic
        currentApiKey = geminiKey
        currentBackupKey = backupKey
        currentVariant = normalizeTemplateVariant(templateName)
        currentRawLatex = ""
        currentEditedLatex = null
        
        _schemaState.value = SchemaState.Loading
        
        viewModelScope.launch {
            try {
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
                    enableByline = safeConfig.enableByline
                )
                var response = ApiClient.retrofitService.generateSchema(geminiKey, request)
                
                if (!response.isSuccessful && (response.code() == 401 || response.code() == 429)) {
                    if (!backupKey.isNullOrBlank()) {
                        response = ApiClient.retrofitService.generateSchema(backupKey, request)
                    }
                }
                
                if (response.isSuccessful) {
                    val schema = response.body()
                    if (schema != null) {
                        _schemaState.value = SchemaState.Success(schema)
                    } else {
                        _schemaState.value = SchemaState.Error("Empty response body")
                    }
                } else {
                    val code = response.code()
                    if (code == 401 || code == 429) {
                        _schemaState.value = SchemaState.Error("Your API key may be invalid or rate-limited — check it in Settings")
                    } else {
                        _schemaState.value = SchemaState.Error("Error: $code")
                    }
                }
            } catch (e: Exception) {
                _schemaState.value = SchemaState.Error(e.message ?: "Generation failed unexpectedly")
            }
        }
    }

    fun generateLatex(schema: MagazineSchema) {
        currentRawLatex = ""
        currentEditedLatex = null
        _latexState.value = LatexState.Loading
        
        viewModelScope.launch {
            try {
                val request = GenerateLatexRequest(schema = schema, templateVariant = currentVariant)
                var response = ApiClient.retrofitService.generateLatex(currentApiKey, request)
                
                if (!response.isSuccessful && (response.code() == 401 || response.code() == 429)) {
                    if (!currentBackupKey.isNullOrBlank()) {
                        response = ApiClient.retrofitService.generateLatex(currentBackupKey!!, request)
                    }
                }
                
                if (response.isSuccessful) {
                    val latex = response.body()?.latexCode
                    if (latex != null) {
                        _latexState.value = LatexState.Success(latex)
                    } else {
                        _latexState.value = LatexState.Error("Received empty latex")
                    }
                } else {
                    val code = response.code()
                    if (code == 401 || code == 429) {
                        _latexState.value = LatexState.Error("Your API key may be invalid or rate-limited — check it in Settings")
                    } else {
                        _latexState.value = LatexState.Error("Error: $code")
                    }
                }
            } catch (e: Exception) {
                _latexState.value = LatexState.Error(e.message ?: "Generation failed unexpectedly")
            }
        }
    }



    fun rewriteSelection(geminiKey: String, backupKey: String?, text: String, instruction: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val request = com.magazineforge.app.models.RewriteSelectionRequest(text = text, instruction = instruction)
                var response = ApiClient.retrofitService.rewriteSelection(geminiKey, request)
                
                if (!response.isSuccessful && (response.code() == 401 || response.code() == 429)) {
                    if (!backupKey.isNullOrBlank()) {
                        response = ApiClient.retrofitService.rewriteSelection(backupKey, request)
                    }
                }
                
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
                    _compileState.value = CompileState.Error("Compile Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _compileState.value = CompileState.Error(e.message ?: "Unknown network error")
            }
        }
    }
    
    private suspend fun pollJobStatus(context: Context, jobId: String) {
        var isPolling = true
        val startTime = System.currentTimeMillis()
        while (isPolling) {
            if (System.currentTimeMillis() - startTime > 100_000) {
                _compileState.value = CompileState.Error("Generation timed out after 100 seconds")
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
