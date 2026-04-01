package com.gondroid.quoteanime.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.worker.QuoteNotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val WORK_NAME = "quote_notification_work"
    }

    private val workManager = WorkManager.getInstance(context)

    fun schedule(preferences: UserPreferences) {
        if (!preferences.notificationsEnabled) {
            cancel()
            return
        }

        val initialDelay = calculateInitialDelay(
            hour = preferences.notificationHour,
            minute = preferences.notificationMinute
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<QuoteNotificationWorker>(
            preferences.notificationFrequency.intervalHours,
            TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    /**
     * Calcula el tiempo en ms hasta la próxima ocurrencia de [hour]:[minute].
     * Si la hora ya pasó hoy, devuelve el delay para mañana.
     */
    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
