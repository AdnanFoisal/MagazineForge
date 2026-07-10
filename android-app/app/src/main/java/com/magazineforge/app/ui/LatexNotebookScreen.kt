package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

// --- SYNTAX HIGHLIGHTING ---
class LatexSyntaxTransformation(private val searchQuery: String = "") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        val code = text.text

        // 1. Highlight LaTeX Commands (e.g., \begin, \section)
        val commandMatcher = Pattern.compile("\\\\[a-zA-Z]+").matcher(code)
        while (commandMatcher.find()) {
            builder.addStyle(
                style = SpanStyle(color = Color(0xFFC678DD), fontWeight = FontWeight.SemiBold), // Purple
                start = commandMatcher.start(),
                end = commandMatcher.end()
            )
        }

        // 2. Highlight Brackets and Braces {} []
        val braceMatcher = Pattern.compile("[{}\\[\\]]").matcher(code)
        while (braceMatcher.find()) {
            builder.addStyle(
                style = SpanStyle(color = Color(0xFF61AFEF)), // Blue
                start = braceMatcher.start(),
                end = braceMatcher.end()
            )
        }

        // 3. Highlight Comments %
        val commentMatcher = Pattern.compile("%.*").matcher(code)
        while (commentMatcher.find()) {
            builder.addStyle(
                style = SpanStyle(color = Color(0xFFAAB2BD), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), // Lighter Grey
                start = commentMatcher.start(),
                end = commentMatcher.end()
            )
        }

        // 4. Highlight Search Query
        if (searchQuery.isNotEmpty()) {
            var startIndex = 0
            while (startIndex < code.length) {
                val index = code.indexOf(searchQuery, startIndex, ignoreCase = true)
                if (index == -1) break
                builder.addStyle(
                    style = SpanStyle(background = Color(0xFFE5C07B), color = Color(0xFF282C34)), // Yellow bg, Dark text
                    start = index,
                    end = index + searchQuery.length
                )
                startIndex = index + searchQuery.length
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatexNotebookScreen(
    initialLatex: String,
    isCompiling: Boolean,
    compileState: CompileState? = null,
    schemaState: SchemaState? = null,
    onCompile: (String) -> Unit,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onRewrite: ((String, String, (String?) -> Unit) -> Unit)? = null
) {
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current
    
    // Editor State
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialLatex)) }
    val latexCode = textFieldValue.text

    // History (Undo/Redo)
    var undoStack by remember { mutableStateOf(listOf<String>()) }
    var redoStack by remember { mutableStateOf(listOf<String>()) }

    fun updateLatex(newValue: TextFieldValue) {
        val newCode = newValue.text
        if (newCode != latexCode) {
            if (undoStack.isEmpty() && latexCode.isNotEmpty()) {
                undoStack = listOf(latexCode)
            }
            if (kotlin.math.abs(newCode.length - latexCode.length) > 5 || newCode.endsWith(" ") || newCode.endsWith("\n")) {
                if (undoStack.lastOrNull() != latexCode) {
                    undoStack = (undoStack + latexCode).takeLast(50)
                    redoStack = emptyList()
                }
            }
            onCodeChange(newCode)
        }
        textFieldValue = newValue
    }

    // Search State
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // Colors
    val editorBg = Color(0xFF1E1E1E) // VSCode Dark Theme background
    val gutterBg = Color(0xFF252526)
    val lineNumberColor = Color(0xFF858585)
    val textColor = Color(0xFFFFFFFF) // Pure white for better contrast
    val accentColor = tokens.primaryAccent

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("LaTeX Editor", color = accentColor, style = com.magazineforge.app.ui.theme.LuxeTypography.titleMedium)
                        Text("magboy_engine_v3", color = tokens.textSecondary, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = tokens.textPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            if (undoStack.isNotEmpty()) {
                                val prev = undoStack.last()
                                undoStack = undoStack.dropLast(1)
                                redoStack = redoStack + latexCode
                                textFieldValue = TextFieldValue(prev)
                                onCodeChange(prev)
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, "Undo", tint = if (undoStack.isNotEmpty()) tokens.textPrimary else tokens.textSecondary.copy(alpha=0.3f))
                    }
                    
                    IconButton(
                        onClick = { 
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.last()
                                redoStack = redoStack.dropLast(1)
                                undoStack = undoStack + latexCode
                                textFieldValue = TextFieldValue(next)
                                onCodeChange(next)
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Redo, "Redo", tint = if (redoStack.isNotEmpty()) tokens.textPrimary else tokens.textSecondary.copy(alpha=0.3f))
                    }

                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "Search", tint = if (showSearch) accentColor else tokens.textPrimary)
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = tokens.textPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(editorBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Editor", color = Color.Red) },
                                onClick = { 
                                    undoStack = undoStack + latexCode
                                    textFieldValue = TextFieldValue("")
                                    onCodeChange("")
                                    showMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save .tex File", color = tokens.textPrimary) },
                                onClick = { 
                                    showMenu = false
                                    saveLatexToDownloads(androidx.compose.ui.platform.LocalContext.current, latexCode)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = gutterBg)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCompile(latexCode) },
                containerColor = accentColor,
                contentColor = editorBg,
                icon = { Icon(Icons.Default.PlayArrow, "Compile") },
                text = { Text(if (isCompiling) "Compiling..." else "Build PDF") }
            )
        },
        containerColor = editorBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            if (showSearch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gutterBg)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Find...", color = tokens.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tokens.textPrimary,
                            unfocusedTextColor = tokens.textPrimary,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = editorBg,
                            unfocusedContainerColor = editorBg
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "Clear", tint = tokens.textSecondary)
                                }
                            }
                        }
                    )
                }
            }

            // Editor Area (Line Numbers + Code)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(editorBg)
            ) {
                // Gutter (Line Numbers)
                val lineCount = latexCode.count { it == '\n' } + 1
                Column(
                    modifier = Modifier
                        .background(gutterBg)
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                        .width(36.dp)
                        .verticalScroll(verticalScrollState),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            color = lineNumberColor,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.height(18.dp) // Approximate height for line
                        )
                    }
                }

                // Code Area
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { updateLatex(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState),
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(accentColor),
                    visualTransformation = LatexSyntaxTransformation(searchQuery)
                )
            }
        }
        
        if (isCompiling) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        }
    }
}

fun saveLatexToDownloads(context: android.content.Context, latexCode: String) {
    try {
        val fileName = "MagBoy_${System.currentTimeMillis()}.tex"
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/x-tex")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(latexCode.toByteArray())
            }
            android.widget.Toast.makeText(context, "Saved to Downloads: $fileName", android.widget.Toast.LENGTH_LONG).show()
        } else {
            android.widget.Toast.makeText(context, "Failed to create file in Downloads", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error saving file: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
