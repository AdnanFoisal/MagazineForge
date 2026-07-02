package com.magazineforge.app

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
import com.magazineforge.app.ui.EditorScreen
import com.magazineforge.app.ui.EditorViewModel
import com.magazineforge.app.ui.OnboardingScreen
import com.magazineforge.app.ui.PdfViewerScreen
import com.magazineforge.app.ui.TemplateGalleryScreen
import com.magazineforge.app.ui.MyMagazinesScreen
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: EditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[EditorViewModel::class.java]

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("onboarding") }
                    var selectedTemplate by remember { mutableStateOf("") }
                    var apiKey by remember { mutableStateOf("") }
                    var selectedPdfForViewer by remember { mutableStateOf<File?>(null) }
                    
                    val compileState by viewModel.compileState.collectAsState()

                    if (selectedPdfForViewer != null) {
                        PdfViewerScreen(
                            pdfFile = selectedPdfForViewer!!,
                            onBack = {
                                selectedPdfForViewer = null
                            }
                        )
                    } else if (compileState is CompileState.Loading) {
                        val loadingState = compileState as CompileState.Loading
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    progress = loadingState.progress / 100f,
                                    color = androidx.compose.ui.graphics.Color(0xFFB87333)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(loadingState.message)
                            }
                        }
                    } else if (compileState is CompileState.Success) {
                        PdfViewerScreen(
                            pdfFile = (compileState as CompileState.Success).pdfFile,
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
                        when (currentScreen) {
                            "onboarding" -> OnboardingScreen(onVerifyClicked = { key ->
                                apiKey = key
                                currentScreen = "gallery"
                            })
                            "gallery" -> TemplateGalleryScreen(
                                onTemplateSelected = { template ->
                                    selectedTemplate = template
                                    currentScreen = "editor"
                                },
                                onLibraryClicked = {
                                    currentScreen = "library"
                                }
                            )
                            "editor" -> EditorScreen(
                                templateVariant = selectedTemplate,
                                onCompileClicked = { magazineTopic, pages ->
                                    viewModel.compileMagazine(this@MainActivity, apiKey, magazineTopic, pages, selectedTemplate)
                                },
                                onBack = {
                                    currentScreen = "gallery"
                                }
                            )
                            "library" -> MyMagazinesScreen(
                                onBack = {
                                    currentScreen = "gallery"
                                },
                                onMagazineSelected = { pdfFile ->
                                    selectedPdfForViewer = pdfFile
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
