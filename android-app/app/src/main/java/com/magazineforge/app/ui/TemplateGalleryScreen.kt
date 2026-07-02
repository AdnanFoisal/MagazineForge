package com.magazineforge.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect

data class TemplateModel(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val thumbnailUrl: String,
    val texTemplate: String
)

fun loadTemplates(context: Context): List<TemplateModel> {
    val jsonString = context.assets.open("template_config.json").bufferedReader().use { it.readText() }
    val jsonArray = JSONArray(jsonString)
    val templates = mutableListOf<TemplateModel>()
    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        templates.add(
            TemplateModel(
                id = obj.getString("id"),
                name = obj.getString("name"),
                category = obj.getString("category"),
                description = obj.getString("description"),
                thumbnailUrl = obj.getString("thumbnailUrl"),
                texTemplate = obj.getString("texTemplate")
            )
        )
    }
    return templates
}

@Composable
fun TemplateGalleryScreen(
    onTemplateSelected: (String) -> Unit,
    onLibraryClicked: () -> Unit
) {
    val context = LocalContext.current
    val templates = remember { loadTemplates(context) }
    val categories = templates.map { it.category }.distinct()
    
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTemplates = templates.filter { 
        (categories.isEmpty() || it.category == categories[selectedCategoryIndex]) && 
        (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
    }

    Surface(
        color = Color(0xFF0F0F10),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = Color(0xFF18181B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Template Gallery",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = Color(0xFFF5F5F7)
                        )
                        IconButton(onClick = onLibraryClicked) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Library",
                                tint = Color(0xFFC5A059)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search templates...", color = Color(0xFFA1A1AA)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFC5A059)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC5A059),
                            unfocusedBorderColor = Color(0xFF2E2A24),
                            focusedTextColor = Color(0xFFF5F5F7),
                            unfocusedTextColor = Color(0xFFF5F5F7),
                            focusedContainerColor = Color(0xFF18181B),
                            unfocusedContainerColor = Color(0xFF18181B)
                        )
                    )
                }
            }
            
            if (categories.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    edgePadding = 8.dp,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFC5A059),
                    indicator = { tabPositions ->
                        if (selectedCategoryIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                                color = Color(0xFFC5A059)
                            )
                        }
                    }
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Text(
                                    text = category,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategoryIndex == index) Color(0xFFC5A059) else Color(0xFFA1A1AA)
                                )
                            }
                        )
                    }
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTemplates) { template ->
                    TemplateCard(template = template, onClick = { onTemplateSelected(template.texTemplate) })
                }
            }
        }
    }
}

