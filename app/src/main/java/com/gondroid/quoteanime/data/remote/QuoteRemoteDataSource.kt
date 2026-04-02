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

    fun getCategories(): Flow<List<CategoryDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val animeSet = mutableSetOf<String>()
                for (child in snapshot.children) {
                    child.toQuoteDto()?.anime?.let { animeSet.add(it) }
                }
                val categories = animeSet.sorted().map { CategoryDto(id = it, name = it) }
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
                    .filter { it.anime == categoryId }
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
                       else allQuotes.filter { it.anime in categoryIds }
        return filtered.randomOrNull()
    }
}
