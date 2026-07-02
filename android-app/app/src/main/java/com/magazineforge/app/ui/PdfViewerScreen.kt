package com.magazineforge.app.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PdfViewerScreen(pdfFile: File, onBack: () -> Unit) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(pdfFile) {
        try {
            val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                // Render at a high resolution for clarity
                val w = page.width * 2
                val h = page.height * 2
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap = bmp
                page.close()
            }
            renderer.close()
            fd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(onClick = onBack) { Text("Back to Editor") }
        }
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Magazine Cover Preview",
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            } else {
                Text("Loading PDF...")
            }
        }
    }
}
