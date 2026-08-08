package com.magazineforge.app.network

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.magazineforge.app.models.ShowcaseItem
import kotlinx.coroutines.tasks.await

/**
 * Resolves a stored media path against [ApiClient.BASE_URL].
 *
 * Everything the app fetches has to go through BASE_URL so that flipping it to
 * the Worker proxy repoints the whole app at once. Stored values are therefore
 * host-less paths ("job/<id>/download", "static/samples/x.pdf").
 *
 * Backward compatibility: Firestore records published before this change hold a
 * full "https://...hf.space/..." URL. Anything that already carries a scheme is
 * returned verbatim, so those records keep working exactly as they did.
 *
 * BASE_URL ends in "/" and a stored path may or may not start with one, so both
 * sides are trimmed before joining — "host//path" would survive the Worker's
 * slash collapsing but not a direct hit on the Space.
 */
fun resolveApiUrl(pathOrUrl: String): String {
    if (pathOrUrl.isEmpty()) return pathOrUrl
    if (pathOrUrl.startsWith("http://", ignoreCase = true) ||
        pathOrUrl.startsWith("https://", ignoreCase = true)
    ) {
        return pathOrUrl
    }
    // Protocol-relative ("//host/path") is already absolute, just scheme-less.
    // It matches neither prefix above, and trimStart('/') would eat BOTH
    // slashes and splice the host onto BASE_URL as if it were a path segment.
    // Nothing emits this form today; handled so it fails loudly rather than
    // silently resolving to a wrong host if something starts to.
    if (pathOrUrl.startsWith("//")) {
        return "https:$pathOrUrl"
    }
    return "${ApiClient.BASE_URL.trimEnd('/')}/${pathOrUrl.trimStart('/')}"
}

class ShowcaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collectionName = "public_showcase"

    suspend fun getShowcaseItems(): List<ShowcaseItem> {
        return try {
            val snapshot = db.collection(collectionName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.toObjects(ShowcaseItem::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun publishMagazine(item: ShowcaseItem): Boolean {
        return try {
            val documentRef = if (item.id.isNotEmpty()) {
                db.collection(collectionName).document(item.id)
            } else {
                db.collection(collectionName).document()
            }
            val newItem = item.copy(id = documentRef.id)
            documentRef.set(newItem).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
