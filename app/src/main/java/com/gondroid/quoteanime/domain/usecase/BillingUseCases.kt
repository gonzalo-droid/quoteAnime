package com.gondroid.quoteanime.domain.usecase

import android.app.Activity
import android.content.Context
import com.gondroid.quoteanime.domain.model.BillingPurchaseResult
import com.gondroid.quoteanime.domain.model.SubscriptionOffer
import com.gondroid.quoteanime.domain.repository.BillingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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

/** Retry path for Play's 72 h acknowledgement window — see `AcknowledgePurchasesWorker`. */
class AcknowledgePendingPurchasesUseCase @Inject constructor(
    private val repository: BillingRepository
) {
    suspend operator fun invoke(): Boolean = repository.acknowledgePendingPurchases()
}

/**
 * Play's subscription centre is the only place a subscription can be cancelled — the Billing
 * Library exposes no cancel API — so the app can only deep-link the user there.
 */
class GetManageSubscriptionUrlUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): String = "$BASE_URL" +
            "?sku=${BillingRepository.PREMIUM_PRODUCT_ID}&package=${context.packageName}"

    private companion object {
        const val BASE_URL = "https://play.google.com/store/account/subscriptions"
    }
}
