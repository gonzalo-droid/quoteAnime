package com.gondroid.quoteanime.domain.model

data class Quote(
    val id: String,
    val anime: String?,
    val author: String?,
    val quote: String?,
    val categories: List<String> = emptyList(),
    val animeSlug: String? = null,   // identifier used to resolve images from /imagenes/{slug}
    val imageUrl: String? = null,    // resolved at runtime from the /imagenes node
    val isFavorite: Boolean = false
)
