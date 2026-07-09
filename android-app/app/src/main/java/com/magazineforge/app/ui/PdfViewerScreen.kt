package com.magazineforge.app.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUrlOrPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val density = LocalDensity.current.density
    var currentFile by remember { mutableStateOf<File?>(null) }
    val renderMutex = remember { Mutex() }
    
    LaunchedEffect(pdfUrlOrPath) {
        isLoading = true
        errorMessage = null
        try {
            val file = withContext(Dispatchers.IO) {
                if (pdfUrlOrPath.startsWith("http")) {
                    val tempFile = File(context.cacheDir, "temp_viewer_${System.currentTimeMillis()}.pdf")
                    val url = URL(pdfUrlOrPath)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("Authorization", "Bearer ${com.magazineforge.app.BuildConfig.HF_TOKEN}")
                    connection.connect()
                    
                    connection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                } else {
                    File(pdfUrlOrPath)
                }
            }
            currentFile = file
            
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer
            pageCount = renderer.pageCount
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load PDF"
        } finally {
            isLoading = false
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Magazine Viewer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (currentFile != null) {
                        IconButton(onClick = {
                            try {
                                val resolver = context.contentResolver
                                val contentValues = android.content.ContentValues().apply {
                                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "magazine_${System.currentTimeMillis()}.pdf")
                                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/MagazineForge")
                                }
                                val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
                                if (uri != null) {
                                    resolver.openOutputStream(uri)?.use { outputStream ->
                                        currentFile!!.inputStream().use { inputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }
                                    android.widget.Toast.makeText(context, "Saved to Downloads/MagazineForge", android.widget.Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Failed to save: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pageCount) { index ->
                        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                        
                        LaunchedEffect(index) {
                            withContext(Dispatchers.IO) {
                                pdfRenderer?.let { renderer ->
                                    renderMutex.withLock {
                                        val page = renderer.openPage(index)
                                        // Render at 2x resolution for better quality
                                        val width = (page.width * density * 1.5).toInt()
                                        val height = (page.height * density * 1.5).toInt()
                                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                        // Fill white background first
                                        bmp.eraseColor(android.graphics.Color.WHITE)
                                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        page.close()
                                        bitmap = bmp
                                    }
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 0.75f)
                                .background(Color.White)
                        ) {
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
