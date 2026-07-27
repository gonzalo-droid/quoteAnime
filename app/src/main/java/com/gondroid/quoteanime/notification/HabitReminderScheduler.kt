package com.gondroid.quoteanime.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.worker.HabitReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One uniquely named chained work item per habit. Exact alarms are deliberately
 * avoided: the same tolerance already accepted for quote notifications applies.
 */
@Singleton
class HabitReminderScheduler @Inject constructor(
    @ApplicationContext context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Deliberately self-replacing: [HabitReminderWorker] calls this on itself at the end of
     * its own `doWork()` to chain the next occurrence. `ExistingWorkPolicy.REPLACE` cancels
     * the old unique work item atomically before enqueueing the new one, so a running work
     * item replacing itself under the same unique name is safe, not a bug — don't "fix" it.
     */
    fun schedule(habit: Habit) {
        val reminderTime = habit.reminderTime
        if (habit.isArchived || reminderTime == null || habit.reminderDays.isEmpty()) {
            cancel(habit.id)
            return
        }

        val now = LocalDateTime.now()
        val next = NextReminderCalculator.nextOccurrence(now, reminderTime, habit.reminderDays)
            ?: return
        if (habit.endDate != null && next.toLocalDate().isAfter(habit.endDate)) {
            cancel(habit.id)
            return
        }

        val delayMillis = Duration.between(now, next).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(HabitReminderWorker.KEY_HABIT_ID, habit.id).build())
            .build()

        workManager.enqueueUniqueWork(workName(habit.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(habitId: String) {
        workManager.cancelUniqueWork(workName(habitId))
    }

    private fun workName(habitId: String) = "$WORK_PREFIX$habitId"

    companion object {
        private const val WORK_PREFIX = "habit_reminder_"
    }
}
