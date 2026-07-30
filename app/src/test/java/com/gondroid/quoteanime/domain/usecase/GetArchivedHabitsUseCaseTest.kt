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
 *  - No archived habits produces an empty list
 *  - An archived habit is paired with its own completions and streak, same as active habits
 */
class GetArchivedHabitsUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var useCase: GetArchivedHabitsUseCase

    private val today = LocalDate.parse("2026-07-25")

    private val habit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = today.minusDays(9),
        isArchived = true
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetArchivedHabitsUseCase(repository, CalculateStreakUseCase())
    }

    @Test
    fun `given no archived habits, when collected, then the list is empty`() = runTest {
        every { repository.getArchivedHabits() } returns flowOf(emptyList())

        useCase(today).test {
            assertEquals(emptyList<Any>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given an archived habit with completions, when collected, then streak and completions are attached`() = runTest {
        every { repository.getArchivedHabits() } returns flowOf(listOf(habit))
        every { repository.getCompletions("h1") } returns flowOf(
            listOf(today, today.minusDays(1), today.minusDays(2))
        )

        useCase(today).test {
            val item = awaitItem().single()
            assertEquals(habit, item.habit)
            assertEquals(3, item.streak.current)
            assertEquals(3, item.completions.size)
            awaitComplete()
        }
    }
}
