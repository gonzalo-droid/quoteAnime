package com.gondroid.quoteanime.presentation.catalog

import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.Quote

data class CatalogUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null, // null = pestaña Favoritos
    val quotes: List<Quote> = emptyList(),
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean get() = !isLoading && quotes.isEmpty()
    val isFavoritesTab: Boolean get() = selectedCategoryId == null
}
