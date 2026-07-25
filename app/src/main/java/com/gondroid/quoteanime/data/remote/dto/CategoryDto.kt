package com.gondroid.quoteanime.data.remote.dto

import com.gondroid.quoteanime.domain.model.Category

data class CategoryDto(
    val id: String,
    val name: String,
    val imageUrl: String = ""
)

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    name = name,
    imageUrl = imageUrl
)
