package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.StreakState
import java.time.LocalDate
import javax.inject.Inject

/**
 * Pure function: given the dates a habit was completed, returns the streak state.
 * A streak stays alive while the most recent completion is today or yesterday.
 */
class CalculateStreakUseCase @Inject constructor() {

    operator fun invoke(dates: List<LocalDate>, today: LocalDate): StreakState {
        val sorted = dates.distinct().sortedDescending()
        if (sorted.isEmpty()) return StreakState()

        val last = sorted.first()
        val isAlive = last == today || last == today.minusDays(1)
        val current = if (isAlive) runLengthFrom(sorted, 0) else 0
        val best = longestRun(sorted)

        return StreakState(
            current = current,
            best = best,
            lastCompletedDate = last,
            completedToday = last == today
        )
    }

    /** Length of the consecutive run starting at [startIndex] in a descending list. */
    private fun runLengthFrom(sorted: List<LocalDate>, startIndex: Int): Int {
        var length = 1
        var index = startIndex
        while (index + 1 < sorted.size && sorted[index + 1] == sorted[index].minusDays(1)) {
            length++
            index++
        }
        return length
    }

    private fun longestRun(sorted: List<LocalDate>): Int {
        var best = 1
        var currentRun = 1
        for (index in 1 until sorted.size) {
            currentRun = if (sorted[index] == sorted[index - 1].minusDays(1)) currentRun + 1 else 1
            if (currentRun > best) best = currentRun
        }
        return best
    }
}
