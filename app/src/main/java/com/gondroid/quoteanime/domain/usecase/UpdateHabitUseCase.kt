package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import javax.inject.Inject

sealed interface UpdateHabitResult {
    data class Success(val habit: Habit) : UpdateHabitResult
    data object BlankTitle : UpdateHabitResult
    data object InvalidDateRange : UpdateHabitResult
    data object HabitNotFound : UpdateHabitResult
}

class UpdateHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habit: Habit): UpdateHabitResult {
        repository.getHabit(habit.id) ?: return UpdateHabitResult.HabitNotFound
        if (habit.title.isBlank()) return UpdateHabitResult.BlankTitle
        if (habit.endDate != null && habit.endDate.isBefore(habit.startDate)) {
            return UpdateHabitResult.InvalidDateRange
        }

        repository.saveHabit(habit)
        return UpdateHabitResult.Success(habit)
    }
}
