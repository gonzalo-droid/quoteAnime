package com.gondroid.quoteanime.data.remote

import com.gondroid.quoteanime.data.remote.dto.QuoteDto
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

    /**
     * One-shot read of every quote, for the notification and widget workers. The anime filter
     * and the random pick live in the domain (`pickRandomFromAnimes`) so Home, the workers and
     * their tests share one rule.
     */
    suspend fun getAllQuotesOnce(): List<QuoteDto> =
        quotesRef.get().await().children.mapNotNull { it.toQuoteDto() }
}
