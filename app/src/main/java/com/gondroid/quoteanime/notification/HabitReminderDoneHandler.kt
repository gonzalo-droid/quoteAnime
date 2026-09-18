package com.gondroid.quoteanime.notification

import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.domain.repository.HabitRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * What the reminder's "Done" action does, kept out of [HabitReminderReceiver] so it can be
 * unit-tested without a broadcast.
 */
class HabitReminderDoneHandler @Inject constructor(
    private val habitRepository: HabitRepository,
    private val analytics: RoutineAnalytics,
    private val routineWidgetScheduler: RoutineWidgetScheduler,
    private val clock: Clock
) {
    suspend fun markDone(habitId: String) {
        val today = LocalDate.now(clock)
        val habit = habitRepository.getHabit(habitId)
        // Guard against a stale notification lingering past the habit's end date
        // (or, in theory, before its start date) — mirrors the validation
        // ToggleHabitCompletionUseCase performs for app-driven toggles.
        if (habit?.isActiveOn(today) == true) {
            // Idempotent "mark done", not a toggle: the habit may already be
            // completed if the user marked it from the app before tapping this action.
            if (!habitRepository.isCompleted(habitId, today)) {
                habitRepository.setCompletion(habitId, today, true)
                analytics.trackHabitCompleted(
                    habitId = habitId,
                    isRetroactive = false,
                    source = RoutineAnalytics.SOURCE_NOTIFICATION
                )
            }
        }
        // The app is usually not on screen when "Done" is tapped, so nothing else would
        // refresh the Mi Rutina widgets: without this they keep showing today unmarked
        // until the next daily refresh. Same immediate update the in-app toggles fire.
        routineWidgetScheduler.triggerImmediateUpdate()
    }
}
