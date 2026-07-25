package com.gondroid.quoteanime

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.notification.WidgetScheduler
import com.google.android.gms.ads.MobileAds
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
    @Inject lateinit var getUserPreferences: GetUserPreferencesUseCase

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        scheduleWidgetUpdates()
    }

    private fun scheduleWidgetUpdates() {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val prefs = getUserPreferences().first()
            widgetScheduler.schedule(prefs.widgetUpdateTimesPerDay)
            scope.cancel()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
