package com.magazineforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatexNotebookScreen(
    initialLatex: String,
    isCompiling: Boolean,
    onCompile: (String) -> Unit,
    onBack: () -> Unit
) {
    var latexCode by remember { mutableStateOf(initialLatex) }
    
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
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.DarkGray)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.DarkGray)
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
                }
                Divider(color = Color.LightGray, thickness = 1.dp)
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
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        },
        containerColor = bgCream
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Gutter for line numbers
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
                    .background(gutterColor)
            ) {
                Column(modifier = Modifier.padding(top = 16.dp, end = 4.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Text("1", color = Color.Gray, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
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
                    onValueChange = { latexCode = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
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
    }
}
