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
import com.magazineforge.app.ui.theme.EditorialGold
import com.magazineforge.app.ui.theme.LuxeTypography

@Composable
fun FloatingProgressTracker(
    schemaState: SchemaState,
    latexState: LatexState,
    compileState: CompileState
) {
    var isExpanded by remember { mutableStateOf(false) }

    val isActive = schemaState is SchemaState.Loading ||
            latexState is LatexState.Loading ||
            compileState is CompileState.Loading

    if (!isActive) {
        isExpanded = false
        return
    }

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
                        Text(
                            text = "AI Editorial Team",
                            style = LuxeTypography.titleSmall.copy(
                                color = EditorialGold,
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
                                color = EditorialGold,
                                trackColor = Color.DarkGray
                            )
                            Text(
                                text = "${(currentProgress * 100).toInt()}%",
                                style = LuxeTypography.labelSmall.copy(color = EditorialGold),
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Floating Action Button
            Surface(
                shape = CircleShape,
                color = EditorialGold,
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
