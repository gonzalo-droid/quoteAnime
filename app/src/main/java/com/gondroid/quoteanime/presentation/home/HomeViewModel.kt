package com.gondroid.quoteanime.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.model.filterByCategories
import com.gondroid.quoteanime.domain.usecase.GetAllQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleFavoriteUseCase
import com.gondroid.quoteanime.presentation.ads.ShareInterstitialManager
import com.gondroid.quoteanime.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllQuotes: GetAllQuotesUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    val shareInterstitialManager: ShareInterstitialManager
) : ViewModel() {

    /**
     * The quote the widget asked for (navigation arg "quoteId"), until the first feed is shown.
     * Looked up once: afterwards the feed re-emits on every favorite toggle and every change of
     * the anime selection, and looking it up again would yank the pager back to it each time.
     * A quote the feed doesn't hold (removed, or outside the selection) leaves Home at the top.
     */
    private var pendingFocusQuoteId: String? =
        savedStateHandle[Screen.Home.ARG_QUOTE_ID]

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadQuotes()
    }

    /**
     * The feed shows only the animes chosen in Settings (empty selection = all), like iOS: "if
     * I picked three animes, I want to see those three". The selection is observed, so coming
     * back from Settings already shows the new feed.
     */
    private fun loadQuotes() {
        val selection = getUserPreferences()
            .map { it.selectedCategoryIds }
            .distinctUntilChanged()

        viewModelScope.launch {
            combine(getAllQuotes(), selection) { quotes, categoryIds ->
                quotes.filterByCategories(categoryIds) to categoryIds
            }
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
                .collect { (quotes, categoryIds) ->
                    val scrollTo = pendingFocusQuoteId?.let { id ->
                        pendingFocusQuoteId = null
                        quotes.indexOfFirst { it.id == id }.takeIf { it >= 0 }
                    }
                    _uiState.update {
                        it.copy(
                            quotes = quotes,
                            appliedCategoryIds = categoryIds,
                            isLoading = false,
                            error = null,
                            scrollToPage = scrollTo ?: it.scrollToPage
                        )
                    }
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
