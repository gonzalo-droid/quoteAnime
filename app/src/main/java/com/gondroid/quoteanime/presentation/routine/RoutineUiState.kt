package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import java.time.LocalDate

enum class RoutineMessage {
    FutureDayNotAllowed,
    OutsideHabitRange
}

enum class RoutineFilter {
    ACTIVE,
    ARCHIVED
}

data class RoutineUiState(
    /** The list for whichever [filter] is currently selected. */
    val habits: List<HabitWithProgress> = emptyList(),
    /** Always the active count, regardless of [filter] — the habit limit is never about
     *  how many archived habits exist, so this keeps [canAddHabit] correct while browsing
     *  the archived tab too. */
    val activeCount: Int = 0,
    val globalStreak: StreakState = StreakState(),
    val isLoading: Boolean = true,
    val maxHabits: Int = 0,
    val message: RoutineMessage? = null,
    val showIntro: Boolean = false,
    val filter: RoutineFilter = RoutineFilter.ACTIVE,
    /**
     * "Today" as the ViewModel's injected Clock sees it. This default is only ever
     * observed by a bare `RoutineUiState()` in a test — production always overwrites it
     * from `RoutineViewModel.today()` so the screen and ViewModel never disagree.
     */
    val today: LocalDate = LocalDate.now()
) {
    val completedToday: Int get() = habits.count { it.streak.completedToday }
    val totalHabits: Int get() = habits.size
    val canAddHabit: Boolean get() = activeCount < maxHabits
    val isEmpty: Boolean get() = !isLoading && habits.isEmpty()
}
