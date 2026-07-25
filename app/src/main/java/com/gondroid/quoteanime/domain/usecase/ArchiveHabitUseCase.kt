package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.HabitRepository
import javax.inject.Inject

/** Archiving keeps the history: completions are never deleted from the database. */
class ArchiveHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String) = repository.archiveHabit(habitId)
}
