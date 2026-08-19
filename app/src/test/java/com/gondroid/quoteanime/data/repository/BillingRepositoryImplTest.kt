package com.gondroid.quoteanime.data.repository

import android.os.SystemClock
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchasesAsync
import com.gondroid.quoteanime.data.local.datastore.UserPreferencesDataStore
import com.gondroid.quoteanime.data.remote.BillingClientFactory
import com.gondroid.quoteanime.worker.PurchaseAcknowledgementScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the rules that cost real money when they break: never revoking an entitlement on a
 * failed query, and never leaving a purchase unacknowledged (Play auto-refunds after 72 h).
 *
 * The Play client is mocked through [BillingClientFactory]; the suspend helpers are top-level
 * extensions, hence the `mockkStatic` on their generated file class.
 */
class BillingRepositoryImplTest {

    private val billingClient = mockk<BillingClient>(relaxed = true)
    private val dataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val scheduler = mockk<PurchaseAcknowledgementScheduler>(relaxed = true)

    private fun buildRepository(): BillingRepositoryImpl {
        val factory = mockk<BillingClientFactory>()
        every { factory.create(any()) } returns billingClient
        return BillingRepositoryImpl(factory, dataStore, scheduler)
    }

    private fun result(code: Int): BillingResult =
        BillingResult.newBuilder().setResponseCode(code).build()

    private fun purchase(state: Int, acknowledged: Boolean): Purchase = mockk {
        every { purchaseState } returns state
        every { isAcknowledged } returns acknowledged
        every { purchaseToken } returns "token"
    }

    @Before
    fun setup() {
        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
        every { billingClient.isReady } returns true
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `a failed purchase query never writes the entitlement flag`() = runTest {
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
                PurchasesResult(result(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE), emptyList())

        buildRepository().restorePurchases()

        coVerify(exactly = 0) { dataStore.setPremium(any()) }
    }

    @Test
    fun `an active purchase marks the user as premium`() = runTest {
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
                PurchasesResult(
                    result(BillingClient.BillingResponseCode.OK),
                    listOf(purchase(Purchase.PurchaseState.PURCHASED, acknowledged = true))
                )

        buildRepository().restorePurchases()

        coVerify { dataStore.setPremium(true) }
    }

    @Test
    fun `no purchases clears premium`() = runTest {
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
                PurchasesResult(result(BillingClient.BillingResponseCode.OK), emptyList())

        buildRepository().restorePurchases()

        coVerify { dataStore.setPremium(false) }
    }

    @Test
    fun `a rejected acknowledgement schedules the retry worker`() = runTest {
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
                PurchasesResult(
                    result(BillingClient.BillingResponseCode.OK),
                    listOf(purchase(Purchase.PurchaseState.PURCHASED, acknowledged = false))
                )
        coEvery { billingClient.acknowledgePurchase(any()) } returns
                result(BillingClient.BillingResponseCode.NETWORK_ERROR)

        val acknowledged = buildRepository().acknowledgePendingPurchases()

        assertFalse("A rejected acknowledgement must not report success", acknowledged)
        verify { scheduler.scheduleRetry() }
    }

    @Test
    fun `an accepted acknowledgement reports success and schedules nothing`() = runTest {
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
                PurchasesResult(
                    result(BillingClient.BillingResponseCode.OK),
                    listOf(purchase(Purchase.PurchaseState.PURCHASED, acknowledged = false))
                )
        coEvery { billingClient.acknowledgePurchase(any()) } returns
                result(BillingClient.BillingResponseCode.OK)

        assertTrue(buildRepository().acknowledgePendingPurchases())
        verify(exactly = 0) { scheduler.scheduleRetry() }
    }

    @Test
    fun `restorePurchases is throttled so returning to the foreground is cheap`() = runTest {
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
                PurchasesResult(result(BillingClient.BillingResponseCode.OK), emptyList())

        val repository = buildRepository()
        repository.restorePurchases()
        repository.restorePurchases()

        coVerify(exactly = 1) { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) }
    }
}
