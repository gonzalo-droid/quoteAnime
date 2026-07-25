package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.HabitRepository
import java.time.LocalDate
import javax.inject.Inject

sealed interface ToggleCompletionResult {
    data class Success(val completed: Boolean) : ToggleCompletionResult
    data object HabitNotFound : ToggleCompletionResult
    data object FutureDate : ToggleCompletionResult
    data object OutsideHabitRange : ToggleCompletionResult
}

/**
 * Marks or unmarks a single day. Past days can be corrected; future days cannot
 * be marked, and days outside the habit's own start/end window are rejected so
 * the completion rate stays meaningful.
 */
class ToggleHabitCompletionUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(
        habitId: String,
        date: LocalDate,
        today: LocalDate
    ): ToggleCompletionResult {
        val habit = repository.getHabit(habitId) ?: return ToggleCompletionResult.HabitNotFound
        if (date.isAfter(today)) return ToggleCompletionResult.FutureDate
        if (!habit.isActiveOn(date)) return ToggleCompletionResult.OutsideHabitRange

        val newValue = !repository.isCompleted(habitId, date)
        repository.setCompletion(habitId, date, newValue)
        return ToggleCompletionResult.Success(completed = newValue)
    }
}
