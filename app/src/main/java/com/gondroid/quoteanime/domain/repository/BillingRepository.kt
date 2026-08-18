package com.gondroid.quoteanime.domain.repository

import android.app.Activity
import com.gondroid.quoteanime.domain.model.BillingPurchaseResult
import com.gondroid.quoteanime.domain.model.SubscriptionOffer
import kotlinx.coroutines.flow.SharedFlow

/**
 * Play Billing needs an [Activity] to launch its purchase UI — the one Android-framework
 * type this domain interface can't avoid, since there's no purely abstract equivalent.
 */
interface BillingRepository {
    val purchaseEvents: SharedFlow<BillingPurchaseResult>

    suspend fun queryOffers(): List<SubscriptionOffer>
    fun launchPurchaseFlow(activity: Activity, offer: SubscriptionOffer)

    /** Re-checks Play for existing purchases and syncs the local entitlement flag. Call on
     *  app start and on every return to the foreground, so a subscription cancelled/expired
     *  outside the app gets caught. Throttled internally — calling it often is cheap. */
    suspend fun restorePurchases()

    /**
     * Acknowledges every purchase Play still reports as unacknowledged.
     *
     * Google auto-refunds and revokes any purchase not acknowledged within 72 h, so this is
     * the retry path used by `AcknowledgePurchasesWorker` when the acknowledgement made right
     * after the purchase failed.
     *
     * @return `false` when the work should be retried later.
     */
    suspend fun acknowledgePendingPurchases(): Boolean

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_subscription"
    }
}
