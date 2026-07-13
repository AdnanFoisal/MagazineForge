import re

file_path = r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\java\com\magazineforge\app\ui\PdfViewerScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

share_button_code = """
                        IconButton(onClick = {
                            coroutineScope.launch {
                                try {
                                    val page = pdfRenderer?.openPage(0)
                                    if (page != null) {
                                        val width = (page.width * density * 2).toInt()
                                        val height = (page.height * density * 2).toInt()
                                        val pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                        pageBitmap.eraseColor(android.graphics.Color.WHITE)
                                        page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        page.close()
                                        
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
                            Icon(androidx.compose.material.icons.filled.Share, contentDescription = "Share")
                        }
"""

if "Icons.Default.Share" not in content and "filled.Share" not in content:
    # We need a coroutineScope inside PdfViewerScreen
    if "val coroutineScope = rememberCoroutineScope()" not in content:
        content = content.replace("val density = LocalDensity.current.density", "val density = LocalDensity.current.density\n    val coroutineScope = rememberCoroutineScope()")
    
    # We need to add the import for Share
    if "import androidx.compose.material.icons.filled.Share" not in content:
        content = content.replace("import androidx.compose.material.icons.filled.Download", "import androidx.compose.material.icons.filled.Download\nimport androidx.compose.material.icons.filled.Share")
    
    # We need to insert the button next to the Download button
    target = """                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }"""
    
    content = content.replace(target, target + "\n" + share_button_code)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
