package com.gondroid.quoteanime.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import com.gondroid.quoteanime.notification.HabitReminderScheduler
import com.gondroid.quoteanime.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Shows one habit reminder and immediately schedules the next occurrence:
 * WorkManager has no weekday-aware periodic work, so the chain is rebuilt each run.
 */
@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: HabitRepository,
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val notificationHelper: NotificationHelper,
    private val scheduler: HabitReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(KEY_HABIT_ID) ?: return Result.failure()
        val habit = repository.getHabit(habitId) ?: return Result.success()
        if (habit.isArchived || habit.reminderTime == null) return Result.success()

        val today = LocalDate.now()
        val shouldNotify = habit.isActiveOn(today) && !repository.isCompleted(habitId, today)

        // Mirrors QuoteNotificationWorker's runtime check — API 33+ requires the grant.
        val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (shouldNotify && hasNotificationPermission) {
            val quote = runCatching { getRandomQuote(emptySet()) }.getOrNull()
            notificationHelper.showHabitReminder(
                habitId = habit.id,
                habitTitle = habit.title,
                quoteText = quote?.quote.orEmpty()
            )
        }

        scheduler.schedule(habit)
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
    }
}
