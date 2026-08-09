package com.magazineforge.app.models

import com.google.gson.annotations.SerializedName

data class MagazineSchema(
    @SerializedName("cover")
    var cover: CoverSchema,
    @SerializedName("masthead")
    var masthead: MastheadSchema?,
    @SerializedName("toc")
    var toc: List<TocItemSchema>,
    @SerializedName("pages")
    var pages: List<PageSchema>,
    @SerializedName("back_cover")
    var backCover: BackCoverSchema?,
    // Returned by generation runs and sent back after Co-Author editing. Without
    // this field Gson drops the user's paper-tone choice before /generate-latex.
    @SerializedName("paper_tone")
    var paperTone: String = "cream"
)

sealed class PageSchema {
    abstract val type: String
}

data class ArticleSchema(
    @SerializedName("type") override val type: String = "article",
    @SerializedName("headline") var headline: String,
    @SerializedName("subheadline") var subheadline: String?,
    @SerializedName("byline") var byline: String,
    @SerializedName("body_copy") var bodyCopy: String,
    @SerializedName("first_letter") var firstLetter: String = "T",
    @SerializedName("first_word_rest") var firstWordRest: String = "he",
    @SerializedName("pull_quotes") var pullQuotes: List<PullQuoteSchema>,
    @SerializedName("images") var images: List<ArticleImageSchema>,
    @SerializedName("sidebar") var sidebar: SidebarSchema?,
    @SerializedName("layout") var layout: String
) : PageSchema()

data class AdSchema(
    @SerializedName("type") override val type: String = "ad",
    @SerializedName("headline") var headline: String,
    @SerializedName("subtext") var subtext: String,
    @SerializedName("fake_company_name") var fakeCompanyName: String,
    @SerializedName("image_url") var imageUrl: String
) : PageSchema()

data class DataPointSchema(
    @SerializedName("label") var label: String,
    @SerializedName("value") var value: Float
)

data class ChartSchema(
    @SerializedName("type") override val type: String = "chart",
    @SerializedName("headline") var headline: String,
    @SerializedName("chart_type") var chartType: String,
    @SerializedName("x_label") var xLabel: String,
    @SerializedName("y_label") var yLabel: String,
    @SerializedName("data_points") var dataPoints: List<DataPointSchema>,
    @SerializedName("description") var description: String = ""
) : PageSchema()

data class PhotoEssaySchema(
    @SerializedName("type") override val type: String = "photo_essay",
    @SerializedName("headline") var headline: String,
    @SerializedName("images") var images: List<ArticleImageSchema>,
    @SerializedName("closing_text") var closingText: String
) : PageSchema()

data class QnaItemSchema(
    @SerializedName("question") var question: String,
    @SerializedName("answer") var answer: String
)

data class QnASchema(
    @SerializedName("type") override val type: String = "qna",
    @SerializedName("headline") var headline: String,
    @SerializedName("interviewer") var interviewer: String,
    @SerializedName("interviewee") var interviewee: String,
    @SerializedName("qna_items") var qnaItems: List<QnaItemSchema>
) : PageSchema()

data class CoverSchema(
    @SerializedName("main_title")
    var mainTitle: String,
    @SerializedName("subtitle")
    var subtitle: String,
    @SerializedName("accent_hex")
    var accentHex: String,
    @SerializedName("title_font")
    var titleFont: String? = "serif",
    @SerializedName("color_theme")
    var colorTheme: String? = "white",
    @SerializedName("cover_pattern")
    var coverPattern: String,
    @SerializedName("callouts")
    var callouts: List<String>,
    @SerializedName("image_url")
    var imageUrl: String
)

data class MastheadSchema(
    @SerializedName("issue_tagline")
    var issueTagline: String,
    @SerializedName("editors_note")
    var editorsNote: String,
    @SerializedName("credits")
    var credits: List<String>
)

data class TocItemSchema(
    @SerializedName("section_title")
    var sectionTitle: String,
    @SerializedName("page_number")
    var pageNumber: Int,
    @SerializedName("teaser")
    var teaser: String
)

data class PullQuoteSchema(
    @SerializedName("quote_text")
    var quoteText: String,
    @SerializedName("attributed_to")
    var attributedTo: String?
)

data class ArticleImageSchema(
    @SerializedName("image_url")
    var imageUrl: String,
    @SerializedName("caption")
    var caption: String,
    @SerializedName("placement")
    var placement: String
)

data class SidebarSchema(
    @SerializedName("box_title")
    var boxTitle: String,
    @SerializedName("bullet_items")
    var bulletItems: List<String>
)

data class BackCoverSchema(
    @SerializedName("style")
    var style: String,
    @SerializedName("tagline")
    var tagline: String,
    @SerializedName("image_url")
    var imageUrl: String?
)

