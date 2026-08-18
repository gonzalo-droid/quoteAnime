package com.gondroid.quoteanime.data.repository

import com.android.billingclient.api.PendingPurchasesParams
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Guards the crash fixed in [BillingRepositoryImpl]: since Play Billing 8 the builder rejects
 * params that don't opt into pending one-time purchases, which blew up inside Hilt's singleton
 * construction and killed the app on start.
 */
class PendingPurchasesParamsTest {

    @Test
    fun `build throws when one-time products are not enabled`() {
        assertThrows(IllegalArgumentException::class.java) {
            PendingPurchasesParams.newBuilder().build()
        }
    }

    @Test
    fun `build succeeds when one-time products are enabled`() {
        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
    }
}
