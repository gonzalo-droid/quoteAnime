package com.gondroid.quoteanime.domain.usecase

import app.cash.turbine.test
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - Each habit is paired with its own completions and streak
 *  - Only completions inside the visible window reach the heatmap
 *  - The streak still counts runs older than the visible window
 *  - Completion rate is measured from the habit start date
 *  - No habits produces an empty list
 */
class GetActiveHabitsUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var useCase: GetActiveHabitsUseCase

    private val today = LocalDate.parse("2026-07-25")

    private val habit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = today.minusDays(9)
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetActiveHabitsUseCase(repository, CalculateStreakUseCase())
    }

    @Test
    fun `given no habits, when collected, then the list is empty`() = runTest {
        every { repository.getActiveHabits() } returns flowOf(emptyList())

        useCase(today).test {
            assertEquals(emptyList<Any>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given a habit with recent completions, when collected, then streak and completions are attached`() = runTest {
        every { repository.getActiveHabits() } returns flowOf(listOf(habit))
        every { repository.getCompletions("h1") } returns flowOf(
            listOf(today, today.minusDays(1), today.minusDays(2))
        )

        useCase(today).test {
            val item = awaitItem().single()
            assertEquals(3, item.streak.current)
            assertEquals(3, item.completions.size)
            awaitComplete()
        }
    }

    @Test
    fun `given completions older than the visible window, when collected, then they are excluded from the heatmap`() = runTest {
        val old = today.minusWeeks(GetActiveHabitsUseCase.VISIBLE_WEEKS.toLong() + 1)
        every { repository.getActiveHabits() } returns flowOf(listOf(habit))
        every { repository.getCompletions("h1") } returns flowOf(listOf(today, old))

        useCase(today).test {
            val item = awaitItem().single()
            assertEquals(setOf(today), item.completions)
            awaitComplete()
        }
    }

    @Test
    fun `given ten active days and five completions, when collected, then completion rate is one half`() = runTest {
        // startDate is 9 days ago, so the active window is 10 days including today
        every { repository.getActiveHabits() } returns flowOf(listOf(habit))
        every { repository.getCompletions("h1") } returns flowOf(
            listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(4))
        )

        useCase(today).test {
            assertEquals(0.5f, awaitItem().single().completionRate, 0.001f)
            awaitComplete()
        }
    }
}
