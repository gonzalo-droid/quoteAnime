package com.gondroid.quoteanime.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.usecase.GetCategoriesUseCase
import com.gondroid.quoteanime.domain.usecase.GetFavoriteQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.GetQuotesByCategoryUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleFavoriteUseCase
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCategories: GetCategoriesUseCase,
    private val getQuotesByCategory: GetQuotesByCategoryUseCase,
    private val getFavoriteQuotes: GetFavoriteQuotesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    // Categoría inicial proveniente del argumento de navegación (puede ser null = Favoritos)
    private val _selectedCategoryId = MutableStateFlow(
        savedStateHandle.get<String?>(Screen.Catalog.ARG)
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val quotesFlow = _selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) getFavoriteQuotes() else getQuotesByCategory(categoryId)
    }

    val uiState: StateFlow<CatalogUiState> = combine(
        getCategories(),
        _selectedCategoryId,
        quotesFlow
    ) { categories, selectedId, quotes ->
        CatalogUiState(
            categories = categories,
            selectedCategoryId = selectedId,
            quotes = quotes,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState()
    )

    fun onCategorySelected(categoryId: String?) {
        _selectedCategoryId.update { categoryId }
    }

    fun onToggleFavorite(quote: Quote) {
        viewModelScope.launch { toggleFavorite(quote) }
    }
}
