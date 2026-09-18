package com.gondroid.quoteanime.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateUserPreferencesUseCase
import com.gondroid.quoteanime.notification.NotificationHelper
import com.gondroid.quoteanime.notification.NotificationScheduler
import com.gondroid.quoteanime.notification.QuoteNotificationSlotCalculator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * Shows one quote notification and chains the next slot through [NotificationScheduler].
 */
@HiltWorker
class QuoteNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
    private val updateUserPreferences: UpdateUserPreferencesUseCase,
    private val notificationHelper: NotificationHelper,
    private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(context, workerParams) {

    private enum class Outcome { DELIVERED, SKIPPED, NO_PERMISSION, TRANSIENT_ERROR }

    override suspend fun doWork(): Result {
        val preferences = attempt { getUserPreferences().first() }
            ?: return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()

        val outcome = attempt { deliver(preferences) } ?: Outcome.TRANSIENT_ERROR
        if (outcome == Outcome.TRANSIENT_ERROR && runAttemptCount < MAX_RETRIES) return Result.retry()

        // Terminal outcome: chain the next slot. Must be the last step (REPLACE cancels this very
        // work item) and must also run on failure, or a revoked permission would end the chain.
        // Re-read the preferences so a toggle made while this run was in flight wins — the user
        // may have just turned notifications off — and skip chaining if this run was cancelled.
        if (isStopped) return Result.failure()
        val latest = attempt { getUserPreferences().first() } ?: preferences
        notificationScheduler.schedule(latest)
        return when (outcome) {
            Outcome.DELIVERED, Outcome.SKIPPED -> Result.success()
            Outcome.NO_PERMISSION, Outcome.TRANSIENT_ERROR -> Result.failure()
        }
    }

    private suspend fun deliver(preferences: UserPreferences): Outcome {
        val inWindow = QuoteNotificationSlotCalculator.isWithinWindow(
            now = LocalTime.now(),
            start = LocalTime.of(preferences.notificationStartHour, preferences.notificationStartMinute),
            end = LocalTime.of(preferences.notificationEndHour, preferences.notificationEndMinute),
            graceMinutes = LATE_GRACE_MINUTES
        )
        // A run delayed far past its slot (e.g. no network overnight) is dropped, not sent at 3 AM.
        if (!inWindow) return Outcome.SKIPPED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Outcome.NO_PERMISSION
        }

        // The Settings anime selection (anime names, matched on `Quote.anime`; stale = all).
        val quote = getRandomQuote(
            animes = preferences.selectedCategoryIds,
            excludeId = preferences.lastNotificationQuoteId.ifEmpty { null }
        ) ?: return Outcome.TRANSIENT_ERROR

        notificationHelper.showQuoteNotification(quote)
        updateUserPreferences.setLastNotificationQuoteId(quote.id)
        return Outcome.DELIVERED
    }

    /**
     * Like `runCatching`, but rethrows cancellation. Swallowing it here would let a worker that
     * was cancelled because the user turned notifications off go on to schedule the next slot.
     */
    private suspend fun <T> attempt(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private companion object {
        const val MAX_RETRIES = 3

        /** WorkManager fires late, never early; this keeps the window's final slot from being dropped. */
        const val LATE_GRACE_MINUTES = 30
    }
}
