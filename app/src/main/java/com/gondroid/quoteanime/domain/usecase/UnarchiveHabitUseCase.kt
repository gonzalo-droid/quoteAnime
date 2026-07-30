package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.HabitRepository
import javax.inject.Inject

/** Restores an archived habit back into the active "Mi rutina" list. */
class UnarchiveHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String) = repository.unarchiveHabit(habitId)
}
