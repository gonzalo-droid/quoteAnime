package com.gondroid.quoteanime.presentation.routine

import java.text.Normalizer
import java.util.Locale

/**
 * The icon picker's search rule, kept pure so it can be tested without Compose.
 *
 * The query is matched as a substring of each icon's **visible name in the user's language**
 * ([describeIcon] — "Beber agua", "Drink water"), never against the stable key or the category
 * title. Categories keep their header and only lose the icons that don't match; a category left
 * with none disappears; a blank query shows everything.
 *
 * Case **and accents** are ignored ("musica" finds "Tocar música") and surrounding whitespace is
 * trimmed: on a phone keyboard the accent is usually skipped and autocorrect leaves a trailing
 * space. Same behaviour as `HabitIconSearch.swift` on iOS.
 */
object HabitIconSearch {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    /**
     * The categories to draw for [query], each with the keys that match. [label] resolves an icon
     * key to its visible name; it is injected so the caller resolves resources and tests can pin
     * the text.
     */
    fun filter(
        categories: List<HabitIconCategory>,
        query: String,
        label: (String) -> String
    ): List<HabitIconCategory> {
        val needle = normalize(query)
        if (needle.isEmpty()) return categories
        return categories.mapNotNull { category ->
            val keys = category.keys.filter { normalize(label(it)).contains(needle) }
            if (keys.isEmpty()) null else category.copy(keys = keys)
        }
    }

    /** Trims, decomposes (NFD) and drops the combining marks, then lowercases: "  Música " → "musica". */
    internal fun normalize(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)
}
