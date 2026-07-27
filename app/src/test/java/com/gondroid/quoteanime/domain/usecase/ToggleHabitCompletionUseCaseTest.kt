package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - Marking an unmarked day inserts it
 *  - Marking an already marked day removes it (toggle)
 *  - Retroactive marking of a past day is allowed
 *  - Future days are rejected
 *  - Days before startDate or after endDate are rejected
 *  - Unknown habit id is reported
 */
class ToggleHabitCompletionUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var useCase: ToggleHabitCompletionUseCase

    private val today = LocalDate.parse("2026-07-25")
    private val habit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = LocalDate.parse("2026-07-01"),
        endDate = LocalDate.parse("2026-07-31")
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = ToggleHabitCompletionUseCase(repository)
    }

    @Test
    fun `given an unmarked day, when toggled, then it is marked as completed`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        coEvery { repository.isCompleted("h1", today) } returns false
        coJustRun { repository.setCompletion("h1", today, true) }

        val result = useCase("h1", today, today)

        assertEquals(ToggleCompletionResult.Success(completed = true), result)
        coVerify(exactly = 1) { repository.setCompletion("h1", today, true) }
    }

    @Test
    fun `given a marked day, when toggled, then it is unmarked`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        coEvery { repository.isCompleted("h1", today) } returns true
        coJustRun { repository.setCompletion("h1", today, false) }

        val result = useCase("h1", today, today)

        assertEquals(ToggleCompletionResult.Success(completed = false), result)
    }

    @Test
    fun `given a past day inside the range, when toggled, then it is allowed`() = runTest {
        val pastDay = today.minusDays(5)
        coEvery { repository.getHabit("h1") } returns habit
        coEvery { repository.isCompleted("h1", pastDay) } returns false
        coJustRun { repository.setCompletion("h1", pastDay, true) }

        assertEquals(ToggleCompletionResult.Success(true), useCase("h1", pastDay, today))
    }

    @Test
    fun `given a future day, when toggled, then it is rejected`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit

        val result = useCase("h1", today.plusDays(1), today)

        assertEquals(ToggleCompletionResult.FutureDate, result)
        coVerify(exactly = 0) { repository.setCompletion(any(), any(), any()) }
    }

    @Test
    fun `given a day before the habit started, when toggled, then it is rejected`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit

        val result = useCase("h1", LocalDate.parse("2026-06-30"), today)

        assertEquals(ToggleCompletionResult.OutsideHabitRange, result)
    }

    @Test
    fun `given a day after the habit ended, when toggled, then it is rejected`() = runTest {
        val endedHabit = habit.copy(endDate = LocalDate.parse("2026-07-10"))
        coEvery { repository.getHabit("h1") } returns endedHabit

        val result = useCase("h1", LocalDate.parse("2026-07-20"), today)

        assertEquals(ToggleCompletionResult.OutsideHabitRange, result)
    }

    @Test
    fun `given an unknown habit, when toggled, then it is reported as not found`() = runTest {
        coEvery { repository.getHabit("missing") } returns null

        assertEquals(ToggleCompletionResult.HabitNotFound, useCase("missing", today, today))
    }
}
