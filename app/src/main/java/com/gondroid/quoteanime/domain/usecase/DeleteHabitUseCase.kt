package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.HabitRepository
import javax.inject.Inject

/** Permanently removes a habit and all of its completions — unlike archiving, not reversible. */
class DeleteHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String) = repository.deleteHabit(habitId)
}
