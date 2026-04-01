package com.gondroid.quoteanime.data.remote.dto

import com.gondroid.quoteanime.domain.model.Category
import com.google.firebase.firestore.DocumentSnapshot

data class CategoryDto(
    val id: String,
    val name: String,
    val imageUrl: String
)

fun DocumentSnapshot.toCategoryDto(): CategoryDto? {
    return CategoryDto(
        id = id,
        name = getString("name") ?: return null,
        imageUrl = getString("imageUrl") ?: ""
    )
}

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    name = name,
    imageUrl = imageUrl
)
