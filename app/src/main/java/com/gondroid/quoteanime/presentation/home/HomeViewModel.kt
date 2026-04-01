package com.gondroid.quoteanime.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.usecase.GetCategoriesUseCase
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.ObserveFavoriteStatusUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val observeFavoriteStatus: ObserveFavoriteStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Job para cancelar la observación de favorito cuando se carga una nueva frase
    private var favoriteObserverJob: Job? = null

    init {
        loadFeaturedQuote()
        loadCategories()
    }

    fun loadFeaturedQuote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isQuoteLoading = true, error = null) }

            runCatching {
                val prefs = getUserPreferences().first()
                getRandomQuote(prefs.selectedCategoryIds)
            }.onSuccess { quote ->
                _uiState.update { it.copy(featuredQuote = quote, isQuoteLoading = false) }
                // Observar estado de favorito reactivamente desde Room
                if (quote != null) observeFavorite(quote.id)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isQuoteLoading = false, error = error.message)
                }
            }
        }
    }

    fun onToggleFavorite() {
        val quote = _uiState.value.featuredQuote ?: return
        viewModelScope.launch {
            toggleFavorite(quote)
        }
        // El estado isFavorite se actualiza reactivamente via observeFavorite()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories, isCategoriesLoading = false) }
            }
        }
    }

    private fun observeFavorite(quoteId: String) {
        favoriteObserverJob?.cancel()
        favoriteObserverJob = viewModelScope.launch {
            observeFavoriteStatus(quoteId).collect { isFavorite ->
                _uiState.update { state ->
                    state.copy(featuredQuote = state.featuredQuote?.copy(isFavorite = isFavorite))
                }
            }
        }
    }
}
