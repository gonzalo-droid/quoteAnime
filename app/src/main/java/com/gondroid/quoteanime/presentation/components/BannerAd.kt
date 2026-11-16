package com.gondroid.quoteanime.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.gondroid.quoteanime.BuildConfig
import com.gondroid.quoteanime.presentation.ads.AdsEntryPoint
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dagger.hilt.android.EntryPointAccessors

/** Live unit in release, Google's public test unit in debug — see `app/build.gradle.kts`. */
private val BANNER_AD_UNIT_ID = BuildConfig.AD_UNIT_BANNER

/**
 * Loads nothing until the ads SDK is started, which only happens once the user's ad consent
 * allows it (see AdsConsentManager). Before that, and in previews, it takes no space.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return
    val context = LocalContext.current
    val readiness = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java)
            .adsInitializer().isReady
    }
    val isReady by readiness.collectAsState()
    if (!isReady) return

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
