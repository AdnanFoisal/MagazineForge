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
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatexNotebookScreen(
    initialLatex: String,
    isCompiling: Boolean,
    aiRawState: LatexState,
    onGenerateRawLatex: (String) -> Unit,
    onCompile: (String) -> Unit,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit
) {
    var latexCode by remember { mutableStateOf(initialLatex) }
    var showAiDialog by remember { mutableStateOf(false) }
    var aiMagName by remember { mutableStateOf("") }
    var aiTopic by remember { mutableStateOf("") }
    var aiNumArticles by remember { mutableStateOf("") }
    var aiImages by remember { mutableStateOf("") }
    var aiStyle by remember { mutableStateOf("") }
    
    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Auto-update editor when AI finishes
    LaunchedEffect(aiRawState) {
        if (aiRawState is LatexState.Success) {
            latexCode = aiRawState.latexCode
            onCodeChange(aiRawState.latexCode)
            showAiDialog = false
        }
    }
    
    val bgCream = Color(0xFFFDFCEB)
    val gutterColor = Color(0xFFEFEFEF)
    val textDark = Color(0xFF2C2C2C)
    val tabGreen = Color(0xFF4CAF50)
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("magazine.tex", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
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
                                        latexCode = ""
                                        onCodeChange("")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset to Template") },
                                    onClick = {
                                        showMenu = false
                                        latexCode = initialLatex
                                        onCodeChange(initialLatex)
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
                // Tab Row Mock
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp)
                ) {
                    Column {
                        Text("*magazine.tex", color = Color.Black, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        Box(modifier = Modifier.height(2.dp).width(100.dp).background(tabGreen))
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
                                    latexCode = updated
                                    onCodeChange(updated)
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
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(20.dp).clickable { showFindReplace = !showFindReplace })
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        },
        containerColor = bgCream
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val lineCount = latexCode.count { it == '\n' } + 1
        val lineNumbersText = (1..lineCount).joinToString("\n")

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Gutter for line numbers
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
            
            // Editor area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(bgCream)
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = latexCode,
                    onValueChange = {
                        latexCode = it
                        onCodeChange(it)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .horizontalScroll(rememberScrollState()),
                    softWrap = false,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = textDark,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(tabGreen)
                )
            }
        }
        
        // AI Generation FAB
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showAiDialog = true },
                containerColor = Color(0xFFC5A059),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Generate")
            }
        }
        
        // AI Prompt Dialog
        if (showAiDialog) {
            AlertDialog(
                onDismissRequest = { showAiDialog = false },
                title = { Text("AI Raw Generation") },
                text = {
                    Column {
                        Text("Fill out ALL fields to generate a structured magazine in LuaLaTeX.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = aiMagName,
                            onValueChange = { aiMagName = it },
                            label = { Text("Magazine Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = aiTopic,
                            onValueChange = { aiTopic = it },
                            label = { Text("Topic (e.g. Travel, Tech)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = aiNumArticles,
                            onValueChange = { aiNumArticles = it.filter { char -> char.isDigit() } },
                            label = { Text("Number of Articles") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = aiImages,
                            onValueChange = { aiImages = it },
                            label = { Text("Preferred Images (URLs or Keywords)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = aiStyle,
                            onValueChange = { aiStyle = it },
                            label = { Text("Style / Vibe") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (aiRawState is LatexState.Loading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (aiRawState is LatexState.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(aiRawState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val constructedPrompt = """
                                Magazine Name: $aiMagName
                                Topic: $aiTopic
                                Number of Articles: $aiNumArticles
                                Images/Assets: $aiImages
                                Style/Vibe: $aiStyle
                                
                                Write the complete, robust LuaLaTeX document for this magazine. It MUST be a single compilable document. Do NOT output JSON. Use --shell-escape friendly \luacode* curl blocks to fetch the provided images if they are URLs.
                            """.trimIndent()
                            onGenerateRawLatex(constructedPrompt)
                        },
                        enabled = aiRawState !is LatexState.Loading && aiMagName.isNotBlank() && aiTopic.isNotBlank() && aiNumArticles.isNotBlank() && aiImages.isNotBlank() && aiStyle.isNotBlank()
                    ) {
                        Text("Generate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAiDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
