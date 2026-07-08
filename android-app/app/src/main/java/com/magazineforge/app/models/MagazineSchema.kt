package com.magazineforge.app.models

import com.google.gson.annotations.SerializedName

data class MagazineSchema(
    @SerializedName("cover")
    var cover: CoverSchema,
    @SerializedName("masthead")
    var masthead: MastheadSchema,
    @SerializedName("toc")
    var toc: List<TocItemSchema>,
    @SerializedName("articles")
    var articles: List<ArticleSchema>,
    @SerializedName("back_cover")
    var backCover: BackCoverSchema
)

data class CoverSchema(
    @SerializedName("main_title")
    var mainTitle: String,
    @SerializedName("subtitle")
    var subtitle: String,
    @SerializedName("accent_hex")
    var accentHex: String,
    @SerializedName("cover_pattern")
    var coverPattern: String, // clean_title_dominant | callout_heavy | typographic_led
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

data class ArticleSchema(
    @SerializedName("headline")
    var headline: String,
    @SerializedName("subheadline")
    var subheadline: String?,
    @SerializedName("byline")
    var byline: String,
    @SerializedName("body_copy")
    var bodyCopy: String,
    @SerializedName("first_letter")
    var firstLetter: String = "T",
    @SerializedName("first_word_rest")
    var firstWordRest: String = "he",
    @SerializedName("pull_quotes")
    var pullQuotes: List<PullQuoteSchema>,
    @SerializedName("images")
    var images: List<ArticleImageSchema>,
    @SerializedName("sidebar")
    var sidebar: SidebarSchema?,
    @SerializedName("layout")
    var layout: String // two_column | three_column | photo_essay
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
    var placement: String // full_bleed | half_page | inset
)

data class SidebarSchema(
    @SerializedName("box_title")
    var boxTitle: String,
    @SerializedName("bullet_items")
    var bulletItems: List<String>
)

data class BackCoverSchema(
    @SerializedName("style")
    var style: String, // closing_image | next_issue_teaser
    @SerializedName("tagline")
    var tagline: String,
    @SerializedName("image_url")
    var imageUrl: String?
)

data class GenerateSchemaRequest(
    @SerializedName("topic")
    val topic: String,
    @SerializedName("templateVariant")
    val templateVariant: String
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
