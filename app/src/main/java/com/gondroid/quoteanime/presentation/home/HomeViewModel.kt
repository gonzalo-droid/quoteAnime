package com.gondroid.quoteanime.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.usecase.GetAllQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleFavoriteUseCase
import com.gondroid.quoteanime.presentation.ads.ShareInterstitialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllQuotes: GetAllQuotesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    val shareInterstitialManager: ShareInterstitialManager
) : ViewModel() {

    // Populated when launched from the widget (navigation arg "quoteId")
    private val widgetQuoteId: String? = savedStateHandle["quoteId"]

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadQuotes()
    }

    private fun loadQuotes() {
        viewModelScope.launch {
            runCatching {
                getAllQuotes().collect { quotes ->
                    val scrollTo = if (widgetQuoteId != null && _uiState.value.scrollToPage == null) {
                        quotes.indexOfFirst { it.id == widgetQuoteId }.takeIf { it >= 0 }
                    } else null
                    _uiState.update {
                        it.copy(quotes = quotes, isLoading = false, error = null,
                            scrollToPage = scrollTo ?: it.scrollToPage)
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun onToggleFavorite(quote: Quote) {
        viewModelScope.launch { toggleFavorite(quote) }
    }

    /** Consumed by the UI after scrolling — prevents repeated scroll */
    fun onScrollToPageConsumed() {
        _uiState.update { it.copy(scrollToPage = null) }
    }
}
