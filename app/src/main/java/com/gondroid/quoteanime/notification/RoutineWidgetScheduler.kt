package com.gondroid.quoteanime.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.worker.UpdateHabitWidgetWorker
import com.gondroid.quoteanime.worker.UpdateRoutineSummaryWidgetWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unlike [WidgetScheduler] (quote content changes on its own schedule), nothing changes a
 * habit widget's data except the user marking a habit in the app — so the only refresh
 * that matters day-to-day is the immediate one fired right after a toggle. The daily
 * periodic one exists purely so the heatmap's "today" column and streak still roll over
 * correctly on a day the user never opens the app at all.
 */
@Singleton
class RoutineWidgetScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SUMMARY_WORK_NAME = "routine_summary_widget_daily_refresh"
        private const val HABIT_WORK_NAME = "habit_widget_daily_refresh"
    }

    private val workManager = WorkManager.getInstance(context)

    fun scheduleDailyRefresh() {
        workManager.enqueueUniquePeriodicWork(
            SUMMARY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateRoutineSummaryWidgetWorker>(24, TimeUnit.HOURS).build()
        )
        workManager.enqueueUniquePeriodicWork(
            HABIT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateHabitWidgetWorker>(24, TimeUnit.HOURS).build()
        )
    }

    /** Called right after a habit is toggled in the app so both widget types reflect it
     *  immediately instead of waiting for the next daily refresh. */
    fun triggerImmediateUpdate() {
        workManager.enqueue(OneTimeWorkRequestBuilder<UpdateRoutineSummaryWidgetWorker>().build())
        workManager.enqueue(OneTimeWorkRequestBuilder<UpdateHabitWidgetWorker>().build())
    }
}
