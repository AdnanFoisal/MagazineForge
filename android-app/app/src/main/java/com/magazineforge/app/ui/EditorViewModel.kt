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
import kotlinx.coroutines.flow.asStateFlow

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
    private val _schemaState = MutableStateFlow<SchemaState>(SchemaState.Idle)
    val schemaState = _schemaState.asStateFlow()

    private val _latexState = MutableStateFlow<LatexState>(LatexState.Idle)
    val latexState = _latexState.asStateFlow()

    private val _compileState = MutableStateFlow<CompileState>(CompileState.Idle)
    val compileState: StateFlow<CompileState> = _compileState.asStateFlow()
    
    private var currentTopic: String = ""
    private var currentVariant: String = ""
    private var isFromShowcase: Boolean = false
    private var currentApiKey: String = ""

    private fun normalizeTemplateVariant(templateName: String): String {
        val candidate = when {
            templateName.startsWith("cover_template_") -> templateName.removePrefix("cover_template_")
            templateName in setOf("a", "b", "c") -> templateName
            else -> templateName.split("_").lastOrNull().orEmpty()
        }.lowercase()

        return candidate.takeIf { it in setOf("a", "b", "c") } ?: "a"
    }
    
    fun resetState() {
        _schemaState.value = SchemaState.Idle
        _latexState.value = LatexState.Idle
        _compileState.value = CompileState.Idle
    }

    fun generateSchema(geminiKey: String, magazineTopic: String, templateName: String) {
        currentTopic = magazineTopic
        currentApiKey = geminiKey
        currentVariant = normalizeTemplateVariant(templateName)
        
        _schemaState.value = SchemaState.Loading
        
        viewModelScope.launch {
            try {
                val request = GenerateSchemaRequest(topic = magazineTopic, templateVariant = currentVariant)
                val response = ApiClient.retrofitService.generateSchema(geminiKey, request)
                if (response.isSuccessful) {
                    val schema = response.body()
                    if (schema != null) {
                        _schemaState.value = SchemaState.Success(schema)
                    } else {
                        _schemaState.value = SchemaState.Error("Received empty schema")
                    }
                } else {
                    _schemaState.value = SchemaState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _schemaState.value = SchemaState.Error(e.message ?: "Unknown network error")
            }
        }
    }

    fun generateLatex(schema: MagazineSchema) {
        _latexState.value = LatexState.Loading
        
        viewModelScope.launch {
            try {
                val request = GenerateLatexRequest(schema = schema, templateVariant = currentVariant)
                val response = ApiClient.retrofitService.generateLatex(currentApiKey, request)
                if (response.isSuccessful) {
                    val latex = response.body()?.latexCode
                    if (latex != null) {
                        _latexState.value = LatexState.Success(latex)
                    } else {
                        _latexState.value = LatexState.Error("Received empty latex")
                    }
                } else {
                    _latexState.value = LatexState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _latexState.value = LatexState.Error(e.message ?: "Unknown network error")
            }
        }
    }

    fun compileRaw(context: Context, latexCode: String, topic: String = "magazine", variant: String = "variant", fromShowcase: Boolean = false) {
        this.currentTopic = topic
        this.currentVariant = variant
        this.isFromShowcase = fromShowcase
        _compileState.value = CompileState.Loading(0, "Starting compile job...")
        
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
        while (isPolling) {
            try {
                val response = ApiClient.retrofitService.getJobStatus(jobId)
                if (response.isSuccessful) {
                    val statusObj = response.body()
                    if (statusObj != null) {
                        if (statusObj.status == "COMPLETED") {
                            _compileState.value = CompileState.Loading(100, "Downloading PDF...")
                            downloadPdf(context, jobId)
                            isPolling = false
                        } else if (statusObj.status == "FAILED") {
                            _compileState.value = CompileState.Error(statusObj.error ?: "Job failed on server")
                            isPolling = false
                        } else {
                            _compileState.value = CompileState.Loading(statusObj.progress, "Generating... ${statusObj.progress}%")
                            delay(5000) // Poll every 5 seconds
                        }
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

            if (response.isSuccessful && coverResponse.isSuccessful) {
                val pdfBytes = response.body()?.bytes()
                val coverBytes = coverResponse.body()?.bytes()
                
                if (pdfBytes != null && coverBytes != null) {
                    val file = savePdfToDisk(context, pdfBytes)
                    
                    _compileState.value = CompileState.Loading(100, "Publishing to Showcase...")
                    
                    if (!isFromShowcase) {
                        try {
                            val currentLatex = (_latexState.value as? LatexState.Success)?.latex ?: ""
                            
                            // Publish to showcase with the raw latex code
                            val showcaseItem = com.magazineforge.app.models.ShowcaseItem(
                                title = currentTopic,
                                templateVariant = currentVariant,
                                latexCode = currentLatex
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
    
    private suspend fun savePdfToDisk(context: Context, bytes: ByteArray): File {
        return withContext(Dispatchers.IO) {
            val previewFile = File(context.cacheDir, "magazine_preview.pdf")
            FileOutputStream(previewFile).use { it.write(bytes) }
            
            try {
                val dir = File(context.filesDir, "magazines")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val safeTopic = currentTopic.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val filename = "magazine_${System.currentTimeMillis()}_${safeTopic}.pdf"
                val persistentFile = File(dir, filename)
                FileOutputStream(persistentFile).use { it.write(bytes) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            previewFile
        }
    }
}
