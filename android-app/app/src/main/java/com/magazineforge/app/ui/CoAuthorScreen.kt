package com.magazineforge.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import com.magazineforge.app.models.MagazineSchema
import com.magazineforge.app.models.ArticleSchema
import com.magazineforge.app.models.TocItemSchema
import com.magazineforge.app.ui.theme.DarkSurface
import com.magazineforge.app.ui.theme.GhostWhite
import com.magazineforge.app.ui.theme.GoldBright
import com.magazineforge.app.ui.theme.PitchBlack
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoAuthorScreen(
    initialSchema: MagazineSchema,
    isGeneratingLatex: Boolean,
    onGenerateLatex: (MagazineSchema) -> Unit,
    onBack: () -> Unit
) {
    var schema by remember { mutableStateOf(initialSchema) }
    val coroutineScope = rememberCoroutineScope()
    
    // We will pass down this helper to any field that wants an image
    // It launches an image picker, uploads to Firebase, and calls onUrlReceived
    var onImageUploadedCallback: ((String) -> Unit)? by remember { mutableStateOf(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val storageRef = FirebaseStorage.getInstance().reference
                    val imageRef = storageRef.child("images/${System.currentTimeMillis()}_${uri.lastPathSegment}")
                    val uploadTask = imageRef.putFile(uri).await()
                    val downloadUrl = imageRef.downloadUrl.await().toString()
                    onImageUploadedCallback?.invoke(downloadUrl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val pickImage = { callback: (String) -> Unit ->
        onImageUploadedCallback = callback
        launcher.launch("image/*")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Co-Author Schema", color = GoldBright) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = GhostWhite)
                    }
                },
                actions = {
                    Button(
                        onClick = { onGenerateLatex(schema) },
                        enabled = !isGeneratingLatex,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldBright)
                    ) {
                        Text(if (isGeneratingLatex) "Generating..." else "Next", color = PitchBlack)
                    }
                }
            )
        },
        containerColor = PitchBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Expandable sections for Cover, Masthead, TOC, Articles, BackCover
            ExpandableSection("Cover") {
                OutlinedTextField(
                    value = schema.cover.mainTitle,
                    onValueChange = { schema = schema.copy(cover = schema.cover.copy(mainTitle = it)) },
                    label = { Text("Main Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schema.cover.subtitle,
                    onValueChange = { schema = schema.copy(cover = schema.cover.copy(subtitle = it)) },
                    label = { Text("Subtitle") },
                    modifier = Modifier.fillMaxWidth()
                )
                ImageUploadField(
                    label = "Cover Image URL",
                    value = schema.cover.imageUrl,
                    onValueChange = { schema = schema.copy(cover = schema.cover.copy(imageUrl = it)) },
                    onPickImage = { pickImage { url -> schema = schema.copy(cover = schema.cover.copy(imageUrl = url)) } }
                )
            }
            
            ExpandableSection("Masthead") {
                OutlinedTextField(
                    value = schema.masthead.issueTagline,
                    onValueChange = { schema = schema.copy(masthead = schema.masthead.copy(issueTagline = it)) },
                    label = { Text("Issue Tagline") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schema.masthead.editorsNote,
                    onValueChange = { schema = schema.copy(masthead = schema.masthead.copy(editorsNote = it)) },
                    label = { Text("Editor's Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ExpandableSection("Table of Contents") {
                schema.toc.forEachIndexed { index, item ->
                    Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            OutlinedTextField(
                                value = item.sectionTitle,
                                onValueChange = { 
                                    val newToc = schema.toc.toMutableList()
                                    newToc[index] = item.copy(sectionTitle = it)
                                    schema = schema.copy(toc = newToc) 
                                },
                                label = { Text("Section Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = item.teaser,
                                onValueChange = { 
                                    val newToc = schema.toc.toMutableList()
                                    newToc[index] = item.copy(teaser = it)
                                    schema = schema.copy(toc = newToc) 
                                },
                                label = { Text("Teaser") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            schema.articles.forEachIndexed { index, article ->
                ExpandableSection("Article: ${article.headline}") {
                    OutlinedTextField(
                        value = article.headline,
                        onValueChange = {
                            val newArticles = schema.articles.toMutableList()
                            newArticles[index] = article.copy(headline = it)
                            schema = schema.copy(articles = newArticles)
                        },
                        label = { Text("Headline") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = article.bodyCopy,
                        onValueChange = {
                            val newArticles = schema.articles.toMutableList()
                            newArticles[index] = article.copy(bodyCopy = it)
                            schema = schema.copy(articles = newArticles)
                        },
                        label = { Text("Body Copy") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )
                    article.images.forEachIndexed { imgIndex, img ->
                        ImageUploadField(
                            label = "Article Image ${imgIndex + 1} URL",
                            value = img.imageUrl,
                            onValueChange = { newUrl ->
                                val newImages = article.images.toMutableList()
                                newImages[imgIndex] = img.copy(imageUrl = newUrl)
                                val newArticles = schema.articles.toMutableList()
                                newArticles[index] = article.copy(images = newImages)
                                schema = schema.copy(articles = newArticles)
                            },
                            onPickImage = {
                                pickImage { url ->
                                    val newImages = article.images.toMutableList()
                                    newImages[imgIndex] = img.copy(imageUrl = url)
                                    val newArticles = schema.articles.toMutableList()
                                    newArticles[index] = article.copy(images = newImages)
                                    schema = schema.copy(articles = newArticles)
                                }
                            }
                        )
                    }
                }
            }

            ExpandableSection("Back Cover") {
                OutlinedTextField(
                    value = schema.backCover.tagline,
                    onValueChange = { schema = schema.copy(backCover = schema.backCover.copy(tagline = it)) },
                    label = { Text("Tagline") },
                    modifier = Modifier.fillMaxWidth()
                )
                ImageUploadField(
                    label = "Back Cover Image URL",
                    value = schema.backCover.imageUrl ?: "",
                    onValueChange = { schema = schema.copy(backCover = schema.backCover.copy(imageUrl = it)) },
                    onPickImage = { pickImage { url -> schema = schema.copy(backCover = schema.backCover.copy(imageUrl = url)) } }
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, color = GhostWhite)
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = GhostWhite
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ImageUploadField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPickImage: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onPickImage) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Pick Image",
                tint = GoldBright
            )
        }
    }
}
