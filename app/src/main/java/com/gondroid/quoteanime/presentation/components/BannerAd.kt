package com.gondroid.quoteanime.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.gondroid.quoteanime.BuildConfig
import com.google.android.gms.ads.AdView

// Reemplaza con tu Ad Unit ID real cuando publiques la app
/** Live unit in release, Google's public test unit in debug — see `app/build.gradle.kts`. */
private val BANNER_AD_UNIT_ID = BuildConfig.AD_UNIT_BANNER

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
