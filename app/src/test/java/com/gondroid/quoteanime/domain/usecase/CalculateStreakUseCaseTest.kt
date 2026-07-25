package com.gondroid.quoteanime.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - No completions at all
 *  - Only today
 *  - Consecutive run ending today
 *  - Run ending yesterday (streak still alive, not completed today)
 *  - Run ending two days ago (streak broken, best preserved)
 *  - Retroactive marking that joins two separate runs
 *  - Duplicated dates are ignored
 *  - Runs crossing month and year boundaries
 */
class CalculateStreakUseCaseTest {

    private val today = LocalDate.parse("2026-07-25")
    private lateinit var useCase: CalculateStreakUseCase

    @Before
    fun setup() {
        useCase = CalculateStreakUseCase()
    }

    private fun daysAgo(vararg offsets: Int): List<LocalDate> =
        offsets.map { today.minusDays(it.toLong()) }

    @Test
    fun `given no completions, when calculated, then everything is zero`() {
        val result = useCase(emptyList(), today)

        assertEquals(0, result.current)
        assertEquals(0, result.best)
        assertNull(result.lastCompletedDate)
        assertFalse(result.completedToday)
    }

    @Test
    fun `given only today, when calculated, then current streak is one and completed today is true`() {
        val result = useCase(daysAgo(0), today)

        assertEquals(1, result.current)
        assertEquals(1, result.best)
        assertEquals(today, result.lastCompletedDate)
        assertTrue(result.completedToday)
    }

    @Test
    fun `given three consecutive days ending today, when calculated, then current streak is three`() {
        val result = useCase(daysAgo(0, 1, 2), today)

        assertEquals(3, result.current)
        assertEquals(3, result.best)
        assertTrue(result.completedToday)
    }

    @Test
    fun `given a run ending yesterday, when calculated, then the streak is alive but not completed today`() {
        val result = useCase(daysAgo(1, 2, 3), today)

        assertEquals(3, result.current)
        assertFalse(result.completedToday)
    }

    @Test
    fun `given the last completion was two days ago, when calculated, then current is zero and best is preserved`() {
        val result = useCase(daysAgo(2, 3, 4, 5), today)

        assertEquals(0, result.current)
        assertEquals(4, result.best)
        assertEquals(today.minusDays(2), result.lastCompletedDate)
    }

    @Test
    fun `given a retroactive mark joining two runs, when calculated, then the runs count as one`() {
        // Days 0,1 and 3,4 are done; marking day 2 joins them into a run of five
        val result = useCase(daysAgo(0, 1, 2, 3, 4), today)

        assertEquals(5, result.current)
        assertEquals(5, result.best)
    }

    @Test
    fun `given duplicated dates, when calculated, then they count once`() {
        val result = useCase(daysAgo(0, 0, 1, 1), today)

        assertEquals(2, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun `given a run crossing the end of the year, when calculated, then it counts as consecutive`() {
        val newYear = LocalDate.parse("2026-01-01")
        val dates = listOf(
            LocalDate.parse("2026-01-01"),
            LocalDate.parse("2025-12-31"),
            LocalDate.parse("2025-12-30")
        )

        val result = useCase(dates, newYear)

        assertEquals(3, result.current)
    }

    @Test
    fun `given an old long run and a short current run, when calculated, then best keeps the long one`() {
        val dates = daysAgo(0, 1) + daysAgo(10, 11, 12, 13, 14)

        val result = useCase(dates, today)

        assertEquals(2, result.current)
        assertEquals(5, result.best)
    }
}
