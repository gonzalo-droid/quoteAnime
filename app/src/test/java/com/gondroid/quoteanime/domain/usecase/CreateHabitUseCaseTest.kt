package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Scenarios covered:
 *  - Creating below the limit succeeds and persists
 *  - Creating at the limit is rejected without touching the repository
 *  - Blank titles are rejected
 *  - End date before start date is rejected
 *  - End date equal to start date is accepted (a one-day challenge)
 */
class CreateHabitUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var useCase: CreateHabitUseCase
    private val premiumGate = PremiumGate()
    private val today = LocalDate.parse("2026-07-25")

    @Before
    fun setup() {
        repository = mockk()
        useCase = CreateHabitUseCase(repository, premiumGate)
    }

    private suspend fun create(
        title: String = "Entrenar",
        startDate: LocalDate = today,
        endDate: LocalDate? = null
    ) = useCase(
        title = title,
        iconKey = "dumbbell",
        colorIndex = 2,
        startDate = startDate,
        endDate = endDate,
        reminderTime = LocalTime.of(7, 0),
        reminderDays = setOf(DayOfWeek.MONDAY),
        templateId = null
    )

    @Test
    fun `given fewer habits than the limit, when creating, then it succeeds and is saved`() = runTest {
        coEvery { repository.countActiveHabits() } returns 2
        coJustRun { repository.saveHabit(any()) }

        val result = create()

        assertTrue(result is CreateHabitResult.Success)
        assertEquals("Entrenar", (result as CreateHabitResult.Success).habit.title)
        coVerify(exactly = 1) { repository.saveHabit(any()) }
    }

    @Test
    fun `given the limit is reached, when creating, then it is rejected and nothing is saved`() = runTest {
        coEvery { repository.countActiveHabits() } returns 3

        val result = create()

        assertEquals(CreateHabitResult.LimitReached(3), result)
        coVerify(exactly = 0) { repository.saveHabit(any()) }
    }

    @Test
    fun `given a blank title, when creating, then it is rejected`() = runTest {
        coEvery { repository.countActiveHabits() } returns 0

        assertEquals(CreateHabitResult.BlankTitle, create(title = "   "))
        coVerify(exactly = 0) { repository.saveHabit(any()) }
    }

    @Test
    fun `given an end date before the start date, when creating, then it is rejected`() = runTest {
        coEvery { repository.countActiveHabits() } returns 0

        val result = create(startDate = today, endDate = today.minusDays(1))

        assertEquals(CreateHabitResult.InvalidDateRange, result)
    }

    @Test
    fun `given an end date equal to the start date, when creating, then it succeeds`() = runTest {
        coEvery { repository.countActiveHabits() } returns 0
        coJustRun { repository.saveHabit(any()) }

        assertTrue(create(startDate = today, endDate = today) is CreateHabitResult.Success)
    }

    @Test
    fun `given a title with surrounding spaces, when creating, then it is trimmed`() = runTest {
        coEvery { repository.countActiveHabits() } returns 0
        coJustRun { repository.saveHabit(any()) }

        val result = create(title = "  Leer  ") as CreateHabitResult.Success

        assertEquals("Leer", result.habit.title)
    }
}