data class BriefArticle(
    @SerializedName("topic")
    val topic: String
)

// Intent Gate. The backend declares these with Pydantic snake_case field names,
// so unlike the camelCase request DTOs below they serialize as must_cover /
// visual_register / extraction_ok.
data class ContractSchema(
    @SerializedName("subject")
    val subject: String = "",
    @SerializedName("audience")
    val audience: String = "general interest",
    @SerializedName("must_cover")
    val mustCover: List<String> = emptyList(),
    @SerializedName("avoid")
    val avoid: List<String> = emptyList(),
    @SerializedName("language")
    val language: String = "en",
    @SerializedName("visual_register")
    val visualRegister: String = "editorial",
    // The model's plain-language reading of the prompt, shown on the Intent
    // screen so the user can see — and correct — what a ten-minute run is
    // about to be built from. Edited text is sent back and used verbatim.
    @SerializedName("expanded_prompt")
    val expandedPrompt: String = "",
    // The photographable things this issue is about. These anchor image
    // search, so editing them changes which photos appear.
    @SerializedName("image_subjects")
    val imageSubjects: List<String> = emptyList()
)

data class ExtractContractRequest(
    @SerializedName("prompt")
    val prompt: String
)

data class ExtractContractResponse(
    @SerializedName("contract")
    val contract: ContractSchema? = null,
    @SerializedName("extraction_ok")
    val extractionOk: Boolean = false
)

data class GenerateBriefResponse(
    @SerializedName("category")
    val category: String,
    @SerializedName("titles")
    val titles: List<String>,
    @SerializedName("tone")
    val tone: String,
    @SerializedName("style_dna")
    val styleDna: String,
    @SerializedName("article_count")
    val articleCount: Int,
    @SerializedName("articles")
    val articles: List<BriefArticle>,
    // Issue bible. The backend declares these Optional, and the MOCK_COMPILE
    // brief response omits them entirely, so they stay nullable — Gson leaves
    // absent fields null regardless of a Kotlin default.
    @SerializedName("voice_guide")
    val voiceGuide: String? = "",
    @SerializedName("forbidden_phrases")
    val forbiddenPhrases: List<String>? = emptyList(),
    @SerializedName("author_cast")
    val authorCast: List<String>? = emptyList(),
    @SerializedName("article_angles")
    val articleAngles: List<String>? = emptyList(),
    @SerializedName("contract")
    val contract: ContractSchema? = null
)

data class GenerateBriefRequest(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("referenceImages")
    val referenceImages: List<String> = emptyList(),
    @SerializedName("articleCount")
    val articleCount: Int? = null,
    @SerializedName("contract")
    val contract: ContractSchema? = null
)

data class GenerateSchemaRequest(
    @SerializedName("topic")
    val topic: String,
    @SerializedName("templateVariant")
    val templateVariant: String,
    @SerializedName("tone")
    val tone: String = "Professional",
    @SerializedName("layoutDensity")
    val layoutDensity: String = "Balanced",
    @SerializedName("enableMasthead")
    val enableMasthead: Boolean = true,
    @SerializedName("mastheadAngle")
    val mastheadAngle: String = "",
    @SerializedName("enableSidebar")
    val enableSidebar: Boolean = true,
    @SerializedName("sidebarTopic")
    val sidebarTopic: String = "",
    @SerializedName("enablePullQuote")
    val enablePullQuote: Boolean = true,
    @SerializedName("enableBackCover")
    val enableBackCover: Boolean = true,
    @SerializedName("enableTocTeasers")
    val enableTocTeasers: Boolean = true,
    @SerializedName("enableByline")
    val enableByline: Boolean = true,
    @SerializedName("coverImageUrl")
    val coverImageUrl: String = "",
    @SerializedName("paperTone")
    val paperTone: String = "cream",
    @SerializedName("contract")
    val contract: ContractSchema? = null
)

data class GenerateLatexRequest(
    @SerializedName("schema")
    val schema: MagazineSchema,
    @SerializedName("templateVariant")
    val templateVariant: String
)

data class GenerateLatexResponse(
    @SerializedName("latexCode")
    val latexCode: String
)


data class CompileRawRequest(
    @SerializedName("latexCode")
    val latexCode: String
)

data class CompileRawResponse(
    @SerializedName("jobId")
    val jobId: String
)

data class RewriteSelectionRequest(
    @SerializedName("text") val text: String,
    @SerializedName("instruction") val instruction: String
)

data class RewriteSelectionResponse(
    @SerializedName("rewrittenText") val rewrittenText: String
)

data class RenderPageRequest(
    @SerializedName("latexCode") val latexCode: String,
    @SerializedName("pageNumber") val pageNumber: Int
)
