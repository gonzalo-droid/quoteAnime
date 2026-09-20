package com.gondroid.quoteanime.presentation.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/** What an ad caller needs: a way to wait until the SDK is usable. */
interface AdsReadiness {
    suspend fun awaitReady()
}

/**
 * Initializes the AdMob SDK off the main thread.
 *
 * `MobileAds.initialize` registers a BroadcastReceiver, a binder call to the system server that
 * normally takes a millisecond. On a loaded device it can block far longer, and doing it from
 * `Application.onCreate` produced ANRs ("Input dispatching timed out … waited 15120ms").
 */
@Singleton
class AdsInitializer @Inject constructor(
    @ApplicationContext private val context: Context
) : AdsReadiness {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val started = AtomicBoolean(false)

    /** Safe to call more than once: only the first call reaches the SDK. Never runs on main. */
    suspend fun initialize() {
        if (!started.compareAndSet(false, true)) return
        withContext(Dispatchers.IO) {
            MobileAds.initialize(context) { _isReady.value = true }
        }
    }

    override suspend fun awaitReady() {
        isReady.first { it }
    }
}
