package com.gondroid.quoteanime.domain.usecase

import android.app.Activity
import com.gondroid.quoteanime.domain.model.BillingPurchaseResult
import com.gondroid.quoteanime.domain.model.SubscriptionOffer
import com.gondroid.quoteanime.domain.repository.BillingRepository
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class GetSubscriptionOffersUseCase @Inject constructor(
    private val repository: BillingRepository
) {
    suspend operator fun invoke(): List<SubscriptionOffer> = repository.queryOffers()
}

class LaunchSubscriptionPurchaseUseCase @Inject constructor(
    private val repository: BillingRepository
) {
    operator fun invoke(activity: Activity, offer: SubscriptionOffer) =
        repository.launchPurchaseFlow(activity, offer)
}

class ObservePurchaseEventsUseCase @Inject constructor(
    private val repository: BillingRepository
) {
    operator fun invoke(): SharedFlow<BillingPurchaseResult> = repository.purchaseEvents
}

/** Called on app start — see [BillingRepository.restorePurchases]. */
class RestorePurchasesUseCase @Inject constructor(
    private val repository: BillingRepository
) {
    suspend operator fun invoke() = repository.restorePurchases()
}
