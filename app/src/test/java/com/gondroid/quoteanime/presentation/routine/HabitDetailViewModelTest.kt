package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.SavedStateHandle
import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.ArchiveHabitUseCase
import com.gondroid.quoteanime.domain.usecase.CalculateStreakUseCase
import com.gondroid.quoteanime.domain.usecase.DeleteHabitUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleCompletionResult
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import com.gondroid.quoteanime.domain.usecase.UnarchiveHabitUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Scenarios covered:
 *  - The habit, its completions and streak reach the state
 *  - Toggling a valid day updates the selected date
 *  - Toggling a future day surfaces a message instead of changing anything
 *  - Month navigation moves the visible month forward and back
 *  - Archiving delegates to the use case, cancels the reminder and flags the state
 */
class HabitDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.parse("2026-07-25")
    private val fixedClock: Clock = Clock.fixed(
        today.atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    )

    private lateinit var repository: HabitRepository
    private lateinit var toggleHabitCompletion: ToggleHabitCompletionUseCase
    private lateinit var archiveHabit: ArchiveHabitUseCase
    private lateinit var unarchiveHabit: UnarchiveHabitUseCase
    private lateinit var deleteHabit: DeleteHabitUseCase
    private lateinit var reminderScheduler: HabitReminderScheduler
    private lateinit var analytics: RoutineAnalytics
    private val calculateStreak = CalculateStreakUseCase()

    private val habit = Habit(
        id = "h1",
        title = "Leer",
        iconKey = "book",
        colorIndex = 0,
        startDate = today.minusMonths(3)
    )

    private fun buildViewModel() = HabitDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("habitId" to "h1")),
        repository = repository,
        toggleHabitCompletion = toggleHabitCompletion,
        archiveHabit = archiveHabit,
        unarchiveHabit = unarchiveHabit,
        deleteHabit = deleteHabit,
        calculateStreak = calculateStreak,
        reminderScheduler = reminderScheduler,
        analytics = analytics,
        clock = fixedClock
    )

    @Before
    fun setup() {
        repository = mockk()
        toggleHabitCompletion = mockk()
        archiveHabit = mockk()
        unarchiveHabit = mockk()
        deleteHabit = mockk()
        reminderScheduler = mockk(relaxed = true)
        analytics = mockk(relaxed = true)
        coEvery { repository.getHabit("h1") } returns habit
        every { repository.getCompletions("h1") } returns flowOf(listOf(today, today.minusDays(1)))
    }

    @Test
    fun `given a habit id, when loaded, then the habit, completions and streak reach the state`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(habit, state.habit)
        assertEquals(setOf(today, today.minusDays(1)), state.completions)
        assertEquals(2, state.streak.current)
        assertTrue(state.streak.completedToday)
    }

    @Test
    fun `given a valid day, when toggled, then it becomes the selected date`() = runTest {
        coEvery { toggleHabitCompletion("h1", today.minusDays(2), today) } returns
            ToggleCompletionResult.Success(true)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onDayClick(today.minusDays(2))
        advanceUntilIdle()

        assertEquals(today.minusDays(2), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `given a future day, when toggled, then a message is exposed and nothing is selected`() = runTest {
        val future = today.plusDays(1)
        coEvery { toggleHabitCompletion("h1", future, today) } returns ToggleCompletionResult.FutureDate
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onDayClick(future)
        advanceUntilIdle()

        assertEquals(HabitDetailMessage.FutureDayNotAllowed, viewModel.uiState.value.message)
        assertEquals(null, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `given the visible month, when changed, then it moves by the given number of months`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        val startMonth = viewModel.uiState.value.visibleMonth

        viewModel.onMonthChanged(1)
        assertEquals(startMonth.plusMonths(1), viewModel.uiState.value.visibleMonth)

        viewModel.onMonthChanged(-1)
        assertEquals(startMonth, viewModel.uiState.value.visibleMonth)
    }

    @Test
    fun `given the initial state, when loaded, then the visible month is the current month`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(YearMonth.from(today), viewModel.uiState.value.visibleMonth)
    }

    @Test
    fun `given a habit, when archived, then the use case runs, the reminder is cancelled and the state is flagged`() = runTest {
        coEvery { archiveHabit("h1") } returns Unit
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onArchive()
        advanceUntilIdle()

        coVerify(exactly = 1) { archiveHabit("h1") }
        coVerify(exactly = 1) { reminderScheduler.cancel("h1") }
        assertTrue(viewModel.uiState.value.isArchived)
    }

    @Test
    fun `given an archived habit, when restored, then the use case runs and the habit stays open with isArchived false`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit.copy(isArchived = true)
        coEvery { unarchiveHabit("h1") } returns Unit
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onUnarchive()
        advanceUntilIdle()

        coVerify(exactly = 1) { unarchiveHabit("h1") }
        coVerify(exactly = 1) { reminderScheduler.schedule(any()) }
        assertEquals(false, viewModel.uiState.value.habit?.isArchived)
        assertEquals(false, viewModel.uiState.value.isArchived)
    }

    @Test
    fun `given a habit, when deleted, then the use case runs, the reminder is cancelled and the state is flagged`() = runTest {
        coEvery { deleteHabit("h1") } returns Unit
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onDelete()
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteHabit("h1") }
        coVerify(exactly = 1) { reminderScheduler.cancel("h1") }
        assertTrue(viewModel.uiState.value.isDeleted)
    }
}
