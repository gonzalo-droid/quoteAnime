package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** A day counts for the global streak when at least one habit was completed. */
class GetGlobalStreakUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val calculateStreak: CalculateStreakUseCase
) {
    operator fun invoke(today: LocalDate): Flow<StreakState> =
        repository.getAllCompletionDates().map { dates -> calculateStreak(dates, today) }
}
