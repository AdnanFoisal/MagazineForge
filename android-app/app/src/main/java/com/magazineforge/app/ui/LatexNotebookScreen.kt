package com.magazineforge.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magazineforge.app.ui.theme.DarkSurface
import com.magazineforge.app.ui.theme.GhostWhite
import com.magazineforge.app.ui.theme.GoldBright
import com.magazineforge.app.ui.theme.PitchBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatexNotebookScreen(
    initialLatex: String,
    isCompiling: Boolean,
    onCompile: (String) -> Unit,
    onBack: () -> Unit
) {
    var latexCode by remember { mutableStateOf(initialLatex) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raw LaTeX Editor", color = GoldBright) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = GhostWhite)
                    }
                },
                actions = {
                    Button(
                        onClick = { onCompile(latexCode) },
                        enabled = !isCompiling,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldBright)
                    ) {
                        Text(if (isCompiling) "Compiling..." else "Compile", color = PitchBlack)
                    }
                }
            )
        },
        containerColor = PitchBlack
    ) { paddingValues ->
        TextField(
            value = latexCode,
            onValueChange = { latexCode = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = GhostWhite
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                cursorColor = GoldBright,
                focusedIndicatorColor = GoldBright
            )
        )
    }
}
