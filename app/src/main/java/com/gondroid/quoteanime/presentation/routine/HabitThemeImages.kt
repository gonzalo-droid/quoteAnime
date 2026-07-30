package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.R

/** Resolves a [com.gondroid.quoteanime.domain.model.HabitTemplate.themeKey] to its bundled cover image. */
object HabitThemeImages {

    private val BY_KEY: Map<String, Int> = mapOf(
        "ninja" to R.drawable.onboarding_01,
        "one_piece" to R.drawable.onboarding_02,
        "saiyan" to R.drawable.onboarding_03
    )

    fun resFor(themeKey: String?): Int? = themeKey?.let { BY_KEY[it] }
}
