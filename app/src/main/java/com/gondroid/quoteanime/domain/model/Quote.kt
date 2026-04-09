package com.gondroid.quoteanime.domain.model

data class Quote(
    val id: String,
    val anime: String?,
    val author: String?,
    val quote: String?,
    val categories: List<String> = emptyList(),
    val imageUrl: String? = null,
    val isFavorite: Boolean = false
)
