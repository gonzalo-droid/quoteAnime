package com.gondroid.quoteanime.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.worker.QuoteNotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules quote notifications one slot at a time. Each [QuoteNotificationWorker] run chains
 * the next slot, so the user gets exactly the number per day they picked, spread across their
 * window (see [QuoteNotificationSlotCalculator]).
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WORK_NAME = "quote_notification_next"

        /**
         * Periodic work used by older versions. Cancelled on every [schedule] so existing installs
         * migrate on their first run: the old periodic fires once, runs the new worker, and that
         * worker's chaining call cancels it.
         */
        private const val LEGACY_PERIODIC_WORK_NAME = "quote_notification_work"
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Deliberately self-replacing, like [HabitReminderScheduler]: [QuoteNotificationWorker] calls
     * this at the end of its own `doWork()`. `ExistingWorkPolicy.REPLACE` cancels the running item
     * atomically, so the worker must call it last — and never when returning a retry.
     */
    fun schedule(preferences: UserPreferences) {
        workManager.cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
        if (!preferences.notificationsEnabled) { cancel(); return }

        val now = LocalDateTime.now()
        val next = QuoteNotificationSlotCalculator.nextSlot(
            from = now,
            start = LocalTime.of(preferences.notificationStartHour, preferences.notificationStartMinute),
            end = LocalTime.of(preferences.notificationEndHour, preferences.notificationEndMinute),
            timesPerDay = preferences.notificationFrequency
        )

        val request = OneTimeWorkRequestBuilder<QuoteNotificationWorker>()
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()

        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
        workManager.cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
    }
}
