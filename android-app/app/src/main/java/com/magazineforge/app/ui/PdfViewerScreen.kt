package com.magazineforge.app.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.magazineforge.app.network.ApiClient
import java.io.File
import java.io.FileOutputStream

private const val MIN_PDF_SCALE = 1f
private const val MAX_PDF_SCALE = 5f

/** Pages kept rendered either side of the visible window, so a flick doesn't show blanks. */
private const val PDF_PAGE_SLACK = 1

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
    val coroutineScope = rememberCoroutineScope()
    var currentFile by remember { mutableStateOf<File?>(null) }
    val renderMutex = remember { Mutex() }

    val listState = rememberLazyListState()
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Only the pages near the viewport are held. Previously every page was
    // rendered into its own ARGB_8888 bitmap and kept for the lifetime of the
    // item, which a large issue could not survive.
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    LaunchedEffect(pdfUrlOrPath) {
        isLoading = true
        errorMessage = null
        // Tear down the previous document. Closing under the mutex waits for any
        // in-flight page render instead of pulling the renderer out from under it.
        renderMutex.withLock {
            pdfRenderer?.close()
            pdfRenderer = null
        }
        pageBitmaps.clear()
        pageCount = 0
        scale = 1f
        offset = Offset.Zero
        try {
            val file = withContext(Dispatchers.IO) {
                if (pdfUrlOrPath.startsWith("http")) {
                    val tempFile = File(context.cacheDir, "temp_viewer_${System.currentTimeMillis()}.pdf")
                    // Goes through ApiClient.okHttpClient rather than a
                    // hand-rolled HttpURLConnection for two reasons:
                    //
                    //  1. Credentials. This used to attach HF_TOKEN to whatever
                    //     URL it was handed. Showcase PDF URLs come from a
                    //     client-writable Firestore collection, so a hostile
                    //     record could point at any server and collect the
                    //     token. ApiClient's interceptor only attaches
                    //     credentials when the host matches BASE_URL.
                    //  2. It sends X-App-Key, which the raw connection never
                    //     did — so this path keeps working once BASE_URL points
                    //     at the Worker proxy instead of the Space.
                    val request = okhttp3.Request.Builder().url(pdfUrlOrPath).build()
                    ApiClient.okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw java.io.IOException("Failed to download PDF (HTTP ${response.code})")
                        }
                        val body = response.body ?: throw java.io.IOException("Empty response body")
                        body.byteStream().use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
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
            pageBitmaps.clear()
        }
    }

    // The page range worth holding in memory, derived from what the list is
    // actually showing. IntRange has value equality, so the effect below only
    // restarts when the window genuinely moves.
    val renderWindow by remember(pageCount) {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty() || pageCount <= 0) {
                IntRange.EMPTY
            } else {
                val first = (visible.first().index - PDF_PAGE_SLACK).coerceAtLeast(0)
                val last = (visible.last().index + PDF_PAGE_SLACK).coerceAtMost(pageCount - 1)
                first..last
            }
        }
    }

    LaunchedEffect(renderWindow, pdfRenderer) {
        val renderer = pdfRenderer ?: return@LaunchedEffect

        // Release pages that scrolled out of the window. The bitmaps are not
        // recycle()d on purpose: at minSdk 33 the pixel data sits on the native
        // heap owned by the Bitmap object, so dropping the last reference is
        // enough, whereas recycle() could hit a bitmap a frame is still drawing.
        pageBitmaps.keys.filter { it !in renderWindow }.forEach { pageBitmaps.remove(it) }

        for (index in renderWindow) {
            if (pageBitmaps.containsKey(index)) continue
            val bmp = withContext(Dispatchers.IO) {
                renderMutex.withLock {
                    try {
                        val page = renderer.openPage(index)
                        try {
                            val width = (page.width * density * 1.5).toInt()
                            val height = (page.height * density * 1.5).toInt()
                            val target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            target.eraseColor(android.graphics.Color.WHITE)
                            page.render(target, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            target
                        } finally {
                            page.close()
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            if (bmp != null) {
                pageBitmaps[index] = bmp
            }
        }
    }

    androidx.activity.compose.BackHandler(onBack = onBack)

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

                        IconButton(onClick = {
                            coroutineScope.launch {
                                try {
                                    // PdfRenderer allows only one open page at a
                                    // time, so this shares the render mutex with
                                    // the page-window renderer.
                                    val rendered = renderMutex.withLock {
                                        val page = pdfRenderer?.openPage(0)
                                        if (page == null) null else {
                                            try {
                                                val w = (page.width * density * 2).toInt()
                                                val h = (page.height * density * 2).toInt()
                                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                                bmp.eraseColor(android.graphics.Color.WHITE)
                                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                bmp
                                            } finally {
                                                page.close()
                                            }
                                        }
                                    }
                                    if (rendered != null) {
                                        val pageBitmap = rendered
                                        val width = pageBitmap.width
                                        val height = pageBitmap.height

                                        // Mockup container
                                        val mWidth = width + 400
                                        val mHeight = height + 400
                                        val mockupBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
                                        val canvas = android.graphics.Canvas(mockupBitmap)
                                        canvas.drawColor(android.graphics.Color.parseColor("#0F0F10")) // Dark theme bg
                                        
                                        // Shadow
                                        val paint = android.graphics.Paint()
                                        paint.setShadowLayer(80f, 0f, 40f, android.graphics.Color.parseColor("#90000000"))
                                        val rect = android.graphics.RectF(200f, 200f, 200f + width, 200f + height)
                                        canvas.drawRect(rect, paint)
                                        
                                        // Draw the rendered page
                                        canvas.drawBitmap(pageBitmap, 200f, 200f, null)
                                        
                                        // Save and share
                                        val resolver = context.contentResolver
                                        val contentValues = android.content.ContentValues().apply {
                                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "mockup_${System.currentTimeMillis()}.png")
                                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/MagazineForge")
                                        }
                                        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                        if (uri != null) {
                                            resolver.openOutputStream(uri)?.use { out ->
                                                mockupBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                            }
                                            
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Magazine"))
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Failed to generate mockup", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    // Reset when zoomed in, otherwise jump to a
                                    // readable zoom on the tapped page.
                                    if (scale > MIN_PDF_SCALE) {
                                        scale = MIN_PDF_SCALE
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2f
                                        offset = Offset.Zero
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            // Watched on the Initial pass, which runs parent
                            // before child. detectTransformGestures would be
                            // useless here: it aborts as soon as any change is
                            // consumed, and the LazyColumn's scroll consumes
                            // drags on the Main pass (child before parent), so
                            // a pinch would never survive. Consuming here only
                            // when a real transform happens leaves ordinary
                            // one-finger scrolling untouched.
                            awaitEachGesture {
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val down = event.changes.count { it.pressed }
                                    val zoom = if (down >= 2) event.calculateZoom() else 1f
                                    val pan = if (down >= 2 || scale > MIN_PDF_SCALE) {
                                        event.calculatePan()
                                    } else {
                                        Offset.Zero
                                    }

                                    if (zoom != 1f || pan != Offset.Zero) {
                                        scale = (scale * zoom).coerceIn(MIN_PDF_SCALE, MAX_PDF_SCALE)
                                        offset = if (scale > MIN_PDF_SCALE) {
                                            // Keep the page from being flung off
                                            // screen: translation is bounded by
                                            // the overhang the zoom created.
                                            val maxX = (size.width * (scale - 1f)) / 2f
                                            val maxY = (size.height * (scale - 1f)) / 2f
                                            Offset(
                                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                                            )
                                        } else {
                                            // At native scale the list owns
                                            // scrolling, so there is nothing to pan.
                                            Offset.Zero
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pageCount) { index ->
                            val bitmap = pageBitmaps[index]

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        if (bitmap != null) bitmap.width.toFloat() / bitmap.height.toFloat() else 0.75f
                                    )
                                    .background(Color.White)
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
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
}
