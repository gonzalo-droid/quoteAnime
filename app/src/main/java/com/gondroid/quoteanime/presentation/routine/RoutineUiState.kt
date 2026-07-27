package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import java.time.LocalDate

enum class RoutineMessage {
    FutureDayNotAllowed,
    OutsideHabitRange,
    HabitLimitReached
}

data class RoutineUiState(
    val habits: List<HabitWithProgress> = emptyList(),
    val globalStreak: StreakState = StreakState(),
    val isLoading: Boolean = true,
    val maxHabits: Int = 0,
    val message: RoutineMessage? = null,
    val showIntro: Boolean = false,
    /**
     * "Today" as the ViewModel's injected Clock sees it. This default is only ever
     * observed by a bare `RoutineUiState()` in a test — production always overwrites it
     * from `RoutineViewModel.today()` so the screen and ViewModel never disagree.
     */
    val today: LocalDate = LocalDate.now()
) {
    val completedToday: Int get() = habits.count { it.streak.completedToday }
    val totalHabits: Int get() = habits.size
    val canAddHabit: Boolean get() = habits.size < maxHabits
    val isEmpty: Boolean get() = !isLoading && habits.isEmpty()
}
