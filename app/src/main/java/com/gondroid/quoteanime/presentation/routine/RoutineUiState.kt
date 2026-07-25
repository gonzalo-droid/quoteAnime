package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState

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
    val message: RoutineMessage? = null
) {
    val completedToday: Int get() = habits.count { it.streak.completedToday }
    val totalHabits: Int get() = habits.size
    val canAddHabit: Boolean get() = habits.size < maxHabits
    val isEmpty: Boolean get() = !isLoading && habits.isEmpty()
}
