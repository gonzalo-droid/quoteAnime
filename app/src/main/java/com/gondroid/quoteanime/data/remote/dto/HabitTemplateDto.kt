package com.gondroid.quoteanime.data.remote.dto

import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.google.firebase.database.DataSnapshot

data class HabitTemplateDto(
    val id: String = "",
    val title: String = "",
    val iconKey: String = "",
    val order: Int = 0,
    val themeColorIndex: Int? = null,
    val themeAnimeSlug: String? = null
)

fun DataSnapshot.toHabitTemplateDto(): HabitTemplateDto? {
    val id = key ?: return null
    val title = child("title").getValue(String::class.java) ?: return null
    val iconKey = child("iconKey").getValue(String::class.java) ?: return null
    val order = child("order").getValue(Int::class.java) ?: 0
    val themeColorIndex = child("themeColorIndex").getValue(Int::class.java)
    val themeAnimeSlug = child("themeAnimeSlug").getValue(String::class.java)
    return HabitTemplateDto(
        id = id,
        title = title,
        iconKey = iconKey,
        order = order,
        themeColorIndex = themeColorIndex,
        themeAnimeSlug = themeAnimeSlug
    )
}

fun HabitTemplateDto.toDomain(): HabitTemplate =
    HabitTemplate(
        id = id,
        title = title,
        iconKey = iconKey,
        order = order,
        themeColorIndex = themeColorIndex,
        themeAnimeSlug = themeAnimeSlug
    )
