package com.gondroid.quoteanime.presentation.routine

import app.cash.turbine.test
import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.domain.usecase.ArchiveHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import com.gondroid.quoteanime.domain.usecase.GetGlobalStreakUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleCompletionResult
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import com.gondroid.quoteanime.notification.HabitReminderScheduler
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Scenarios covered:
 *  - Habits and global streak reach the state
 *  - completedToday counts only habits completed today
 *  - canAddHabit turns false once the limit is reached
 *  - Toggling a day delegates to the use case
 *  - A rejected toggle surfaces a message
 *  - Archiving delegates to the use case and cancels the habit's reminder
 */
class RoutineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.parse("2026-07-25")

    private lateinit var getActiveHabits: GetActiveHabitsUseCase
    private lateinit var getGlobalStreak: GetGlobalStreakUseCase
    private lateinit var toggleCompletion: ToggleHabitCompletionUseCase
    private lateinit var archiveHabit: ArchiveHabitUseCase
    private lateinit var reminderScheduler: HabitReminderScheduler
    private val premiumGate = PremiumGate()

    private fun habit(id: String) = Habit(
        id = id,
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = today.minusDays(10)
    )

    private fun progress(id: String, completedToday: Boolean) = HabitWithProgress(
        habit = habit(id),
        completions = if (completedToday) setOf(today) else emptySet(),
        streak = StreakState(current = 1, best = 1, lastCompletedDate = today, completedToday = completedToday),
        completionRate = 0.5f
    )

    /** Pinned clock: the tests must not depend on the machine's current date. */
    private val fixedClock: Clock = Clock.fixed(
        LocalDate.parse("2026-07-25").atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    )

    private fun buildViewModel() = RoutineViewModel(
        getActiveHabits = getActiveHabits,
        getGlobalStreak = getGlobalStreak,
        toggleHabitCompletion = toggleCompletion,
        archiveHabit = archiveHabit,
        reminderScheduler = reminderScheduler,
        premiumGate = premiumGate,
        clock = fixedClock
    )

    @Before
    fun setup() {
        getActiveHabits = mockk()
        getGlobalStreak = mockk()
        toggleCompletion = mockk()
        archiveHabit = mockk()
        reminderScheduler = mockk(relaxed = true)
        every { getGlobalStreak(any()) } returns flowOf(StreakState(current = 4, best = 9))
    }

    @Test
    fun `given habits, when state is collected, then they reach the state with the global streak`() = runTest {
        every { getActiveHabits(today) } returns flowOf(listOf(progress("h1", completedToday = true)))

        buildViewModel().uiState.test {
            skipItems(1) // initial loading state
            val state = awaitItem()
            assertEquals(1, state.habits.size)
            assertEquals(4, state.globalStreak.current)
            assertEquals(1, state.completedToday)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `given fewer habits than the limit, when state is collected, then adding is allowed`() = runTest {
        every { getActiveHabits(today) } returns flowOf(listOf(progress("h1", false)))

        buildViewModel().uiState.test {
            skipItems(1)
            assertTrue(awaitItem().canAddHabit)
        }
    }

    @Test
    fun `given the limit is reached, when state is collected, then adding is blocked`() = runTest {
        every { getActiveHabits(today) } returns flowOf(
            listOf(progress("h1", false), progress("h2", false), progress("h3", false))
        )

        buildViewModel().uiState.test {
            skipItems(1)
            assertFalse(awaitItem().canAddHabit)
        }
    }

    @Test
    fun `given a day, when toggled, then the use case is invoked with today as reference`() = runTest {
        every { getActiveHabits(today) } returns flowOf(emptyList())
        coEvery { toggleCompletion("h1", today, today) } returns ToggleCompletionResult.Success(true)

        val viewModel = buildViewModel()
        viewModel.onToggleDay("h1", today)
        advanceUntilIdle()

        coVerify(exactly = 1) { toggleCompletion("h1", today, today) }
    }

    @Test
    fun `given a future day, when toggled, then a message is exposed`() = runTest {
        every { getActiveHabits(today) } returns flowOf(emptyList())
        val future = today.plusDays(1)
        coEvery { toggleCompletion("h1", future, today) } returns ToggleCompletionResult.FutureDate

        val viewModel = buildViewModel()
        viewModel.onToggleDay("h1", future)
        advanceUntilIdle()

        assertEquals(RoutineMessage.FutureDayNotAllowed, viewModel.uiState.value.message)
    }

    @Test
    fun `given a habit id, when archived, then the use case is invoked and the reminder is cancelled`() = runTest {
        every { getActiveHabits(today) } returns flowOf(emptyList())
        coEvery { archiveHabit("h1") } returns Unit

        val viewModel = buildViewModel()
        viewModel.onArchiveHabit("h1")
        advanceUntilIdle()

        coVerify(exactly = 1) { archiveHabit("h1") }
        coVerify(exactly = 1) { reminderScheduler.cancel("h1") }
    }
}
