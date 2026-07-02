package com.magazineforge.app.models

data class PageRequest(
    val type: String,
    val imageUrl: String?,
    val topic: String?
)

data class JobRequest(
    val topic: String,
    val templateVariant: String,
    val pages: List<PageRequest>
)

data class JobResponse(
    val job_id: String
)

data class JobStatusResponse(
    val status: String,
    val progress: Int,
    val error: String?
)
