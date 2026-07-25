package com.gondroid.quoteanime.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.worker.UpdateQuoteWidgetWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WORK_NAME = "widget_update_periodic_work"
        private const val MIN_INTERVAL_HOURS = 1L
    }

    private val workManager = WorkManager.getInstance(context)

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Schedule periodic updates. timesPerDay: 1–8 */
    fun schedule(timesPerDay: Int) {
        val intervalHours = (24L / timesPerDay.coerceIn(1, 8)).coerceAtLeast(MIN_INTERVAL_HOURS)
        val request = PeriodicWorkRequestBuilder<UpdateQuoteWidgetWorker>(
            intervalHours, TimeUnit.HOURS
        ).setConstraints(networkConstraint).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    /**
     * Run the worker immediately (one-time).
     * Called after changing widget size or any setting that should reflect right away.
     */
    fun triggerImmediateUpdate() {
        workManager.enqueue(
            OneTimeWorkRequestBuilder<UpdateQuoteWidgetWorker>()
                .setConstraints(networkConstraint)
                .build()
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }
}
