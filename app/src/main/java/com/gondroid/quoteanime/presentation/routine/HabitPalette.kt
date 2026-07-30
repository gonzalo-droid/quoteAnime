package com.gondroid.quoteanime.presentation.routine

import androidx.compose.ui.graphics.Color

/**
 * Fixed palette so every habit stays readable over the app's dark background.
 * Habits persist the index, never a hex value: changing a color here updates
 * existing habits without a data migration.
 */
object HabitPalette {

    val COLORS: List<Color> = listOf(
        Color(0xFFA78BFA), // brand purple
        Color(0xFFFF6B8A), // rose
        Color(0xFF4ADE80), // green
        Color(0xFF38BDF8), // sky
        Color(0xFFFBBF24), // amber
        Color(0xFFFB7185), // coral
        Color(0xFF2DD4BF), // teal
        Color(0xFFE879F9), // fuchsia
        Color(0xFF818CF8), // indigo
        Color(0xFFA3E635), // lime
        Color(0xFFFB923C), // orange
        Color(0xFFF87171), // red
        Color(0xFF67E8F9), // cyan
        Color(0xFFF472B6)  // pink
    )

    fun colorAt(index: Int): Color = COLORS[index.mod(COLORS.size)]
}
