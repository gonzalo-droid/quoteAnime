package com.gondroid.quoteanime.presentation.subscription

import android.app.Activity
import com.gondroid.quoteanime.domain.model.SubscriptionOffer
import com.gondroid.quoteanime.domain.usecase.GetManageSubscriptionUrlUseCase
import com.gondroid.quoteanime.domain.usecase.GetSubscriptionOffersUseCase
import com.gondroid.quoteanime.domain.usecase.LaunchSubscriptionPurchaseUseCase
import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import com.gondroid.quoteanime.domain.usecase.ObservePurchaseEventsUseCase
import com.gondroid.quoteanime.domain.usecase.SetPremiumStatusUseCase
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Scenarios covered:
 *  - The current entitlement reaches the state
 *  - Subscribing launches the real Play purchase flow with the selected offer
 *  - Removing premium (QA-only) sets premium to false
 */
class PaywallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var observePremiumStatus: ObservePremiumStatusUseCase
    private lateinit var setPremiumStatus: SetPremiumStatusUseCase
    private lateinit var getSubscriptionOffers: GetSubscriptionOffersUseCase
    private lateinit var launchSubscriptionPurchase: LaunchSubscriptionPurchaseUseCase
    private lateinit var observePurchaseEvents: ObservePurchaseEventsUseCase
    private lateinit var getManageSubscriptionUrl: GetManageSubscriptionUrlUseCase

    private fun buildViewModel() = PaywallViewModel(
        observePremiumStatus,
        setPremiumStatus,
        getSubscriptionOffers,
        launchSubscriptionPurchase,
        observePurchaseEvents,
        getManageSubscriptionUrl
    )

    @Before
    fun setup() {
        observePremiumStatus = mockk()
        setPremiumStatus = mockk()
        getSubscriptionOffers = mockk()
        launchSubscriptionPurchase = mockk()
        observePurchaseEvents = mockk()
        getManageSubscriptionUrl = mockk()
        every { getManageSubscriptionUrl() } returns MANAGE_URL
        coJustRun { setPremiumStatus(any()) }
        coEvery { getSubscriptionOffers() } returns emptyList()
        every { observePurchaseEvents() } returns MutableSharedFlow()
        every { launchSubscriptionPurchase(any(), any()) } returns Unit
    }

    @Test
    fun `given a free user, when loaded, then isPremium is false`() = runTest {
        every { observePremiumStatus() } returns flowOf(false)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPremium)
    }

    @Test
    fun `given a premium user, when loaded, then isPremium is true`() = runTest {
        every { observePremiumStatus() } returns flowOf(true)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPremium)
    }

    @Test
    fun `given a selected offer, when subscribe is tapped, then the purchase flow launches with that offer`() = runTest {
        every { observePremiumStatus() } returns flowOf(false)
        val offer = SubscriptionOffer(
            offerToken = "token",
            basePlanId = "monthly",
            formattedPrice = "$2.99",
            billingPeriod = "P1M",
            freeTrialDays = null
        )
        coEvery { getSubscriptionOffers() } returns listOf(offer)
        val viewModel = buildViewModel()
        advanceUntilIdle()
        val activity = mockk<Activity>()

        viewModel.onSubscribe(activity)
        advanceUntilIdle()

        verify(exactly = 1) { launchSubscriptionPurchase(activity, offer) }
    }

    @Test
    fun `given the QA remove action, when tapped, then the entitlement flag is set to false`() = runTest {
        every { observePremiumStatus() } returns flowOf(true)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onRemovePremiumForTesting()
        advanceUntilIdle()

        coVerify(exactly = 1) { setPremiumStatus(false) }
    }

    private companion object {
        const val MANAGE_URL = "https://play.google.com/store/account/subscriptions"
    }
}
