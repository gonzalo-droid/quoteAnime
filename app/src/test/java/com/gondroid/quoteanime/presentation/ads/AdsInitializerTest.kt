package com.gondroid.quoteanime.presentation.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The AdMob SDK registers a BroadcastReceiver on the thread that initializes it. Doing that on
 * the main thread produced ANRs ("Input dispatching timed out") on a loaded device, so the
 * initialization moved off it — and the interstitial now waits for it instead of racing it.
 *
 * Scenarios covered:
 *  - The SDK is initialized once even if initialize() is called repeatedly
 *  - isReady stays false until the SDK calls back, and flips afterwards
 *  - awaitReady() resumes once the SDK has finished
 */
class AdsInitializerTest {

    private val context = mockk<Context>(relaxed = true)
    private var pendingListener: OnInitializationCompleteListener? = null

    @Before
    fun setUp() {
        mockkStatic(MobileAds::class)
        every { MobileAds.initialize(any(), any<OnInitializationCompleteListener>()) } answers {
            pendingListener = secondArg()
        }
    }

    @After
    fun tearDown() {
        pendingListener = null
        unmockkAll()
    }

    private fun finishSdkInitialization() {
        pendingListener?.onInitializationComplete(mockk<InitializationStatus>(relaxed = true))
    }

    @Test
    fun `given initialize is called three times, then the SDK is initialized once`() = runTest {
        val initializer = AdsInitializer(context)

        repeat(3) { initializer.initialize() }

        verify(exactly = 1) { MobileAds.initialize(any(), any<OnInitializationCompleteListener>()) }
    }

    @Test
    fun `given the SDK has not called back, then ads are not ready`() = runTest {
        val initializer = AdsInitializer(context)

        initializer.initialize()

        assertFalse(initializer.isReady.value)
    }

    @Test
    fun `given the SDK finished, then ads are ready`() = runTest {
        val initializer = AdsInitializer(context)

        initializer.initialize()
        finishSdkInitialization()

        assertTrue(initializer.isReady.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a caller waiting for ads, when the SDK finishes, then it resumes`() = runTest {
        val initializer = AdsInitializer(context)
        initializer.initialize()

        finishSdkInitialization()
        withTimeout(1_000) { initializer.awaitReady() }

        assertTrue(initializer.isReady.value)
    }
}
