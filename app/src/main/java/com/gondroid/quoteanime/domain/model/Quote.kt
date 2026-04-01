package com.gondroid.quoteanime.domain.model

data class Quote(
    val id: String,
    val text: String,
    val author: String,
    val categoryId: String,
    val isFavorite: Boolean = false
)
