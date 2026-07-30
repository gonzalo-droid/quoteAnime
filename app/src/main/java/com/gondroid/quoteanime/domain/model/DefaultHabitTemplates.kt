package com.gondroid.quoteanime.domain.model

/**
 * Bundled fallback so the habit editor is never empty on first launch or offline.
 * The remote /habitTemplates node overrides this list when available.
 * Titles are placeholders resolved to strings.xml keys by the UI layer.
 */
object DefaultHabitTemplates {
    val ALL: List<HabitTemplate> = listOf(
        HabitTemplate("train", "template_train", "dumbbell", 1),
        HabitTemplate("read", "template_read", "book", 2),
        HabitTemplate("meditate", "template_meditate", "self_improvement", 3),
        HabitTemplate("water", "template_water", "water_drop", 4),
        HabitTemplate("sleep_early", "template_sleep_early", "bedtime", 5),
        HabitTemplate("study", "template_study", "school", 6),
        HabitTemplate("write", "template_write", "edit_note", 7),
        HabitTemplate("walk", "template_walk", "directions_walk", 8),
        // Themed suggestions: selecting one sets the color and a bundled cover image
        // (see HabitThemeImages) in addition to title and icon.
        HabitTemplate("theme_ninja", "template_theme_ninja", "dumbbell", 9, themeColorIndex = 10, themeKey = "ninja"),
        HabitTemplate("theme_one_piece", "template_theme_one_piece", "directions_walk", 10, themeColorIndex = 11, themeKey = "one_piece"),
        HabitTemplate("theme_saiyan", "template_theme_saiyan", "self_improvement", 11, themeColorIndex = 3, themeKey = "saiyan")
    )
}
