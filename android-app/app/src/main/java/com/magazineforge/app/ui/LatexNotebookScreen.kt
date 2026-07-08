package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.magazineforge.app.ui.CompileState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent

class SearchHighlightTransformation(private val searchQuery: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (searchQuery.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text.text)
        var startIndex = 0
        while (startIndex < text.length) {
            val index = text.text.indexOf(searchQuery, startIndex, ignoreCase = true)
            if (index == -1) break
            builder.addStyle(
                style = SpanStyle(background = Color.Yellow, color = Color.Black),
                start = index,
                end = index + searchQuery.length
            )
            startIndex = index + searchQuery.length
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
    schemaState: com.magazineforge.app.ui.SchemaState? = null,
    onCompile: (String) -> Unit,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onRewrite: ((String, String, (String?) -> Unit) -> Unit)? = null
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialLatex)) }
    val latexCode = textFieldValue.text
    var isRewriting by remember { mutableStateOf(false) }
    var isVisualMode by remember { mutableStateOf(false) }

    var undoStack by remember { mutableStateOf(listOf<String>()) }
    var redoStack by remember { mutableStateOf(listOf<String>()) }

    fun updateLatex(newCode: String) {
        if (newCode != latexCode) {
            if (newCode.length - latexCode.length > 5 || newCode.endsWith(" ") || newCode.endsWith("\n")) {
                if (undoStack.lastOrNull() != latexCode) {
                    undoStack = (undoStack + latexCode).takeLast(50)
                    redoStack = emptyList()
                }
            }
            onCodeChange(newCode)
        }
    }
    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    data class OutlineItem(val title: String, val lineIndex: Int)
    val outlineItems = remember(latexCode) {
        val items = mutableListOf<OutlineItem>()
        latexCode.lines().forEachIndexed { index, line ->
            if (line.startsWith("% --- ")) {
                val title = line.removePrefix("% --- ").removeSuffix(" ---").trim()
                items.add(OutlineItem(title, index))
            }
        }
        items
    }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-tex")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(latexCode.toByteArray())
            }
        }
    }


    
    val bgCream = Color(0xFFFDFCEB)
    val gutterColor = Color(0xFFEFEFEF)
    val textDark = Color(0xFF2C2C2C)
    val tabGreen = Color(0xFF4CAF50)
    
    val scrollState = rememberScrollState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = Color.White
            ) {
                Text("Document Outline", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                Divider()
                outlineItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.title, fontSize = 14.sp) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                // Scroll to line
                                val lineHeightPx = 20 * context.resources.displayMetrics.scaledDensity
                                scrollState.animateScrollTo((item.lineIndex * lineHeightPx).toInt())
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }
            }
        }
    ) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("magazine.tex", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            
                            if (compileState != null && compileState !is CompileState.Idle && compileState !is CompileState.Loading) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val isSuccess = compileState is CompileState.Success
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (isSuccess) tabGreen else Color.Red)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSuccess) "Success" else "Failed",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        Row {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Outline", tint = Color.Black)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onCompile(latexCode) }, enabled = !isCompiling) {
                            if (isCompiling) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = tabGreen, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Compile", tint = tabGreen)
                            }
                        }
                        IconButton(onClick = { showFindReplace = !showFindReplace }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.DarkGray)
                        }
                        IconButton(onClick = { isVisualMode = !isVisualMode }) {
                            Icon(if (isVisualMode) Icons.Default.Code else Icons.Default.Visibility, contentDescription = "Toggle Mode", tint = Color.DarkGray)
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.DarkGray)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Copy Code") },
                                    onClick = {
                                        showMenu = false
                                        clipboardManager.setText(AnnotatedString(latexCode))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Editor") },
                                    onClick = {
                                        showMenu = false
                                        undoStack = undoStack + latexCode
                                        val newCode = ""
                                        textFieldValue = TextFieldValue(newCode)
                                        updateLatex(newCode)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset to Template") },
                                    onClick = {
                                        showMenu = false
                                        undoStack = undoStack + latexCode
                                        textFieldValue = TextFieldValue(initialLatex)
                                        updateLatex(initialLatex)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = {
                                        showMenu = false
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, latexCode)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    }
                                )
                            }
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp)
                ) {
                    Column {
                        Text("*magazine.tex", color = Color.Black, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        Box(modifier = Modifier.height(2.dp).width(100.dp).background(tabGreen))
                    }
                }
                Divider(color = Color.LightGray, thickness = 1.dp)
                if (showFindReplace) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = findText,
                            onValueChange = { findText = it },
                            label = { Text("Find", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp)
                        )
                        OutlinedTextField(
                            value = replaceText,
                            onValueChange = { replaceText = it },
                            label = { Text("Replace", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp)
                        )
                        Button(
                            onClick = {
                                if (findText.isNotEmpty()) {
                                    val updated = latexCode.replace(findText, replaceText)
                                    undoStack = undoStack + latexCode
                                    textFieldValue = TextFieldValue(updated)
                                    updateLatex(updated)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tabGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Replace", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Divider(color = Color.LightGray, thickness = 1.dp)
                }
            }
        },
        bottomBar = {
            Column {
                if (compileState is CompileState.Error && compileState.message.startsWith("LINE_ERROR:")) {
                    val parts = compileState.message.removePrefix("LINE_ERROR:").split(":", limit = 2)
                    if (parts.size == 2) {
                        val lineNum = parts[0].toIntOrNull() ?: 1
                        val errMsg = parts[1]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Line $lineNum: $errMsg", color = Color.Red, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    val lineHeightPx = 20 * context.resources.displayMetrics.scaledDensity
                                    scrollState.animateScrollTo(((lineNum - 1) * lineHeightPx).toInt())
                                }
                            }) {
                                Text("Jump", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Divider(color = Color.LightGray, thickness = 1.dp)
                    }
                }
                BottomAppBar(
                containerColor = Color(0xFFF5F5F5),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) Color.Black else Color.Gray, modifier = Modifier.size(20.dp).clickable(enabled = undoStack.isNotEmpty()) {
                        if (undoStack.isNotEmpty()) {
                            redoStack = redoStack + latexCode
                            val prev = undoStack.last()
                            undoStack = undoStack.dropLast(1)
                            textFieldValue = TextFieldValue(prev)
                            updateLatex(prev)
                        }
                    })
                    
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) Color.Black else Color.Gray, modifier = Modifier.size(20.dp).clickable(enabled = redoStack.isNotEmpty()) {
                        if (redoStack.isNotEmpty()) {
                            undoStack = undoStack + latexCode
                            val next = redoStack.last()
                            redoStack = redoStack.dropLast(1)
                            textFieldValue = TextFieldValue(next)
                            updateLatex(next)
                        }
                    })
                    
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black, modifier = Modifier.size(20.dp).clickable { showFindReplace = !showFindReplace })
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(20.dp).clickable { createDocumentLauncher.launch("magazine.tex") })
                }
            }
        },
        containerColor = bgCream
    ) { paddingValues ->
        if (isVisualMode) {
            VisualEditor(latexCode, schemaState, onPatchAndRecompile = { newCode ->
                updateLatex(newCode)
                textFieldValue = TextFieldValue(newCode)
            })
        } else {
            val lineCount = latexCode.count { it == '\n' } + 1
            val lineNumbersText = (1..lineCount).joinToString("\n")

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(40.dp)
                        .background(gutterColor)
                        .padding(top = 16.dp, bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(end = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = lineNumbersText,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Divider(color = Color.LightGray, modifier = Modifier.width(1.dp).fillMaxHeight())
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(bgCream)
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newVal: TextFieldValue ->
                            updateLatex(newVal.text)
                            textFieldValue = newVal
                        },
                        visualTransformation = SearchHighlightTransformation(findText),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .horizontalScroll(rememberScrollState()),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = textDark,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(tabGreen)
                    )
                    
                    if (textFieldValue.selection.length > 0 && onRewrite != null) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(4.dp)) {
                            if (isRewriting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp), color = tabGreen)
                            } else {
                                Row {
                                    listOf("Rewrite", "Punch up", "Shorten").forEach { action ->
                                        TextButton(onClick = {
                                            isRewriting = true
                                            val selectedText = textFieldValue.text.substring(textFieldValue.selection.start, textFieldValue.selection.end)
                                            onRewrite(selectedText, action) { rewritten ->
                                                isRewriting = false
                                                if (rewritten != null) {
                                                    val prefix = textFieldValue.text.substring(0, textFieldValue.selection.start)
                                                    val suffix = textFieldValue.text.substring(textFieldValue.selection.end)
                                                    val newCode = prefix + rewritten + suffix
                                                    undoStack = undoStack + latexCode
                                                    textFieldValue = TextFieldValue(newCode, selection = TextRange(prefix.length, prefix.length + rewritten.length))
                                                    updateLatex(newCode)
                                                }
                                            }
                                        }) {
                                            Text(action, color = tabGreen, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

data class TapZone(val field: String, val left: Float, val top: Float, val right: Float, val bottom: Float)

val templateTapZones = mapOf(
    "cover" to listOf(
        TapZone("main_title", 0.05f, 0.05f, 0.95f, 0.25f),
        TapZone("subtitle", 0.05f, 0.25f, 0.95f, 0.35f),
        TapZone("image_url", 0.0f, 0.35f, 1.0f, 1.0f)
    ),
    "article" to listOf(
        TapZone("headline", 0.05f, 0.05f, 0.95f, 0.15f),
        TapZone("subheadline", 0.05f, 0.15f, 0.95f, 0.25f),
        TapZone("body_copy", 0.05f, 0.25f, 0.95f, 0.95f)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualEditor(
    latexCode: String,
    schemaState: com.magazineforge.app.ui.SchemaState?,
    onPatchAndRecompile: (String) -> Unit
) {
    var currentPage by remember { mutableStateOf(1) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var selectedZone by remember { mutableStateOf<TapZone?>(null) }
    var editValue by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(currentPage, latexCode) {
        isLoading = true
        errorMsg = null
        try {
            val request = com.magazineforge.app.models.RenderPageRequest(latexCode = latexCode, pageNumber = currentPage)
            val response = com.magazineforge.app.network.ApiClient.retrofitService.renderPage(request)
            if (response.isSuccessful) {
                imageBytes = response.body()?.bytes()
            } else {
                errorMsg = "Failed to render page"
            }
        } catch (e: Exception) {
            errorMsg = e.message
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (currentPage > 1) currentPage-- }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Prev Page")
                }
                Text("Page $currentPage")
                IconButton(onClick = { currentPage++ }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Page")
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMsg != null) {
                    Text(errorMsg!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                } else if (imageBytes != null) {
                    
                    
                    AsyncImage(
                        model = imageBytes,
                        contentDescription = "Rendered Page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    
                    // Invisible clickable overlays based on template
                    val zones = if (currentPage == 1) templateTapZones["cover"] else templateTapZones["article"]
                    zones?.forEach { zone ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = (zone.left * 300).dp, // Placeholder sizing
                                    top = (zone.top * 400).dp
                                )
                                .clickable {
                                    selectedZone = zone
                                    editValue = "" // Try to extract existing text if possible, or just start empty
                                    showSheet = true
                                    scope.launch { sheetState.show() }
                                }
                        )
                    }
                }
            }
        }

        if (showSheet && selectedZone != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Edit ${selectedZone?.field}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Simple string replacement for now. In a real app we'd regex or update schema.
                            // Assuming the text edit is just prepending/appending for safety, or if we extract it, we replace it.
                            val newCode = latexCode + "\n% Edit: ${selectedZone?.field} -> $editValue"
                            onPatchAndRecompile(newCode)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showSheet = false
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
