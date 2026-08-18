package com.gondroid.quoteanime.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.usecase.AcknowledgePendingPurchasesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Retries the Play acknowledgement of a purchase whose first attempt failed. Worth this many
 * attempts because an unacknowledged purchase is auto-refunded after 72 h — the user would lose
 * a subscription they paid for without ever asking for a refund. With exponential backoff from
 * 30 s (WorkManager caps each wait at 5 h) [MAX_ATTEMPTS] spans well over a day.
 */
@HiltWorker
class AcknowledgePurchasesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val acknowledgePendingPurchases: AcknowledgePendingPurchasesUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val acknowledged = runCatching { acknowledgePendingPurchases() }.getOrDefault(false)
        return when {
            acknowledged -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 15
    }
}
