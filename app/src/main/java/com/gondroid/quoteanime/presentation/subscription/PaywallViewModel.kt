package com.gondroid.quoteanime.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import com.gondroid.quoteanime.domain.usecase.SetPremiumStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val observePremiumStatus: ObservePremiumStatusUseCase,
    private val setPremiumStatus: SetPremiumStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePremiumStatus().collect { isPremium ->
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }

    /** Pre-billing mock "purchase" — see [SetPremiumStatusUseCase]. */
    fun onSubscribe() {
        viewModelScope.launch { setPremiumStatus(true) }
    }

    /** QA-only affordance to flip back off, since there is no real subscription to cancel
     *  yet. Remove once Google Play Billing drives this flag instead of a button tap. */
    fun onRemovePremiumForTesting() {
        viewModelScope.launch { setPremiumStatus(false) }
    }
}
