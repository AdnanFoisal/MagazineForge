package com.magazineforge.app.models

import com.google.gson.annotations.SerializedName

data class GenerationRunRequest(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("templateVariant") val templateVariant: String,
    @SerializedName("articles") val articles: List<BriefArticle>,
    @SerializedName("coverTitle") val coverTitle: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("tone") val tone: String = "Professional",
    @SerializedName("layoutDensity") val layoutDensity: String = "Balanced",
    @SerializedName("enableMasthead") val enableMasthead: Boolean = true,
    @SerializedName("mastheadAngle") val mastheadAngle: String = "",
    @SerializedName("enableSidebar") val enableSidebar: Boolean = true,
    @SerializedName("sidebarTopic") val sidebarTopic: String = "",
    @SerializedName("enablePullQuote") val enablePullQuote: Boolean = true,
    @SerializedName("enableBackCover") val enableBackCover: Boolean = true,
    @SerializedName("enableTocTeasers") val enableTocTeasers: Boolean = true,
    @SerializedName("enableByline") val enableByline: Boolean = true,
    @SerializedName("coverImageUrl") val coverImageUrl: String = "",
    @SerializedName("articleImageUrls") val articleImageUrls: List<String> = emptyList()
)

data class GenerationRunCreateResponse(
    @SerializedName("runId") val runId: String,
    @SerializedName("status") val status: String
)

data class GenerationRunSection(
    @SerializedName("sectionId") val sectionId: String,
    @SerializedName("order") val order: Int,
    @SerializedName("topic") val topic: String,
    @SerializedName("status") val status: String,
    @SerializedName("attempts") val attempts: Int,
    @SerializedName("error") val error: String?,
    @SerializedName("page") val page: PageSchema?
)

data class GenerationRunStatus(
    @SerializedName("runId") val runId: String,
    @SerializedName("status") val status: String,
    @SerializedName("error") val error: String?,
    @SerializedName("totalSections") val totalSections: Int,
    @SerializedName("completedSections") val completedSections: Int,
    @SerializedName("schema") val schema: MagazineSchema,
    @SerializedName("sections") val sections: List<GenerationRunSection>
)
