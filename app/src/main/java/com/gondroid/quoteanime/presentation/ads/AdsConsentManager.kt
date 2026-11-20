package com.gondroid.quoteanime.presentation.ads

import android.app.Activity
import android.content.Context
import com.gondroid.quoteanime.BuildConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google's User Messaging Platform (UMP): asks for ad consent where the law requires it (EEA,
 * UK, Switzerland and some US states) and starts the ads SDK only once ads may be requested.
 *
 * Elsewhere Google answers that no form is needed and ads start right away. The form, its
 * texts and the regions it covers are configured in AdMob → Privacy & messaging, not here.
 */
@Singleton
class AdsConsentManager @Inject constructor(
    @ApplicationContext context: Context,
    private val adsInitializer: AdsInitializer
) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gathering = AtomicBoolean(false)

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /** True when Settings must offer a way to change the ad consent (a UMP policy requirement). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    init {
        refreshPrivacyOptions()
    }

    /**
     * Starts the ads SDK if a consent saved in an earlier session already allows it. Safe from
     * `Application.onCreate`: it only reads what UMP stored, it never shows anything.
     */
    fun startAdsIfAllowed() {
        // Debug-only switch (always true in release): the SDK never starts, so the banner takes
        // no space and the interstitial never loads — sharing goes straight through.
        if (!BuildConfig.SHOW_ADS) return
        if (consentInformation.canRequestAds()) {
            scope.launch { adsInitializer.initialize() }
        }
    }

    /**
     * Refreshes the consent status and shows the form if this user still has to answer it.
     * Called from every `MainActivity.onCreate`, as Google recommends: the status can change
     * (a new region, a new form version) between launches.
     */
    fun gatherConsent(activity: Activity) {
        if (!BuildConfig.SHOW_ADS) return
        startAdsIfAllowed()
        if (!gathering.compareAndSet(false, true)) return
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    gathering.set(false)
                    refreshPrivacyOptions()
                    startAdsIfAllowed()
                }
            },
            { _ ->
                // No network or a misconfigured form: keep whatever an earlier session allowed.
                gathering.set(false)
                startAdsIfAllowed()
            }
        )
    }

    /** Opens the form again so the user can change their choice (Settings → ad privacy). */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
            refreshPrivacyOptions()
            startAdsIfAllowed()
        }
    }

    private fun refreshPrivacyOptions() {
        _privacyOptionsRequired.value = BuildConfig.SHOW_ADS && consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }
}

/** Lets composables that have no ViewModel (the banner, the Settings rows) reach the ads singletons. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun adsInitializer(): AdsInitializer
    fun adsConsentManager(): AdsConsentManager
}