@Composable
fun TemplateCard(template: TemplateModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2A24)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TemplatePreview(template = template, modifier = Modifier.fillMaxSize())
                if (template.thumbnailUrl.isNotEmpty()) {
                    coil.compose.AsyncImage(
                        model = template.thumbnailUrl,
                        contentDescription = "Preview Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color(0xFFF5F5F7),
                maxLines = 1
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFFA1A1AA),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { /* TODO: Open sample PDF */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC5A059)),
                border = BorderStroke(1.dp, Color(0xFFC5A059))
            ) {
                Text("Preview Sample PDF", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun TemplatePreview(template: TemplateModel, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, Color(0xFFC5A059)),
        color = Color(0xFF0F0F10)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            when (template.id) {
                "food_classic" -> {
                    val gradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFCF8F2), Color(0xFFEFE8DD))
                    )
                    drawRect(brush = gradient)
                    val goldColor = Color(0xFFD4AF37)
                    drawCircle(
                        color = goldColor,
                        radius = width * 0.3f,
                        center = Offset(width / 2, height / 2),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = goldColor.copy(alpha = 0.5f),
                        radius = width * 0.2f,
                        center = Offset(width / 2, height / 2),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    val knifePath = Path().apply {
                        moveTo(width * 0.48f, height * 0.7f)
                        lineTo(width * 0.48f, height * 0.3f)
                    }
                    val forkPath = Path().apply {
                        moveTo(width * 0.52f, height * 0.7f)
                        lineTo(width * 0.52f, height * 0.3f)
                    }
                    drawPath(knifePath, color = goldColor, style = Stroke(width = 1.dp.toPx()))
                    drawPath(forkPath, color = goldColor, style = Stroke(width = 1.dp.toPx()))
                }
                "food_modern" -> {
                    val gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF5722), Color(0xFFFFC107)),
                        start = Offset(0f, 0f),
                        end = Offset(width, height)
                    )
                    drawRect(brush = gradient)
                    val bandPath = Path().apply {
                        moveTo(0f, height * 0.4f)
                        lineTo(width, height * 0.2f)
                        lineTo(width, height * 0.6f)
                        lineTo(0f, height * 0.8f)
                        close()
                    }
                    drawPath(bandPath, color = Color(0xFF1E1E24))
                    drawCircle(
                        color = Color(0xFFEAEAEA),
                        radius = width * 0.18f,
                        center = Offset(width * 0.5f, height * 0.5f)
                    )
                }
                "food_rustic" -> {
                    val gradient = Brush.radialGradient(
                        colors = listOf(Color(0xFF8D6E63), Color(0xFF4E342E)),
                        center = Offset(width / 2, height / 2)
                    )
                    drawRect(brush = gradient)
                    val leafPath = Path().apply {
                        moveTo(width * 0.5f, height * 0.8f)
                        cubicTo(width * 0.1f, height * 0.5f, width * 0.3f, height * 0.2f, width * 0.5f, height * 0.15f)
                        cubicTo(width * 0.7f, height * 0.2f, width * 0.9f, height * 0.5f, width * 0.5f, height * 0.8f)
                    }
                    drawPath(leafPath, color = Color(0xFFA5D6A7), style = Stroke(width = 2.dp.toPx()))
                    drawLine(
                        color = Color(0xFFA5D6A7),
                        start = Offset(width * 0.5f, height * 0.8f),
                        end = Offset(width * 0.5f, height * 0.15f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                "travel_landscape" -> {
                    val skyGradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00B0FF), Color(0xFF80DEEA), Color(0xFFFFB74D))
                    )
                    drawRect(brush = skyGradient)
                    val mountainPath1 = Path().apply {
                        moveTo(0f, height)
                        lineTo(width * 0.4f, height * 0.55f)
                        lineTo(width * 0.8f, height)
                        close()
                    }
                    val mountainPath2 = Path().apply {
                        moveTo(width * 0.3f, height)
                        lineTo(width * 0.75f, height * 0.45f)
                        lineTo(width, height)
                        close()
                    }
                    drawPath(mountainPath1, color = Color(0xFF263238).copy(alpha = 0.8f))
                    drawPath(mountainPath2, color = Color(0xFF37474F).copy(alpha = 0.9f))
                    drawCircle(
                        color = Color(0xFFFFF176),
                        radius = width * 0.1f,
                        center = Offset(width * 0.25f, height * 0.35f)
                    )
                }
                "travel_journal" -> {
                    drawRect(color = Color(0xFFF9F5EB))
                    val frame1 = Path().apply {
                        addRect(androidx.compose.ui.geometry.Rect(width * 0.15f, height * 0.15f, width * 0.65f, height * 0.6f))
                    }
                    val frame2 = Path().apply {
                        addRect(androidx.compose.ui.geometry.Rect(width * 0.35f, height * 0.35f, width * 0.85f, height * 0.8f))
                    }
                    drawPath(frame1, color = Color(0xFF8D8D8D), style = Stroke(width = 1.dp.toPx()))
                    drawPath(frame2, color = Color(0xFF636363), style = Stroke(width = 1.dp.toPx()))
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val routePath = Path().apply {
                        moveTo(width * 0.2f, height * 0.8f)
                        cubicTo(width * 0.3f, height * 0.5f, width * 0.7f, height * 0.6f, width * 0.8f, height * 0.2f)
                    }
                    drawPath(routePath, color = Color(0xFFC5A059), style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect))
                }
                "travel_guide" -> {
                    drawRect(color = Color(0xFF0F2027))
                    val gridColor = Color(0xFF203A43)
                    for (i in 1..4) {
                        val x = width * (i / 5f)
                        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1.dp.toPx())
                    }
                    for (j in 1..6) {
                        val y = height * (j / 7f)
                        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1.dp.toPx())
                    }
                    val pinPath = Path().apply {
                        val cx = width * 0.5f
                        val cy = height * 0.45f
                        moveTo(cx, cy + width * 0.2f)
                        cubicTo(cx - width * 0.15f, cy, cx - width * 0.15f, cy - width * 0.15f, cx, cy - width * 0.15f)
                        cubicTo(cx + width * 0.15f, cy - width * 0.15f, cx + width * 0.15f, cy, cx, cy + width * 0.2f)
                    }
                    drawPath(pinPath, color = Color(0xFFC5A059))
                    drawCircle(color = Color(0xFF0F2027), radius = width * 0.05f, center = Offset(width * 0.5f, height * 0.45f))
                }
                "tech_cyber" -> {
                    drawRect(color = Color(0xFF0D0D11))
                    val gridColor = Color(0xFF00FFCC).copy(alpha = 0.1f)
                    val step = width / 6
                    for (i in 0..6) {
                        drawLine(color = gridColor, start = Offset(i * step, 0f), end = Offset(i * step, height))
                        drawLine(color = gridColor, start = Offset(0f, i * step), end = Offset(width, i * step))
                    }
                    val circuitColor = Color(0xFF00FFCC)
                    val p = Path().apply {
                        moveTo(width * 0.1f, height * 0.2f)
                        lineTo(width * 0.4f, height * 0.2f)
                        lineTo(width * 0.6f, height * 0.5f)
                        lineTo(width * 0.9f, height * 0.5f)
                    }
                    drawPath(p, color = circuitColor, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(color = circuitColor, radius = 4.dp.toPx(), center = Offset(width * 0.1f, height * 0.2f))
                    drawCircle(color = circuitColor, radius = 4.dp.toPx(), center = Offset(width * 0.9f, height * 0.5f))
                }
                "tech_minimal" -> {
                    drawRect(color = Color(0xFF1E1E1E))
                    val cubeColor = Color(0xFFFFFFFF).copy(alpha = 0.7f)
                    val cx = width * 0.5f
                    val cy = height * 0.5f
                    val cubeSize = width * 0.25f
                    val v0 = Offset(cx, cy - cubeSize)
                    val v1 = Offset(cx - cubeSize * 0.86f, cy - cubeSize * 0.5f)
                    val v2 = Offset(cx + cubeSize * 0.86f, cy - cubeSize * 0.5f)
                    val v3 = Offset(cx, cy + cubeSize)
                    val v4 = Offset(cx - cubeSize * 0.86f, cy + cubeSize * 0.5f)
                    val v5 = Offset(cx + cubeSize * 0.86f, cy + cubeSize * 0.5f)
                    val v6 = Offset(cx, cy)

                    drawLine(cubeColor, v0, v1, 1.5.dp.toPx())
                    drawLine(cubeColor, v0, v2, 1.5.dp.toPx())
                    drawLine(cubeColor, v1, v4, 1.5.dp.toPx())
                    drawLine(cubeColor, v2, v5, 1.5.dp.toPx())
                    drawLine(cubeColor, v3, v4, 1.5.dp.toPx())
                    drawLine(cubeColor, v3, v5, 1.5.dp.toPx())
                    drawLine(cubeColor, v6, v0, 1.dp.toPx())
                    drawLine(cubeColor, v6, v4, 1.dp.toPx())
                    drawLine(cubeColor, v6, v5, 1.dp.toPx())
                }
                "tech_spec" -> {
                    drawRect(color = Color(0xFF121824))
                    val accentColor = Color(0xFFFFB300)
                    for (i in 0..4) {
                        val y = height * (0.25f + i * 0.12f)
                        val barWidth = width * (0.3f + (i % 3) * 0.2f)
                        drawRect(
                            color = Color(0xFF1F2B3E),
                            topLeft = Offset(width * 0.1f, y),
                            size = Size(width * 0.8f, 6.dp.toPx())
                        )
                        drawRect(
                            color = accentColor.copy(alpha = 0.8f),
                            topLeft = Offset(width * 0.1f, y),
                            size = Size(barWidth, 6.dp.toPx())
                        )
                    }
                    drawCircle(color = accentColor, radius = 3.dp.toPx(), center = Offset(width * 0.9f, height * 0.15f))
                    drawLine(color = Color(0xFF1F2B3E), start = Offset(0f, height * 0.8f), end = Offset(width, height * 0.8f), strokeWidth = 2.dp.toPx())
                }
                "lifestyle_vogue" -> {
                    drawRect(color = Color(0xFF0A0A0A))
                    val goldColor = Color(0xFFD4AF37)
                    drawRect(
                        color = goldColor,
                        topLeft = Offset(width * 0.1f, height * 0.1f),
                        size = Size(width * 0.8f, height * 0.8f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    val diamondPath = Path().apply {
                        moveTo(width * 0.5f, height * 0.15f)
                        lineTo(width * 0.85f, height * 0.5f)
                        lineTo(width * 0.5f, height * 0.85f)
                        lineTo(width * 0.15f, height * 0.5f)
                        close()
                    }
                    drawPath(diamondPath, color = goldColor.copy(alpha = 0.3f), style = Stroke(width = 1.dp.toPx()))
                    drawCircle(
                        color = goldColor,
                        radius = width * 0.12f,
                        center = Offset(width / 2, height / 2),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                "lifestyle_indie" -> {
                    val pastelGrad = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF8A80), Color(0xFFFFD180), Color(0xFF80D8FF))
                    )
                    drawRect(brush = pastelGrad)
                    val wavePath = Path().apply {
                        moveTo(0f, height * 0.4f)
                        cubicTo(width * 0.25f, height * 0.2f, width * 0.5f, height * 0.6f, width * 0.75f, height * 0.3f)
                        cubicTo(width * 0.85f, height * 0.2f, width * 0.95f, height * 0.4f, width, height * 0.35f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(wavePath, color = Color(0xFFFFFFFF).copy(alpha = 0.3f))
                    drawCircle(color = Color(0xFFFFFFFF).copy(alpha = 0.9f), radius = width * 0.08f, center = Offset(width * 0.5f, height * 0.45f))
                }
                "lifestyle_wellness" -> {
                    drawRect(color = Color(0xFFF7F1E5))
                    val rippleColor = Color(0xFFC4B5A5)
                    for (i in 1..4) {
                        drawCircle(
                            color = rippleColor.copy(alpha = 0.8f / i),
                            radius = width * (0.1f + i * 0.12f),
                            center = Offset(width * 0.5f, height * 0.6f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    drawOval(
                        color = Color(0xFFBCAAA4),
                        topLeft = Offset(width * 0.38f, height * 0.5f),
                        size = Size(width * 0.24f, width * 0.15f)
                    )
                    drawOval(
                        color = Color(0xFF8D6E63),
                        topLeft = Offset(width * 0.42f, height * 0.43f),
                        size = Size(width * 0.16f, width * 0.1f)
                    )
                    drawOval(
                        color = Color(0xFFD7CCC8),
                        topLeft = Offset(width * 0.45f, height * 0.39f),
                        size = Size(width * 0.1f, width * 0.07f)
                    )
                }
                "science_cosmos" -> {
                    val cosmicGrad = Brush.verticalGradient(
                        colors = listOf(Color(0xFF030A16), Color(0xFF0F2042))
                    )
                    drawRect(brush = cosmicGrad)
                    val orbitColor = Color(0xFF81D4FA).copy(alpha = 0.3f)
                    drawCircle(
                        color = orbitColor,
                        radius = width * 0.35f,
                        center = Offset(width * 0.5f, height * 0.5f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFE1F5FE),
                        radius = width * 0.15f,
                        center = Offset(width * 0.5f, height * 0.5f)
                    )
                    val ringPath = Path().apply {
                        moveTo(width * 0.2f, height * 0.55f)
                        cubicTo(width * 0.3f, height * 0.65f, width * 0.7f, height * 0.65f, width * 0.8f, height * 0.55f)
                    }
                    drawPath(ringPath, color = Color(0xFFFFD54F), style = Stroke(width = 3.dp.toPx()))
                }
                "science_nature" -> {
                    val forestGrad = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B5E20), Color(0xFF003300))
                    )
                    drawRect(brush = forestGrad)
                    val veinColor = Color(0xFF81C784).copy(alpha = 0.5f)
                    val mainVein = Path().apply {
                        moveTo(width * 0.5f, height)
                        lineTo(width * 0.5f, height * 0.1f)
                    }
                    drawPath(mainVein, color = veinColor, style = Stroke(width = 3.dp.toPx()))
                    for (i in 1..4) {
                        val y = height * (0.2f + i * 0.18f)
                        val veinLeft = Path().apply {
                            moveTo(width * 0.5f, y)
                            quadraticTo(width * 0.3f, y - height * 0.08f, width * 0.15f, y - height * 0.05f)
                        }
                        val veinRight = Path().apply {
                            moveTo(width * 0.5f, y)
                            quadraticTo(width * 0.7f, y - height * 0.08f, width * 0.85f, y - height * 0.05f)
                        }
                        drawPath(veinLeft, color = veinColor, style = Stroke(width = 1.5.dp.toPx()))
                        drawPath(veinRight, color = veinColor, style = Stroke(width = 1.5.dp.toPx()))
                    }
                }
                "science_journal" -> {
                    drawRect(color = Color(0xFFFCFDFD))
                    drawLine(color = Color(0xFFECEFF1), start = Offset(width * 0.5f, 0f), end = Offset(width * 0.5f, height), strokeWidth = 2.dp.toPx())
                    drawCircle(
                        color = Color(0xFF37474F),
                        radius = width * 0.14f,
                        center = Offset(width * 0.25f, height * 0.4f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(
                        color = Color(0xFFD84315),
                        start = Offset(width * 0.25f, height * 0.4f),
                        end = Offset(width * 0.35f, height * 0.32f),
                        strokeWidth = 2.dp.toPx()
                    )
                    for (i in 0..5) {
                        val y = height * (0.6f + i * 0.05f)
                        drawLine(
                            color = Color(0xFFCFD8DC),
                            start = Offset(width * 0.08f, y),
                            end = Offset(width * 0.42f, y),
                            strokeWidth = 3.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFFCFD8DC),
                            start = Offset(width * 0.58f, y),
                            end = Offset(width * 0.92f, y),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
                "custom_corporate" -> {
                    val corpGrad = Brush.verticalGradient(
                        colors = listOf(Color(0xFF263238), Color(0xFF102027))
                    )
                    drawRect(brush = corpGrad)
                    val barColor = Color(0xFF90A4AE)
                    val highlightColor = Color(0xFF00E676)
                    for (i in 0..3) {
                        val barHeight = height * (0.2f + i * 0.15f)
                        val x = width * (0.15f + i * 0.2f)
                        drawRect(
                            color = if (i == 3) highlightColor else barColor,
                            topLeft = Offset(x, height * 0.8f - barHeight),
                            size = Size(width * 0.12f, barHeight)
                        )
                    }
                    val trendPath = Path().apply {
                        moveTo(width * 0.21f, height * 0.55f)
                        lineTo(width * 0.41f, height * 0.4f)
                        lineTo(width * 0.61f, height * 0.25f)
                        lineTo(width * 0.81f, height * 0.1f)
                    }
                    drawPath(trendPath, color = highlightColor, style = Stroke(width = 2.5.dp.toPx()))
                }
                "custom_bold" -> {
                    drawRect(color = Color(0xFFD50000))
                    val stripeColor = Color(0xFF000000)
                    for (i in -2..4) {
                        val path = Path().apply {
                            moveTo(0f, height * (i * 0.25f))
                            lineTo(width, height * (i * 0.25f + 0.3f))
                            lineTo(width, height * (i * 0.25f + 0.45f))
                            lineTo(0f, height * (i * 0.25f + 0.15f))
                            close()
                        }
                        drawPath(path, color = stripeColor.copy(alpha = 0.85f))
                    }
                    val shapePath = Path().apply {
                        moveTo(width * 0.5f, height * 0.35f)
                        lineTo(width * 0.7f, height * 0.5f)
                        lineTo(width * 0.5f, height * 0.65f)
                        lineTo(width * 0.3f, height * 0.5f)
                        close()
                    }
                    drawPath(shapePath, color = Color(0xFFFFFFFF))
                }
                else -> { // custom_blank & default fallback
                    drawRect(color = Color(0xFFE0E0E0))
                    val borderEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    drawRect(
                        color = Color(0xFF9E9E9E),
                        topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                        size = Size(width - 16.dp.toPx(), height - 16.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx(), pathEffect = borderEffect)
                    )
                    drawLine(
                        color = Color(0xFFBDBDBD),
                        start = Offset(8.dp.toPx(), 8.dp.toPx()),
                        end = Offset(width - 16.dp.toPx(), height - 16.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFBDBDBD),
                        start = Offset(width - 16.dp.toPx(), 8.dp.toPx()),
                        end = Offset(8.dp.toPx(), height - 16.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}
