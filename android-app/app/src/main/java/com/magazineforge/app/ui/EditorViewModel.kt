package com.magazineforge.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magazineforge.app.models.JobRequest
import com.magazineforge.app.models.PageRequest
import com.magazineforge.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class CompileState {
    object Idle : CompileState()
    data class Loading(val progress: Int, val message: String = "Generating...") : CompileState()
    data class Success(val pdfFile: File) : CompileState()
    data class Error(val message: String) : CompileState()
}

class EditorViewModel : ViewModel() {
    private val _compileState = MutableStateFlow<CompileState>(CompileState.Idle)
    val compileState: StateFlow<CompileState> = _compileState
    
    var currentTopic: String = "Untitled"
    
    fun resetState() {
        _compileState.value = CompileState.Idle
    }

    fun compileMagazine(
        context: Context,
        geminiKey: String,
        magazineTopic: String,
        pages: List<PageBlock>,
        templateName: String
    ) {
        currentTopic = magazineTopic
        _compileState.value = CompileState.Loading(0, "Starting job...")
        
        // Extract variant from templateName (e.g. "cover_template_a" -> "a")
        val variant = templateName.split("_").lastOrNull() ?: "a"
        
        val pageRequests = pages.map { 
            PageRequest(type = it.type, topic = it.topic, imageUrl = it.imageUrl.takeIf { url -> url.isNotBlank() })
        }
        val jobRequest = JobRequest(magazineTopic, variant, pageRequests)
        
        viewModelScope.launch {
            try {
                val response = ApiClient.retrofitService.createJob(geminiKey, jobRequest)
                if (response.isSuccessful) {
                    val jobId = response.body()?.job_id
                    if (jobId != null) {
                        pollJobStatus(context, jobId)
                    } else {
                        _compileState.value = CompileState.Error("Invalid job ID received")
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message()
                    _compileState.value = CompileState.Error("Job Creation Error: ${response.code()} $errorMsg")
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
            val response = ApiClient.retrofitService.downloadJob(jobId)
            if (response.isSuccessful) {
                val pdfBytes = response.body()?.bytes()
                if (pdfBytes != null) {
                    val file = savePdfToDisk(context, pdfBytes)
                    _compileState.value = CompileState.Success(file)
                } else {
                    _compileState.value = CompileState.Error("Received empty PDF bytes")
                }
            } else {
                _compileState.value = CompileState.Error("Download Error: ${response.code()}")
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
