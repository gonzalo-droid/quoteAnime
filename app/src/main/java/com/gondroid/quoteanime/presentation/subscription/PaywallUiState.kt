package com.gondroid.quoteanime.presentation.subscription

import com.gondroid.quoteanime.domain.model.SubscriptionOffer

enum class PaywallMessage {
    PENDING,
    USER_CANCELLED,
    ERROR,
    MANAGE_UNAVAILABLE
}

data class PaywallUiState(
    val isPremium: Boolean = false,
    val isLoadingOffers: Boolean = true,
    val offers: List<SubscriptionOffer> = emptyList(),
    val selectedOfferIndex: Int = 0,
    val message: PaywallMessage? = null,
    /** Deep link to Play's subscription centre — the only place a subscription can be cancelled. */
    val manageSubscriptionUrl: String = ""
) {
    val selectedOffer: SubscriptionOffer?
        get() = offers.getOrNull(selectedOfferIndex)
}
