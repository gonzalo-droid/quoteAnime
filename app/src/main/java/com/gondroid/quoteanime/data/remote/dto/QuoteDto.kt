package com.gondroid.quoteanime.data.remote.dto

import com.gondroid.quoteanime.domain.model.Quote
import com.google.firebase.firestore.DocumentSnapshot

data class QuoteDto(
    val id: String,
    val text: String,
    val author: String,
    val categoryId: String
)

fun DocumentSnapshot.toQuoteDto(): QuoteDto? {
    return QuoteDto(
        id = id,
        text = getString("text") ?: return null,
        author = getString("author") ?: return null,
        categoryId = getString("categoryId") ?: return null
    )
}

fun QuoteDto.toDomain(isFavorite: Boolean = false): Quote = Quote(
    id = id,
    text = text,
    author = author,
    categoryId = categoryId,
    isFavorite = isFavorite
)
