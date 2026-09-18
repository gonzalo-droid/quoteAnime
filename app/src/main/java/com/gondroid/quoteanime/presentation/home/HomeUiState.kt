package com.gondroid.quoteanime.presentation.home

import com.gondroid.quoteanime.domain.model.Quote

data class HomeUiState(
    val quotes: List<Quote> = emptyList(),
    /** The anime selection [quotes] was filtered with; a new one restarts the pager at the top. */
    val appliedAnimes: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val scrollToPage: Int? = null   // set once when widget deep-link arrives
)
