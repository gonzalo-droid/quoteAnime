package com.gondroid.quoteanime.presentation.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.model.BillingPurchaseResult
import com.gondroid.quoteanime.domain.usecase.GetSubscriptionOffersUseCase
import com.gondroid.quoteanime.domain.usecase.LaunchSubscriptionPurchaseUseCase
import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import com.gondroid.quoteanime.domain.usecase.ObservePurchaseEventsUseCase
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
    private val setPremiumStatus: SetPremiumStatusUseCase,
    private val getSubscriptionOffers: GetSubscriptionOffersUseCase,
    private val launchSubscriptionPurchase: LaunchSubscriptionPurchaseUseCase,
    private val observePurchaseEvents: ObservePurchaseEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePremiumStatus().collect { isPremium ->
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
        viewModelScope.launch {
            observePurchaseEvents().collect { event ->
                _uiState.update {
                    it.copy(
                        message = when (event) {
                            is BillingPurchaseResult.Success -> null
                            is BillingPurchaseResult.Pending -> PaywallMessage.PENDING
                            is BillingPurchaseResult.UserCancelled -> PaywallMessage.USER_CANCELLED
                            is BillingPurchaseResult.Error -> PaywallMessage.ERROR
                        }
                    )
                }
            }
        }
        loadOffers()
    }

    private fun loadOffers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOffers = true) }
            val offers = getSubscriptionOffers()
            _uiState.update { it.copy(offers = offers, isLoadingOffers = false) }
        }
    }

    fun onOfferSelected(index: Int) {
        _uiState.update { it.copy(selectedOfferIndex = index) }
    }

    fun onSubscribe(activity: Activity) {
        val offer = _uiState.value.selectedOffer ?: return
        launchSubscriptionPurchase(activity, offer)
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /** QA-only affordance to flip back off, since there is no real subscription to cancel
     *  from within the app. Only meaningful while testing with a license tester account. */
    fun onRemovePremiumForTesting() {
        viewModelScope.launch { setPremiumStatus(false) }
    }
}
