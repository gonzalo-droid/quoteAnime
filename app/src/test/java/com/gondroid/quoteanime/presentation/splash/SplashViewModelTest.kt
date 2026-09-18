package com.gondroid.quoteanime.presentation.splash

import androidx.lifecycle.SavedStateHandle
import com.gondroid.quoteanime.domain.usecase.GetOnboardingCompletedUseCase
import com.gondroid.quoteanime.presentation.navigation.Screen
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Scenarios covered:
 *  - Normal launch: the intro plays, then routes by the onboarding flag
 *  - Deep-link launch, onboarding complete: straight to Home, no intro
 *  - Deep-link launch, onboarding incomplete: the onboarding is still shown first
 */
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(onboardingCompleted: Boolean, skipIntro: Boolean): SplashViewModel {
        val getOnboardingCompleted = mockk<GetOnboardingCompletedUseCase>()
        every { getOnboardingCompleted() } returns flowOf(onboardingCompleted)
        val handle = if (skipIntro) {
            SavedStateHandle(mapOf(Screen.Splash.ARG_SKIP_INTRO to true))
        } else {
            SavedStateHandle()
        }
        return SplashViewModel(handle, getOnboardingCompleted)
    }

    @Test
    fun `given a normal launch, when the intro ends, then it routes by the onboarding flag`() = runTest {
        val viewModel = buildViewModel(onboardingCompleted = true, skipIntro = false)

        runCurrent()
        assertNull(viewModel.destination.value)

        advanceTimeBy(SplashViewModel.INTRO_DURATION_MS + 1)
        assertEquals(SplashDestination.Home, viewModel.destination.value)
    }

    @Test
    fun `given a pending deep link and a completed onboarding, when launched, then it goes straight to Home`() = runTest {
        val viewModel = buildViewModel(onboardingCompleted = true, skipIntro = true)

        runCurrent()

        assertEquals(SplashDestination.Home, viewModel.destination.value)
    }

    @Test
    fun `given a pending deep link and an incomplete onboarding, when launched, then the onboarding is shown first`() = runTest {
        val viewModel = buildViewModel(onboardingCompleted = false, skipIntro = true)

        runCurrent()

        assertEquals(SplashDestination.Onboarding, viewModel.destination.value)
    }
}
