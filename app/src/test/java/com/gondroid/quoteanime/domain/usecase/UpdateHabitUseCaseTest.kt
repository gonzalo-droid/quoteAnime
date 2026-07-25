package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - Updating an existing habit persists the new values
 *  - Blank title and invalid date range are rejected
 *  - Updating a habit that no longer exists is reported
 *  - Archiving delegates to the repository
 */
class UpdateHabitUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var updateHabit: UpdateHabitUseCase
    private lateinit var archiveHabit: ArchiveHabitUseCase

    private val habit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = LocalDate.parse("2026-07-01")
    )

    @Before
    fun setup() {
        repository = mockk()
        updateHabit = UpdateHabitUseCase(repository)
        archiveHabit = ArchiveHabitUseCase(repository)
    }

    @Test
    fun `given an existing habit, when updated, then the new values are saved`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        coJustRun { repository.saveHabit(any()) }
        val edited = habit.copy(title = "Entrenar duro", colorIndex = 5)

        val result = updateHabit(edited)

        assertTrue(result is UpdateHabitResult.Success)
        coVerify(exactly = 1) { repository.saveHabit(edited) }
    }

    @Test
    fun `given a blank title, when updated, then it is rejected`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit

        assertEquals(UpdateHabitResult.BlankTitle, updateHabit(habit.copy(title = "  ")))
        coVerify(exactly = 0) { repository.saveHabit(any()) }
    }

    @Test
    fun `given an end date before the start date, when updated, then it is rejected`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        val invalid = habit.copy(endDate = habit.startDate.minusDays(1))

        assertEquals(UpdateHabitResult.InvalidDateRange, updateHabit(invalid))
    }

    @Test
    fun `given a habit that no longer exists, when updated, then it is reported as not found`() = runTest {
        coEvery { repository.getHabit("h1") } returns null

        assertEquals(UpdateHabitResult.HabitNotFound, updateHabit(habit))
    }

    @Test
    fun `given a habit id, when archived, then the repository archives it`() = runTest {
        coJustRun { repository.archiveHabit("h1") }

        archiveHabit("h1")

        coVerify(exactly = 1) { repository.archiveHabit("h1") }
    }
}
