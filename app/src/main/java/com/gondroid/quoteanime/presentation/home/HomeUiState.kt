package com.gondroid.quoteanime.presentation.home

import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.Quote

data class HomeUiState(
    val featuredQuote: Quote? = null,
    val categories: List<Category> = emptyList(),
    val isQuoteLoading: Boolean = true,
    val isCategoriesLoading: Boolean = true,
    val error: String? = null
) {
    val isLoading: Boolean get() = isQuoteLoading && isCategoriesLoading
}
