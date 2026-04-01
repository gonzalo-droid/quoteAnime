package com.gondroid.quoteanime.data.remote

import com.gondroid.quoteanime.data.remote.dto.CategoryDto
import com.gondroid.quoteanime.data.remote.dto.QuoteDto
import com.gondroid.quoteanime.data.remote.dto.toCategoryDto
import com.gondroid.quoteanime.data.remote.dto.toQuoteDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class QuoteRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getCategories(): Flow<List<CategoryDto>> = callbackFlow {
        val listener = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents
                    ?.mapNotNull { it.toCategoryDto() }
                    ?: emptyList()
                trySend(categories)
            }
        awaitClose { listener.remove() }
    }

    fun getQuotesByCategory(categoryId: String): Flow<List<QuoteDto>> = callbackFlow {
        val listener = firestore.collection("quotes")
            .whereEqualTo("categoryId", categoryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val quotes = snapshot?.documents
                    ?.mapNotNull { it.toQuoteDto() }
                    ?: emptyList()
                trySend(quotes)
            }
        awaitClose { listener.remove() }
    }

    // Usado por WorkManager y Widget para obtener una frase aleatoria sin Flow
    suspend fun getRandomQuote(categoryIds: Set<String>): QuoteDto? {
        val query = if (categoryIds.isEmpty()) {
            firestore.collection("quotes")
        } else {
            // Firestore no soporta whereIn con más de 30 valores; se asume uso razonable
            firestore.collection("quotes")
                .whereIn("categoryId", categoryIds.toList())
        }
        val snapshot = query.get().await()
        return snapshot.documents.randomOrNull()?.toQuoteDto()
    }
}
