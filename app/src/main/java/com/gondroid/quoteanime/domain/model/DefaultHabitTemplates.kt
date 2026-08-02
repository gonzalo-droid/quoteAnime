package com.gondroid.quoteanime.domain.model

/**
 * Bundled fallback so the habit editor is never empty on first launch or offline.
 * The remote /habitTemplates node overrides this list when available.
 * Titles are placeholders resolved to strings.xml keys by the UI layer.
 *
 * Suggestions are 100% themed: every entry sets a color and a bundled cover image
 * (see HabitThemeImages) in addition to title and icon — no plain/generic presets.
 *
 * The two newest themes (Pokémon, Black Clover) are premium-exclusive — the original
 * three stay free so no existing user loses a suggestion they already had.
 */
object DefaultHabitTemplates {
    val ALL: List<HabitTemplate> = listOf(
        HabitTemplate("theme_ninja", "template_theme_ninja", "dumbbell", 1, themeColorIndex = 10, themeKey = "ninja"),
        HabitTemplate("theme_one_piece", "template_theme_one_piece", "directions_walk", 2, themeColorIndex = 11, themeKey = "one_piece"),
        HabitTemplate("theme_saiyan", "template_theme_saiyan", "self_improvement", 3, themeColorIndex = 3, themeKey = "saiyan"),
        HabitTemplate("theme_pokemon", "template_theme_pokemon", "emoji_events", 4, themeColorIndex = 4, themeKey = "pokemon", isPremiumOnly = true),
        HabitTemplate("theme_black_clover", "template_theme_black_clover", "local_fire_department", 5, themeColorIndex = 9, themeKey = "black_clover", isPremiumOnly = true)
    )
}
