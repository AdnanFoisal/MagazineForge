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
import com.magazineforge.app.utils.SecureStorage
import com.magazineforge.app.network.ApiClient
import com.magazineforge.app.models.VerifyKeyRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.magazineforge.app.ui.theme.LuxeEditorialNoirTheme
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
            LuxeEditorialNoirTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(if (savedKey != null) "gallery" else "onboarding") }
                    var selectedTemplate by remember { mutableStateOf("") }
                    var apiKey by remember { mutableStateOf(savedKey ?: "") }
                    var selectedPdfForViewer by remember { mutableStateOf<File?>(null) }
                    
                    val coroutineScope = rememberCoroutineScope()
                    var isVerifying by remember { mutableStateOf(false) }
                    var verifyError by remember { mutableStateOf<String?>(null) }
                    
                    val compileState by viewModel.compileState.collectAsState()
                    var showExitDialog by remember { mutableStateOf(false) }
                    var showProgressCard by remember { mutableStateOf(false) }

                    LaunchedEffect(compileState) {
                        if (compileState is CompileState.Loading) {
                            showProgressCard = true
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (selectedPdfForViewer != null) {
                            PdfViewerScreen(
                                pdfFile = selectedPdfForViewer!!,
                                onBack = {
                                    selectedPdfForViewer = null
                                }
                            )
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
                                                    currentScreen = "gallery"
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
                                    isCompileLoading = isCompileLoading,
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
                                    "editor", "library" -> {
                                        currentScreen = "gallery"
                                    }
                                    "gallery", "onboarding" -> {
                                        showExitDialog = true
                                    }
                                }
                            }
                        }

                        // Exit confirmation dialog
                        if (showExitDialog) {
                            AlertDialog(
                                onDismissRequest = { showExitDialog = false },
                                title = { Text("Exit MagBoy", style = LuxeTypography.headlineSmall) },
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
