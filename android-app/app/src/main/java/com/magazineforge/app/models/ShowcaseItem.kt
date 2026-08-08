package com.magazineforge.app.models

/**
 * A Firestore document in "public_showcase".
 *
 * [coverImageUrl] and [pdfUrl] hold paths relative to ApiClient.BASE_URL
 * ("job/<id>/download"), never absolute URLs — a stored hostname would pin the
 * record to one backend forever. Records published before that rule existed do
 * hold absolute URLs; pass both fields through
 * com.magazineforge.app.network.resolveApiUrl, which handles either form.
 *
 * Every field keeps a default so Firestore's reflective deserializer can build
 * an instance through the synthesized no-arg constructor. Field names are the
 * wire contract: renaming one silently orphans the data in existing documents.
 */
data class ShowcaseItem(
    val id: String = "",
    val title: String = "",
    val coverImageUrl: String = "",
    val pdfUrl: String = "",
    val templateVariant: String = "",
    val latexCode: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
