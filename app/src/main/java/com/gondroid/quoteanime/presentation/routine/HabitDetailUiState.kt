package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.StreakState
import java.time.LocalDate
import java.time.YearMonth

enum class HabitDetailMessage {
    FutureDayNotAllowed,
    OutsideHabitRange
}

data class HabitDetailUiState(
    val habit: Habit? = null,
    val completions: Set<LocalDate> = emptySet(),
    val streak: StreakState = StreakState(),
    val visibleMonth: YearMonth = YearMonth.now(),
    /** Last date tapped, in either the heatmap or the calendar — echoed back in the callout. */
    val selectedDate: LocalDate? = null,
    val today: LocalDate = LocalDate.now(),
    val message: HabitDetailMessage? = null,
    /** Set once archiving completes, so the screen can navigate back. */
    val isArchived: Boolean = false,
    /** Set once deletion completes, so the screen can navigate back. */
    val isDeleted: Boolean = false,
    val isLoading: Boolean = true
)
