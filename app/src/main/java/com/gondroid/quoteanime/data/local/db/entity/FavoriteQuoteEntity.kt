package com.gondroid.quoteanime.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gondroid.quoteanime.domain.model.Quote

@Entity(tableName = "favorite_quotes")
data class FavoriteQuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val author: String,
    val categoryId: String,
    val savedAt: Long = System.currentTimeMillis()
)

fun FavoriteQuoteEntity.toDomain(): Quote = Quote(
    id = id,
    text = text,
    author = author,
    categoryId = categoryId,
    isFavorite = true
)

fun Quote.toFavoriteEntity(): FavoriteQuoteEntity = FavoriteQuoteEntity(
    id = id,
    text = text,
    author = author,
    categoryId = categoryId
)
