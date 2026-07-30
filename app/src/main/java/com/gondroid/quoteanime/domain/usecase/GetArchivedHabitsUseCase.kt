package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.repository.HabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Mirrors GetActiveHabitsUseCase but sources archived habits, for the "Archivados" filter
 * in Mi rutina. There's no archivedAt timestamp on Habit, so completion rate is still
 * computed against "today" — a reasonable approximation, same as before archiving.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetArchivedHabitsUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val calculateStreak: CalculateStreakUseCase
) {
    operator fun invoke(today: LocalDate): Flow<List<HabitWithProgress>> =
        repository.getArchivedHabits().flatMapLatest { habits ->
            if (habits.isEmpty()) {
                flowOf(emptyList())
            } else {
                val windowStart = today.minusWeeks(GetActiveHabitsUseCase.VISIBLE_WEEKS.toLong())
                val progressFlows = habits.map { habit ->
                    repository.getCompletions(habit.id).map { dates ->
                        HabitWithProgress(
                            habit = habit,
                            completions = dates.filter { !it.isBefore(windowStart) }.toSet(),
                            streak = calculateStreak(dates, today),
                            completionRate = completionRate(habit.startDate, today, dates.size)
                        )
                    }
                }
                combine(progressFlows) { it.toList() }
            }
        }

    private fun completionRate(startDate: LocalDate, today: LocalDate, completed: Int): Float {
        val activeDays = ChronoUnit.DAYS.between(startDate, today) + 1
        if (activeDays <= 0) return 0f
        return (completed.toFloat() / activeDays.toFloat()).coerceIn(0f, 1f)
    }
}
