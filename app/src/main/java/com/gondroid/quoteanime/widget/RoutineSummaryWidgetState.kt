package com.gondroid.quoteanime.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

data class RoutineWidgetHabitSummary(
    val id: String,
    val title: String,
    val colorIndex: Int,
    val streakCurrent: Int,
    val completedToday: Boolean
)

/**
 * Preferences DataStore (Glance's [androidx.glance.state.PreferencesGlanceStateDefinition])
 * only stores primitives — no way to persist a `List<RoutineWidgetHabitSummary>` directly,
 * and the project has no JSON library, so the list is flattened into one delimited string
 * instead of adding a new dependency for this. The separators are ASCII sequences a habit
 * title could plausibly never contain, kept printable (rather than control characters) so
 * the encoded value stays easy to inspect while debugging.
 */
object RoutineSummaryWidgetState {
    val HABITS_DATA = stringPreferencesKey("routine_widget_habits_data")
    val IS_LOADING = booleanPreferencesKey("routine_widget_is_loading")
    val HAS_ERROR = booleanPreferencesKey("routine_widget_has_error")

    private const val FIELD_SEPARATOR = "|~|"
    private const val ENTRY_SEPARATOR = "~|~|~"

    fun encode(habits: List<RoutineWidgetHabitSummary>): String =
        habits.joinToString(ENTRY_SEPARATOR) { habit ->
            listOf(
                habit.id,
                habit.title,
                habit.colorIndex,
                habit.streakCurrent,
                habit.completedToday
            ).joinToString(FIELD_SEPARATOR)
        }

    fun decode(data: String): List<RoutineWidgetHabitSummary> {
        if (data.isBlank()) return emptyList()
        return data.split(ENTRY_SEPARATOR).mapNotNull { entry ->
            val parts = entry.split(FIELD_SEPARATOR)
            if (parts.size != 5) return@mapNotNull null
            RoutineWidgetHabitSummary(
                id = parts[0],
                title = parts[1],
                colorIndex = parts[2].toIntOrNull() ?: 0,
                streakCurrent = parts[3].toIntOrNull() ?: 0,
                completedToday = parts[4].toBoolean()
            )
        }
    }
}
