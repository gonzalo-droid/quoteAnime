package com.gondroid.quoteanime.domain.model

data class Quote(
    val id: String,
    val quote: String,
    val author: String,
    val anime: String,
    val isFavorite: Boolean = false
)
