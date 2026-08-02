package com.gondroid.quoteanime.presentation.subscription

import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import com.gondroid.quoteanime.domain.usecase.SetPremiumStatusUseCase
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
 *  - Subscribing (mock, pre-billing) sets premium to true
 *  - Removing premium (QA-only) sets premium to false
 */
class PaywallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var observePremiumStatus: ObservePremiumStatusUseCase
    private lateinit var setPremiumStatus: SetPremiumStatusUseCase

    private fun buildViewModel() = PaywallViewModel(observePremiumStatus, setPremiumStatus)

    @Before
    fun setup() {
        observePremiumStatus = mockk()
        setPremiumStatus = mockk()
        coJustRun { setPremiumStatus(any()) }
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
    fun `given the subscribe action, when tapped, then the entitlement flag is set to true`() = runTest {
        every { observePremiumStatus() } returns flowOf(false)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onSubscribe()
        advanceUntilIdle()

        coVerify(exactly = 1) { setPremiumStatus(true) }
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
}
