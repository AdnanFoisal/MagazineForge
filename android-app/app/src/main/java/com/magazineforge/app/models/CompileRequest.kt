package com.magazineforge.app.models

data class CompileRequest(
    val title: String,
    val subtitle: String,
    val coverImageUrl: String,
    val templateName: String,
    val fcmToken: String? = null
)
