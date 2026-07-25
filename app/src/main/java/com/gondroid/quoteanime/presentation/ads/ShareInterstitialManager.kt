package com.gondroid.quoteanime.presentation.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

// Reemplaza con tu Ad Unit ID real antes de publicar
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1427341798923689/1127716194"

/** Número de shares entre cada aparición del interstitial. */
private const val SHARES_PER_AD = 3

/**
 * Singleton que gestiona el interstitial que aparece cada [SHARES_PER_AD] veces
 * que el usuario comparte una frase, tanto desde HomeScreen como desde CatalogScreen.
 *
 * Uso:
 *   1. Llamar [preload] en cuanto la pantalla esté visible (para que el ad esté listo).
 *   2. Llamar [onShareRequested] en el onClick del botón compartir.
 *      - Si toca mostrar el ad, lo muestra y llama [onProceed] al cerrar.
 *      - Si no toca, llama [onProceed] directamente.
 */
@Singleton
class ShareInterstitialManager @Inject constructor() {

    private var interstitialAd: InterstitialAd? = null
    private var shareCount = 0
    private var isLoading = false

    /** Pre-carga el siguiente interstitial. Llámalo al entrar a pantallas con Share. */
    fun preload(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Llamar cuando el usuario pulsa "Compartir".
     * Muestra el interstitial cada [SHARES_PER_AD] shares; el resto de veces
     * llama [onProceed] directamente sin interrumpir el flujo.
     */
    fun onShareRequested(activity: Activity, onProceed: () -> Unit) {
        shareCount++
        val ad = interstitialAd

        if (shareCount % SHARES_PER_AD == 0 && ad != null) {
            interstitialAd = null   // consumir — no mostrar dos veces el mismo ad

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onProceed()
                    preload(activity)   // precargar el siguiente
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    onProceed()         // no bloquear al usuario si falla
                    preload(activity)
                }
            }
            ad.show(activity)
        } else {
            onProceed()
        }
    }
}
