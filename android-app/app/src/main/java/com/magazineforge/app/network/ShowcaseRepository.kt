package com.magazineforge.app.network

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.magazineforge.app.models.ShowcaseItem
import kotlinx.coroutines.tasks.await

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
