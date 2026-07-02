package com.magazineforge.app.models

import kotlinx.serialization.Serializable

@Serializable
data class MagazineSchema(
    var cover: CoverSchema,
    var masthead: MastheadSchema,
    var toc: List<TocItemSchema>,
    var articles: List<ArticleSchema>,
    var backCover: BackCoverSchema
)

@Serializable
data class CoverSchema(
    var mainTitle: String,
    var subtitle: String,
    var accentHex: String,
    var coverPattern: String, // clean_title_dominant | callout_heavy | typographic_led
    var callouts: List<String>,
    var imageUrl: String
)

@Serializable
data class MastheadSchema(
    var issueTagline: String,
    var editorsNote: String,
    var credits: List<String>
)

@Serializable
data class TocItemSchema(
    var sectionTitle: String,
    var pageNumber: Int,
    var teaser: String
)

@Serializable
data class ArticleSchema(
    var headline: String,
    var subheadline: String?,
    var byline: String,
    var bodyCopy: String,
    var firstLetter: String = "T",
    var firstWordRest: String = "he",
    var pullQuotes: List<PullQuoteSchema>,
    var images: List<ArticleImageSchema>,
    var sidebar: SidebarSchema?,
    var layout: String // two_column | three_column | photo_essay
)

@Serializable
data class PullQuoteSchema(
    var quoteText: String,
    var attributedTo: String?
)

@Serializable
data class ArticleImageSchema(
    var imageUrl: String,
    var caption: String,
    var placement: String // full_bleed | half_page | inset
)

@Serializable
data class SidebarSchema(
    var boxTitle: String,
    var bulletItems: List<String>
)

@Serializable
data class BackCoverSchema(
    var style: String, // closing_image | next_issue_teaser
    var tagline: String,
    var imageUrl: String?
)

@Serializable
data class GenerateSchemaRequest(
    val topic: String,
    val templateVariant: String
)

@Serializable
data class GenerateLatexRequest(
    val schema: MagazineSchema,
    val templateVariant: String
)

@Serializable
data class GenerateLatexResponse(
    val latexCode: String
)

@Serializable
data class CompileRawRequest(
    val latexCode: String
)

@Serializable
data class CompileRawResponse(
    val jobId: String
)
