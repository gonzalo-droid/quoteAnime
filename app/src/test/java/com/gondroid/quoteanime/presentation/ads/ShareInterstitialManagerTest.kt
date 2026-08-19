package com.gondroid.quoteanime.presentation.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * "Sin anuncios" is a paid promise, and the interstitial used to ignore it entirely — banners
 * were gated but this one wasn't, so a paying user still got a full-screen ad every third share.
 */
class ShareInterstitialManagerTest {

    private val observePremiumStatus = mockk<ObservePremiumStatusUseCase>()

    private fun manager(isPremium: Boolean): ShareInterstitialManager {
        every { observePremiumStatus() } returns MutableStateFlow(isPremium)
        return ShareInterstitialManager(observePremiumStatus)
    }

    @Before
    fun setup() {
        mockkStatic(InterstitialAd::class)
        every {
            InterstitialAd.load(any(), any(), any<AdRequest>(), any<InterstitialAdLoadCallback>())
        } just runs
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `premium never requests an interstitial`() {
        manager(isPremium = true).preload(mockk<Context>(relaxed = true))

        verify(exactly = 0) {
            InterstitialAd.load(any(), any(), any<AdRequest>(), any<InterstitialAdLoadCallback>())
        }
    }

    @Test
    fun `premium shares go straight through`() {
        var proceeded = false

        // Third share: exactly where a free user would be interrupted.
        val manager = manager(isPremium = true)
        repeat(3) { manager.onShareRequested(mockk<Activity>(relaxed = true)) { proceeded = true } }

        assertTrue("A premium share must never wait on an ad", proceeded)
    }

    @Test
    fun `free users still get the ad preloaded`() {
        manager(isPremium = false).preload(mockk<Context>(relaxed = true))

        verify(exactly = 1) {
            InterstitialAd.load(any(), any(), any<AdRequest>(), any<InterstitialAdLoadCallback>())
        }
    }
}
