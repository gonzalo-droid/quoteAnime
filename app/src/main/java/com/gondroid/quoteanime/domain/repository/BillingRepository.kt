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
     *  app start so a subscription cancelled/expired outside the app eventually gets caught. */
    suspend fun restorePurchases()
}
