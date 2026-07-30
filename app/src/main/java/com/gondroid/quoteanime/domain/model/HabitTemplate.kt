package com.gondroid.quoteanime.domain.model

data class HabitTemplate(
    val id: String,
    val title: String,
    val iconKey: String,
    val order: Int,
    /** Suggested [com.gondroid.quoteanime.presentation.routine.HabitPalette] index, for templates with a clear anime tie-in. */
    val themeColorIndex: Int? = null,
    /** Anime slug used to fetch this template's themed background, if any. */
    val themeAnimeSlug: String? = null
)
