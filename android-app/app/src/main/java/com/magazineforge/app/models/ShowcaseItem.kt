package com.magazineforge.app.models

data class ShowcaseItem(
    val id: String = "",
    val title: String = "",
    val coverImageUrl: String = "",
    val pdfUrl: String = "",
    val templateVariant: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
