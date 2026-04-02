package com.gondroid.quoteanime.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

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

            // Check whether the current time is within the notification window
            val cal = Calendar.getInstance()
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startMinutes = preferences.notificationStartHour * 60 + preferences.notificationStartMinute
            val endMinutes   = preferences.notificationEndHour   * 60 + preferences.notificationEndMinute
            if (nowMinutes < startMinutes || nowMinutes > endMinutes) {
                return Result.success() // Outside allowed window — skip silently
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return Result.failure()
            }

            val quote = getRandomQuote(preferences.selectedCategoryIds)
                ?: return Result.retry()

            notificationHelper.showQuoteNotification(quote)
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
