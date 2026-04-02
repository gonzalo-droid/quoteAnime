package com.gondroid.quoteanime.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.usecase.GetAllQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllQuotes: GetAllQuotesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadQuotes()
    }

    private fun loadQuotes() {
        viewModelScope.launch {
            runCatching {
                getAllQuotes().collect { quotes ->
                    _uiState.update { it.copy(quotes = quotes, isLoading = false, error = null) }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun onToggleFavorite(quote: Quote) {
        viewModelScope.launch { toggleFavorite(quote) }
    }
}
