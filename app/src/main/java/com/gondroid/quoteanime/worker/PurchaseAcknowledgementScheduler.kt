package com.gondroid.quoteanime.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queues the retry that closes Play's 72-hour acknowledgement window. WorkManager is the only
 * thing here that survives the process being killed — which is exactly the case that loses the
 * purchase: user pays, the acknowledgement fails, and they never reopen the app.
 */
@Singleton
class PurchaseAcknowledgementScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleRetry() {
        val request = OneTimeWorkRequestBuilder<AcknowledgePurchasesWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        // KEEP: a retry already queued (or running) is as good as this one, and re-enqueuing
        // would reset its backoff every time a sync finds the same pending purchase.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val WORK_NAME = "acknowledge_purchases"
    }
}
