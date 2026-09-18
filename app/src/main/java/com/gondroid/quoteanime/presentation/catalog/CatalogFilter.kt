package com.gondroid.quoteanime.presentation.catalog

/**
 * What the catalog list shows. Carries no display text: the screen resolves each label from
 * string resources, so it follows the device language.
 */
sealed class CatalogFilter {
    data object Favorites : CatalogFilter()
    data object All : CatalogFilter()

    /** [emotionLabel] is a fallback for an id the screen has no label for (it shows the id). */
    data class ByEmotion(val categoryId: String, val emotionLabel: String) : CatalogFilter()
}
