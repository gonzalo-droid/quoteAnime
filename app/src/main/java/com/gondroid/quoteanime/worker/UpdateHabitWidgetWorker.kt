package com.gondroid.quoteanime.worker

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.CalculateStreakUseCase
import com.gondroid.quoteanime.widget.HabitWidget
import com.gondroid.quoteanime.widget.HabitWidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

/**
 * Refreshes either every [HabitWidget] instance (periodic/reboot — no input data) or a
 * single one right after it's configured (input data carries [KEY_APP_WIDGET_ID]), since
 * at that point only the newly-added instance has a habit bound to it — refreshing "all"
 * would be a no-op for it anyway before this runs, but scoping avoids redundant work on
 * every other already-up-to-date instance.
 */
@HiltWorker
class UpdateHabitWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val clock: Clock
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val targetAppWidgetId = workerParams.inputData.getInt(KEY_APP_WIDGET_ID, -1)

        val glanceIds = if (targetAppWidgetId != -1) {
            listOf(manager.getGlanceIdBy(targetAppWidgetId))
        } else {
            manager.getGlanceIds(HabitWidget::class.java)
        }
        if (glanceIds.isEmpty()) return Result.success()

        glanceIds.forEach { glanceId -> refreshInstance(glanceId) }
        return Result.success()
    }

    private suspend fun refreshInstance(glanceId: GlanceId) {
        val currentPrefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val habitId = currentPrefs[HabitWidgetState.HABIT_ID] ?: return

        val habit = runCatching { habitRepository.getHabit(habitId) }.getOrNull()
        if (habit == null) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[HabitWidgetState.IS_LOADING] = false
                    this[HabitWidgetState.HAS_ERROR] = true
                }
            }
            HabitWidget().update(context, glanceId)
            return
        }

        val today = LocalDate.now(clock)
        val dates = runCatching { habitRepository.getCompletions(habitId).first() }.getOrDefault(emptyList())
        val streak = calculateStreak(dates, today)
        val windowStart = today.minusWeeks(9)
        val visibleCompletions = dates.filter { !it.isBefore(windowStart) }.toSet()

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[HabitWidgetState.TITLE] = habit.title
                this[HabitWidgetState.COLOR_INDEX] = habit.colorIndex
                this[HabitWidgetState.COMPLETIONS_DATA] = HabitWidgetState.encodeCompletions(visibleCompletions)
                this[HabitWidgetState.STREAK_CURRENT] = streak.current
                this[HabitWidgetState.COMPLETED_TODAY] = streak.completedToday
                this[HabitWidgetState.IS_LOADING] = false
                this[HabitWidgetState.HAS_ERROR] = false
            }
        }
        HabitWidget().update(context, glanceId)
    }

    companion object {
        const val KEY_APP_WIDGET_ID = "app_widget_id"
    }
}
