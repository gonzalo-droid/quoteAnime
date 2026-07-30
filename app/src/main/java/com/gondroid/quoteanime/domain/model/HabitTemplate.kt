package com.gondroid.quoteanime.domain.model

data class HabitTemplate(
    val id: String,
    val title: String,
    val iconKey: String,
    val order: Int,
    /** Suggested [com.gondroid.quoteanime.presentation.routine.HabitPalette] index, for themed templates. */
    val themeColorIndex: Int? = null,
    /** Opaque key resolved by the presentation layer to this theme's cover image and description. */
    val themeKey: String? = null
)
