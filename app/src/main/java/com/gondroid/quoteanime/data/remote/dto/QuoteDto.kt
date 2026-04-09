package com.gondroid.quoteanime.data.remote.dto

import com.gondroid.quoteanime.domain.model.Quote
import com.google.firebase.database.DataSnapshot

data class QuoteDto(
    val id: String,
    val quote: String?,
    val author: String?,
    val anime: String?,
    val categories: List<String>?,
    val imageUrl: String?
)

fun DataSnapshot.toQuoteDto(): QuoteDto? {
    val id = child("id").getValue(Long::class.java)?.toString() ?: key ?: return null
    val categories = runCatching {
        child("categories").children
            .mapNotNull { it.getValue(String::class.java) }
            .takeIf { it.isNotEmpty() }
    }.getOrNull()
    return QuoteDto(
        id = id,
        quote = child("quote").getValue(String::class.java),
        author = child("author").getValue(String::class.java),
        anime = child("anime").getValue(String::class.java),
        categories = categories,
        imageUrl = child("imageUrl").getValue(String::class.java)
    )
}

fun QuoteDto.toDomain(isFavorite: Boolean = false): Quote = Quote(
    id = id,
    quote = quote,
    author = author,
    anime = anime,
    categories = categories ?: emptyList(),
    imageUrl = imageUrl,
    isFavorite = isFavorite
)
