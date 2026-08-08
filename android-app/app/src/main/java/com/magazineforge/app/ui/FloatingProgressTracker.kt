package com.magazineforge.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magazineforge.app.ui.theme.LuxeTypography

@Composable
fun FloatingProgressTracker(
    schemaState: SchemaState,
    latexState: LatexState,
    compileState: CompileState,
    onCancel: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    val tokens = com.magazineforge.app.ui.theme.LocalThemeTokens.current

    // This tracker is the only progress surface shown during a generation run,
    // so the cancel affordance belongs here. Callers that don't pass onCancel
    // fall back to the shared EditorViewModel: LocalViewModelStoreOwner inside
    // an activity's setContent is that activity, which is the same
    // ViewModelStore MainActivity resolved EditorViewModel from, so this is the
    // same instance rather than a second one.
    val editorViewModel: EditorViewModel = viewModel()
    val runState by editorViewModel.generationRunState.collectAsState()

    val isActive = schemaState is SchemaState.Loading ||
            latexState is LatexState.Loading ||
            compileState is CompileState.Loading

    if (!isActive) {
        isExpanded = false
        showCancelConfirm = false
        return
    }

    // Only a live server-side run can be aborted. A local LaTeX compile has
    // nothing on the backend to cancel, so no button is offered for it.
    val canCancel = onCancel != null ||
            runState is GenerationRunState.Loading ||
            runState is GenerationRunState.Active

    val currentMessage = when {
        schemaState is SchemaState.Loading -> "Brainstorming Magazine Structure..."
        latexState is LatexState.Loading -> "Drafting Articles & Injecting Images..."
        compileState is CompileState.Loading -> (compileState as CompileState.Loading).message
        else -> "Working..."
    }

    val currentProgress = when (compileState) {
        is CompileState.Loading -> (compileState as CompileState.Loading).progress / 100f
        else -> -1f // Indeterminate
    }

    // A run can take 12 minutes, so confirm before throwing that work away.
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel generation?") },
            text = { Text("The issue being generated will be discarded. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        isExpanded = false
                        if (onCancel != null) onCancel() else editorViewModel.cancelGenerationRun()
                    }
                ) {
                    Text("Cancel run", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("Keep going", color = tokens.primaryAccent)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Card(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .widthIn(max = 250.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val statusText = when {
                            schemaState is SchemaState.Loading -> "Curating Content..."
                            latexState is LatexState.Loading -> "Writing LaTeX Code..."
                            currentProgress < 0f -> "Initializing Engine..."
                            currentProgress < 0.3f -> "Typesetting Layout..."
                            currentProgress < 0.6f -> "Downloading Assets..."
                            currentProgress < 0.9f -> "Rendering PDF..."
                            else -> "Final Polish..."
                        }
                        Text(
                            text = statusText,
                            style = LuxeTypography.titleSmall.copy(
                                color = tokens.primaryAccent,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = currentMessage,
                            style = LuxeTypography.bodyMedium.copy(color = Color.White),
                            lineHeight = 20.sp
                        )
                        if (currentProgress >= 0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = currentProgress,
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = tokens.primaryAccent,
                                trackColor = Color.DarkGray
                            )
                            Text(
                                text = "${(currentProgress * 100).toInt()}%",
                                style = LuxeTypography.labelSmall.copy(color = tokens.primaryAccent),
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                            )
                        }
                        if (canCancel) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = { showCancelConfirm = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Cancel generation",
                                    style = LuxeTypography.labelSmall.copy(color = Color(0xFFE57373))
                                )
                            }
                        }
                    }
                }
            }

            // Floating Action Button
            Surface(
                shape = CircleShape,
                color = tokens.primaryAccent,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { isExpanded = !isExpanded }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (currentProgress >= 0f) {
                        CircularProgressIndicator(
                            progress = currentProgress,
                            modifier = Modifier.size(48.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            strokeWidth = 3.dp
                        )
                    }
                    // Inner icon or dot
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Black))
                }
            }
        }
    }
}
