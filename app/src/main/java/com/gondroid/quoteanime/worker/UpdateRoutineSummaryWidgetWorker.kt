package com.gondroid.quoteanime.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import com.gondroid.quoteanime.widget.RoutineSummaryWidget
import com.gondroid.quoteanime.widget.RoutineSummaryWidgetState
import com.gondroid.quoteanime.widget.RoutineWidgetHabitSummary
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

@HiltWorker
class UpdateRoutineSummaryWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getActiveHabits: GetActiveHabitsUseCase,
    private val clock: Clock
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(RoutineSummaryWidget::class.java)
        if (glanceIds.isEmpty()) return Result.success()

        return runCatching {
            val habits = getActiveHabits(LocalDate.now(clock)).first()
            val summaries = habits.map { progress ->
                RoutineWidgetHabitSummary(
                    id = progress.habit.id,
                    title = progress.habit.title,
                    colorIndex = progress.habit.colorIndex,
                    streakCurrent = progress.streak.current,
                    completedToday = progress.streak.completedToday
                )
            }

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[RoutineSummaryWidgetState.HABITS_DATA] = RoutineSummaryWidgetState.encode(summaries)
                        this[RoutineSummaryWidgetState.IS_LOADING] = false
                        this[RoutineSummaryWidgetState.HAS_ERROR] = false
                    }
                }
                RoutineSummaryWidget().update(context, glanceId)
            }
            Result.success()
        }.getOrElse {
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[RoutineSummaryWidgetState.IS_LOADING] = false
                        this[RoutineSummaryWidgetState.HAS_ERROR] = true
                    }
                }
                RoutineSummaryWidget().update(context, glanceId)
            }
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
