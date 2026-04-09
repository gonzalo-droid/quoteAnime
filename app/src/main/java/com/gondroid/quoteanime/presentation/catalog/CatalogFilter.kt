package com.gondroid.quoteanime.presentation.catalog

sealed class CatalogFilter(val label: String) {
    data object Favorites : CatalogFilter("Favoritos")
    data object All : CatalogFilter("Todas")
    data class ByEmotion(val categoryId: String, val emotionLabel: String) : CatalogFilter(emotionLabel)
}
