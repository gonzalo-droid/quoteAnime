package com.gondroid.quoteanime.presentation.home

import com.gondroid.quoteanime.domain.model.Quote

data class HomeUiState(
    val quotes: List<Quote> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
