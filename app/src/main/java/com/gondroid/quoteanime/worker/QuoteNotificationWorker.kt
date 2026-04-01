package com.gondroid.quoteanime.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class QuoteNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val preferences = getUserPreferences().first()
            val quote = getRandomQuote(preferences.selectedCategoryIds)
                ?: return Result.retry()  // Sin frases disponibles, reintentar más tarde

            notificationHelper.showQuoteNotification(quote)
            Result.success()
        }.getOrElse {
            // Error de red u otro fallo transitorio → WorkManager reintentará
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
