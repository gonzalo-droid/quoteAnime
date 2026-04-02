package com.gondroid.quoteanime.data.remote.dto

import com.gondroid.quoteanime.domain.model.Quote
import com.google.firebase.database.DataSnapshot

data class QuoteDto(
    val id: String,
    val quote: String,
    val author: String,
    val anime: String
)

fun DataSnapshot.toQuoteDto(): QuoteDto? {
    return QuoteDto(
        id = child("id").getValue(Long::class.java)?.toString() ?: key ?: return null,
        quote = child("quote").getValue(String::class.java) ?: return null,
        author = child("author").getValue(String::class.java) ?: return null,
        anime = child("anime").getValue(String::class.java) ?: return null
    )
}

fun QuoteDto.toDomain(isFavorite: Boolean = false): Quote = Quote(
    id = id,
    quote = quote,
    author = author,
    anime = anime,
    isFavorite = isFavorite
)
