package com.gondroid.quoteanime.data.remote

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exists so `BillingRepositoryImpl` doesn't build its own [BillingClient] out of a Context.
 * That construction is what made the repository — the one place in the app where a mistake
 * costs the user money — impossible to unit test.
 */
interface BillingClientFactory {
    fun create(listener: PurchasesUpdatedListener): BillingClient
}

@Singleton
class PlayBillingClientFactory @Inject constructor(
    @ApplicationContext private val context: Context
) : BillingClientFactory {

    /**
     * [PendingPurchasesParams.Builder.build] throws unless one-time products are opted in —
     * mandatory since Play Billing 8, even for a subscription-only catalogue like this one.
     * Prepaid plans stay off because no offer in the Play Console uses them.
     */
    override fun create(listener: PurchasesUpdatedListener): BillingClient =
        BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()
}
