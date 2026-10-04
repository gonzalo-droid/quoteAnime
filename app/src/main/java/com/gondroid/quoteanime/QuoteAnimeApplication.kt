package com.gondroid.quoteanime

import android.app.Application
import android.content.res.Configuration as ResConfiguration
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.RestorePurchasesUseCase
import com.gondroid.quoteanime.notification.RoutineWidgetScheduler
import com.gondroid.quoteanime.notification.WidgetScheduler
import com.gondroid.quoteanime.presentation.ads.AdsInitializer
import com.gondroid.quoteanime.widget.HabitWidget
import com.gondroid.quoteanime.widget.QuoteWidget
import com.gondroid.quoteanime.widget.RoutineSummaryWidget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class QuoteAnimeApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var widgetScheduler: WidgetScheduler
    @Inject lateinit var routineWidgetScheduler: RoutineWidgetScheduler
    @Inject lateinit var getUserPreferences: GetUserPreferencesUseCase
    @Inject lateinit var restorePurchases: RestorePurchasesUseCase
    @Inject lateinit var adsInitializer: AdsInitializer

    /** Language the widgets were last drawn in — see [onConfigurationChanged]. */
    private var widgetLocales: String = ""

    override fun onCreate() {
        super.onCreate()
        widgetLocales = resources.configuration.locales.toLanguageTags()
        initializeAds()
        scheduleWidgetUpdates()
        routineWidgetScheduler.scheduleDailyRefresh()
        syncPremiumEntitlement()
    }

    /** Off the main thread on purpose — the SDK's binder work there caused ANRs. */
    private fun initializeAds() {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            adsInitializer.initialize()
        }
    }

    /** Catches a subscription cancelled/expired outside the app — see [RestorePurchasesUseCase]. */
    private fun syncPremiumEntitlement() {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            restorePurchases()
        }
    }

    private fun scheduleWidgetUpdates() {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val prefs = getUserPreferences().first()
            widgetScheduler.schedule(prefs.widgetUpdateTimesPerDay)
            scope.cancel()
        }
    }

    /**
     * A home-screen widget keeps the views it was last drawn with, so after a language change
     * (from Settings > Language or the system's per-app language page) it would stay in the old
     * language until its next refresh. Redraw the three right away; their texts are resolved
     * while composing, so redrawing is enough — no quote is fetched.
     */
    override fun onConfigurationChanged(newConfig: ResConfiguration) {
        super.onConfigurationChanged(newConfig)
        val locales = newConfig.locales.toLanguageTags()
        if (locales == widgetLocales) return
        widgetLocales = locales
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            QuoteWidget().updateAll(this@QuoteAnimeApplication)
            RoutineSummaryWidget().updateAll(this@QuoteAnimeApplication)
            HabitWidget().updateAll(this@QuoteAnimeApplication)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
