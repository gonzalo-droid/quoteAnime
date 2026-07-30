package com.gondroid.quoteanime.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where routine events are named, so the dashboards stay stable
 * even if call sites move.
 */
@Singleton
class RoutineAnalytics @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    fun trackTabOpened() = analytics.logEvent("routine_tab_opened", Bundle())

    fun trackHabitDetailOpened() = analytics.logEvent("habit_detail_opened", Bundle())

    fun trackHabitCreated(
        templateId: String?,
        isCustom: Boolean,
        hasReminder: Boolean,
        hasEndDate: Boolean
    ) = analytics.logEvent(
        "habit_created",
        Bundle().apply {
            putString("template_id", templateId ?: "custom")
            putBoolean("is_custom", isCustom)
            putBoolean("has_reminder", hasReminder)
            putBoolean("has_end_date", hasEndDate)
        }
    )

    fun trackHabitCompleted(habitId: String, isRetroactive: Boolean, source: String) =
        analytics.logEvent(
            "habit_completed",
            Bundle().apply {
                putString("habit_id", habitId)
                putBoolean("is_retroactive", isRetroactive)
                putString("source", source)
            }
        )

    fun trackHabitArchived(daysActive: Long) = analytics.logEvent(
        "habit_archived",
        Bundle().apply { putLong("days_active", daysActive) }
    )

    fun trackStreakMilestone(days: Int) = analytics.logEvent(
        "streak_milestone",
        Bundle().apply { putInt("days", days) }
    )

    fun trackStreakBroken(previousStreak: Int) = analytics.logEvent(
        "streak_broken",
        Bundle().apply { putInt("previous_streak", previousStreak) }
    )

    companion object {
        const val SOURCE_APP = "app"
        const val SOURCE_NOTIFICATION = "notification"
    }
}
