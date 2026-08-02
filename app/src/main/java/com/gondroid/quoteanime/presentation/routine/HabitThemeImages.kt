package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.R

/** Resolves a [com.gondroid.quoteanime.domain.model.HabitTemplate.themeKey] to its bundled cover image. */
object HabitThemeImages {

    private val BY_KEY: Map<String, Int> = mapOf(
        "ninja" to R.drawable.naruto,
        "one_piece" to R.drawable.onepiece,
        "saiyan" to R.drawable.dragonball,
        "pokemon" to R.drawable.pokemon,
        "black_clover" to R.drawable.blackclover,
    )

    fun resFor(themeKey: String?): Int? = themeKey?.let { BY_KEY[it] }
}
