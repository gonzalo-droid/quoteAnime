package com.gondroid.quoteanime.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gondroid.quoteanime.domain.model.Quote

@Entity(tableName = "favorite_quotes")
data class FavoriteQuoteEntity(
    @PrimaryKey val id: String,
    val quote: String?,
    val author: String?,
    val anime: String?,
    val categories: List<String> = emptyList(),
    val imageUrl: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)

fun FavoriteQuoteEntity.toDomain(): Quote = Quote(
    id = id,
    quote = quote,
    author = author,
    anime = anime,
    categories = categories,
    imageUrl = imageUrl,
    isFavorite = true
)

fun Quote.toFavoriteEntity(): FavoriteQuoteEntity = FavoriteQuoteEntity(
    id = id,
    quote = quote,
    author = author,
    anime = anime,
    categories = categories,
    imageUrl = imageUrl
)
