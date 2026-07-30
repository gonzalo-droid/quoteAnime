package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.domain.model.HabitTemplate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed interface HabitEditorError {
    data object BlankTitle : HabitEditorError
    data object InvalidDateRange : HabitEditorError
    data class LimitReached(val max: Int) : HabitEditorError
}

data class HabitEditorUiState(
    val habitId: String? = null,
    val templates: List<HabitTemplate> = emptyList(),
    val title: String = "",
    val description: String = "",
    val iconKey: String = "dumbbell",
    val templateId: String? = null,
    val colorIndex: Int = 0,
    /** Theme key carried by the selected themed template, if any; persisted on the habit
     *  as [com.gondroid.quoteanime.domain.model.Habit.coverAnimeSlug]. Resolved to a bundled
     *  cover image via [HabitThemeImages]. */
    val themeKey: String? = null,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val reminderDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val error: HabitEditorError? = null,
    val isSaved: Boolean = false
) {
    val isEditing: Boolean get() = habitId != null
}
