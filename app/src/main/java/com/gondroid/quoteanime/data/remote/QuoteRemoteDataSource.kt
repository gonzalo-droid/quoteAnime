package com.gondroid.quoteanime.data.remote

import com.gondroid.quoteanime.data.remote.dto.CategoryDto
import com.gondroid.quoteanime.data.remote.dto.QuoteDto
import com.gondroid.quoteanime.data.remote.dto.toDomain
import com.gondroid.quoteanime.data.remote.dto.toQuoteDto
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class QuoteRemoteDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val quotesRef = database.getReference("quotes")
    private val imagenesRef = database.getReference("imagenes")

    /**
     * One-shot fetch of the /imagenes node.
     * Returns a map of animeSlug → list of image URLs.
     */
    suspend fun getAnimeImages(): Map<String, List<String>> {
        val snapshot = imagenesRef.get().await()
        return snapshot.children.associate { slugSnapshot ->
            val slug = slugSnapshot.key ?: ""
            val urls = slugSnapshot.children.mapNotNull { it.getValue(String::class.java) }
            slug to urls
        }
    }

    fun getCategories(): Flow<List<CategoryDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categorySet = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val dto = child.toQuoteDto() ?: continue
                    // New schema: categories array; old schema: anime field
                    if (!dto.categories.isNullOrEmpty()) {
                        categorySet.addAll(dto.categories)
                    } else {
                        dto.anime?.let { categorySet.add(it) }
                    }
                }
                val categories = categorySet.sorted().map { CategoryDto(id = it, name = it) }
                trySend(categories)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        quotesRef.addValueEventListener(listener)
        awaitClose { quotesRef.removeEventListener(listener) }
    }

    fun getAllQuotes(): Flow<List<QuoteDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val quotes = snapshot.children.mapNotNull { it.toQuoteDto() }
                trySend(quotes)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        quotesRef.addValueEventListener(listener)
        awaitClose { quotesRef.removeEventListener(listener) }
    }

    fun getQuotesByCategory(categoryId: String): Flow<List<QuoteDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val quotes = snapshot.children
                    .mapNotNull { it.toQuoteDto() }
                    .filter { dto ->
                        if (!dto.categories.isNullOrEmpty()) categoryId in dto.categories
                        else dto.anime == categoryId
                    }
                trySend(quotes)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        quotesRef.addValueEventListener(listener)
        awaitClose { quotesRef.removeEventListener(listener) }
    }

    suspend fun getRandomQuote(categoryIds: Set<String>): QuoteDto? {
        val snapshot = quotesRef.get().await()
        val allQuotes = snapshot.children.mapNotNull { it.toQuoteDto() }
        val filtered = if (categoryIds.isEmpty()) allQuotes
                       else allQuotes.filter { dto ->
                           if (!dto.categories.isNullOrEmpty()) dto.categories.any { it in categoryIds }
                           else dto.anime in categoryIds
                       }
        return filtered.randomOrNull()
    }
}
