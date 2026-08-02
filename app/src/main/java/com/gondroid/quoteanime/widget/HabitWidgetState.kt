package com.gondroid.quoteanime.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate

/**
 * Per-instance state: [PreferencesGlanceStateDefinition][androidx.glance.state.PreferencesGlanceStateDefinition]
 * scopes its store to each [androidx.glance.GlanceId], so every [HabitWidget] placed on the
 * home screen keeps its own bound habit and progress independently.
 */
object HabitWidgetState {
    val HABIT_ID = stringPreferencesKey("habit_widget_habit_id")
    val TITLE = stringPreferencesKey("habit_widget_title")
    val COLOR_INDEX = intPreferencesKey("habit_widget_color_index")
    val COMPLETIONS_DATA = stringPreferencesKey("habit_widget_completions")
    val STREAK_CURRENT = intPreferencesKey("habit_widget_streak_current")
    val COMPLETED_TODAY = booleanPreferencesKey("habit_widget_completed_today")
    val IS_LOADING = booleanPreferencesKey("habit_widget_is_loading")
    val HAS_ERROR = booleanPreferencesKey("habit_widget_has_error")

    private const val DATE_SEPARATOR = ","

    fun encodeCompletions(dates: Set<LocalDate>): String = dates.joinToString(DATE_SEPARATOR)

    fun decodeCompletions(data: String): Set<LocalDate> {
        if (data.isBlank()) return emptySet()
        return data.split(DATE_SEPARATOR).mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
    }
}
