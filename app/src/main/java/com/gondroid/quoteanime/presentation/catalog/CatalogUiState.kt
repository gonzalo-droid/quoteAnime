package com.gondroid.quoteanime.presentation.catalog

import com.gondroid.quoteanime.domain.model.Quote

data class CatalogUiState(
    val selectedFilter: CatalogFilter? = null,   // null = Selector view
    val selectedQuote: Quote? = null,            // non-null = Detail view
    val quotes: List<Quote> = emptyList(),
    val isLoading: Boolean = false
) {
    val isEmpty: Boolean get() = !isLoading && quotes.isEmpty()
}
