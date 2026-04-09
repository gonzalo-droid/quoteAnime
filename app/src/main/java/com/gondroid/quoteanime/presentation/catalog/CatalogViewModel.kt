package com.gondroid.quoteanime.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.usecase.GetAllQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.GetFavoriteQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.GetQuotesByCategoryUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleFavoriteUseCase
import com.gondroid.quoteanime.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllQuotes: GetAllQuotesUseCase,
    private val getQuotesByCategory: GetQuotesByCategoryUseCase,
    private val getFavoriteQuotes: GetFavoriteQuotesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    // If launched from Home with a categoryId, jump straight to list
    private val initialFilter: CatalogFilter? =
        savedStateHandle.get<String?>(Screen.Catalog.ARG)
            ?.let { CatalogFilter.ByEmotion(it, it) }

    private val _selectedFilter = MutableStateFlow<CatalogFilter?>(initialFilter)
    private val _selectedQuoteId = MutableStateFlow<String?>(null)

    private sealed class QuotesState {
        data object Loading : QuotesState()
        data class Ready(val quotes: List<Quote>) : QuotesState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val quotesStateFlow: Flow<QuotesState> = _selectedFilter.flatMapLatest { filter ->
        when (filter) {
            null -> flowOf(QuotesState.Ready(emptyList()))
            CatalogFilter.Favorites -> getFavoriteQuotes()
                .map<List<Quote>, QuotesState> { QuotesState.Ready(it) }
                .onStart { emit(QuotesState.Loading) }
            CatalogFilter.All -> getAllQuotes()
                .map<List<Quote>, QuotesState> { QuotesState.Ready(it) }
                .onStart { emit(QuotesState.Loading) }
            is CatalogFilter.ByEmotion -> getQuotesByCategory(filter.categoryId)
                .map<List<Quote>, QuotesState> { QuotesState.Ready(it) }
                .onStart { emit(QuotesState.Loading) }
        }
    }

    val uiState: StateFlow<CatalogUiState> = combine(
        _selectedFilter,
        _selectedQuoteId,
        quotesStateFlow
    ) { filter, quoteId, state ->
        val quotes = (state as? QuotesState.Ready)?.quotes ?: emptyList()
        CatalogUiState(
            selectedFilter = filter,
            selectedQuote = quoteId?.let { id -> quotes.find { it.id == id } },
            quotes = quotes,
            isLoading = state is QuotesState.Loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState(
            selectedFilter = initialFilter,
            isLoading = initialFilter != null
        )
    )

    fun onFilterSelected(filter: CatalogFilter) {
        _selectedFilter.update { filter }
        _selectedQuoteId.update { null }
    }

    fun onBackFromList() {
        _selectedFilter.update { null }
        _selectedQuoteId.update { null }
    }

    fun onQuoteSelected(quote: Quote) {
        _selectedQuoteId.update { quote.id }
    }

    fun onBackFromDetail() {
        _selectedQuoteId.update { null }
    }

    fun onToggleFavorite(quote: Quote) {
        viewModelScope.launch { toggleFavorite(quote) }
    }
}
