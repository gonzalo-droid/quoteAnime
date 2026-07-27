package com.gondroid.quoteanime.presentation.routine

import app.cash.turbine.test
import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.ArchiveHabitUseCase
import com.gondroid.quoteanime.domain.usecase.CalculateStreakUseCase
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import com.gondroid.quoteanime.domain.usecase.GetGlobalStreakUseCase
import com.gondroid.quoteanime.domain.usecase.IsRoutineIntroSeenUseCase
import com.gondroid.quoteanime.domain.usecase.SetRoutineIntroSeenUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleCompletionResult
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import com.gondroid.quoteanime.notification.HabitReminderScheduler
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private lateinit var isRoutineIntroSeen: IsRoutineIntroSeenUseCase
    private lateinit var setRoutineIntroSeen: SetRoutineIntroSeenUseCase
    private lateinit var reminderScheduler: HabitReminderScheduler
    private lateinit var analytics: RoutineAnalytics
    private lateinit var habitRepository: HabitRepository
    private val calculateStreak = CalculateStreakUseCase()
    private val premiumGate = PremiumGate()

    private fun habit(id: String, createdAt: Long = 0L) = Habit(
        id = id,
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = today.minusDays(10),
        createdAt = createdAt
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
        isRoutineIntroSeen = isRoutineIntroSeen,
        setRoutineIntroSeen = setRoutineIntroSeen,
        reminderScheduler = reminderScheduler,
        premiumGate = premiumGate,
        analytics = analytics,
        habitRepository = habitRepository,
        calculateStreak = calculateStreak,
        clock = fixedClock
    )

    @Before
    fun setup() {
        getActiveHabits = mockk()
        getGlobalStreak = mockk()
        toggleCompletion = mockk()
        archiveHabit = mockk()
        isRoutineIntroSeen = mockk()
        setRoutineIntroSeen = mockk(relaxed = true)
        reminderScheduler = mockk(relaxed = true)
        analytics = mockk(relaxed = true)
        habitRepository = mockk()
        every { getGlobalStreak(any()) } returns flowOf(StreakState(current = 4, best = 9))
        every { isRoutineIntroSeen() } returns flowOf(true)
        every { habitRepository.getCompletions(any()) } returns flowOf(emptyList())
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
    fun `given onToggleToday, when invoked, then it resolves today live from the clock instead of a cached value`() = runTest {
        every { getActiveHabits(today) } returns flowOf(emptyList())
        coEvery { toggleCompletion("h1", today, today) } returns ToggleCompletionResult.Success(true)

        val viewModel = buildViewModel()
        viewModel.onToggleToday("h1")
        advanceUntilIdle()

        // Must resolve to the ViewModel's live today() (from the fixed clock), not some other
        // stale/cached date — this is what protects the "mark today" button from writing to
        // the wrong day if RoutineUiState.today ever goes stale across a midnight rollover.
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

    @Test
    fun `given a habit found in state, when archived, then analytics report days active`() = runTest {
        val daysAgo = 5L
        val createdAt = fixedClock.millis() - daysAgo * 24 * 60 * 60 * 1000L
        every { getActiveHabits(today) } returns flowOf(
            listOf(progress("h1", completedToday = false).copy(habit = habit("h1", createdAt)))
        )
        coEvery { archiveHabit("h1") } returns Unit

        val viewModel = buildViewModel()
        advanceUntilIdle() // let observeRoutine() populate state before archiving reads it
        viewModel.onArchiveHabit("h1")
        advanceUntilIdle()

        verify(exactly = 1) { analytics.trackHabitArchived(daysAgo) }
    }

    @Test
    fun `given a habit not in state, when archived, then no analytics event is sent`() = runTest {
        every { getActiveHabits(today) } returns flowOf(emptyList())
        coEvery { archiveHabit("missing") } returns Unit

        val viewModel = buildViewModel()
        viewModel.onArchiveHabit("missing")
        advanceUntilIdle()

        verify(exactly = 0) { analytics.trackHabitArchived(any()) }
    }

    @Test
    fun `given a completion that newly reaches a milestone, when toggled, then a milestone event is sent`() = runTest {
        val sixDayStreak = StreakState(current = 6, best = 6, lastCompletedDate = today.minusDays(1))
        every { getActiveHabits(today) } returns flowOf(
            listOf(progress("h1", completedToday = false).copy(streak = sixDayStreak))
        )
        every { habitRepository.getCompletions("h1") } returns
            flowOf((0..6L).map { today.minusDays(it) })
        coEvery { toggleCompletion("h1", today, today) } returns ToggleCompletionResult.Success(true)

        val viewModel = buildViewModel()
        advanceUntilIdle() // let observeRoutine() populate state before the toggle reads it
        viewModel.onToggleDay("h1", today)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.trackStreakMilestone(7) }
    }

    @Test
    fun `given a streak decreasing through a milestone number, when toggled, then no milestone event is sent`() = runTest {
        // previousStreak = 8 (today was completed); unmarking today drops it to 7, which is
        // itself a milestone number — but a DECREASE through 7 must not fire the "reached 7"
        // celebration event.
        val eightDayStreak = StreakState(current = 8, best = 8, lastCompletedDate = today, completedToday = true)
        every { getActiveHabits(today) } returns flowOf(
            listOf(progress("h1", completedToday = true).copy(streak = eightDayStreak))
        )
        every { habitRepository.getCompletions("h1") } returns
            flowOf((1..7L).map { today.minusDays(it) }) // today's completion removed, 7 days remain
        coEvery { toggleCompletion("h1", today, today) } returns ToggleCompletionResult.Success(false)

        val viewModel = buildViewModel()
        advanceUntilIdle() // let observeRoutine() populate state before the toggle reads it
        viewModel.onToggleDay("h1", today)
        advanceUntilIdle()

        coVerify(exactly = 0) { analytics.trackStreakMilestone(any()) }
    }

    @Test
    fun `given the only completion is unmarked, when toggled, then a streak broken event is sent`() = runTest {
        val threeDayStreak = StreakState(current = 3, best = 3, lastCompletedDate = today, completedToday = true)
        every { getActiveHabits(today) } returns flowOf(
            listOf(progress("h1", completedToday = true).copy(streak = threeDayStreak))
        )
        every { habitRepository.getCompletions("h1") } returns flowOf(emptyList())
        coEvery { toggleCompletion("h1", today, today) } returns ToggleCompletionResult.Success(false)

        val viewModel = buildViewModel()
        advanceUntilIdle() // let observeRoutine() populate state before the toggle reads it
        viewModel.onToggleDay("h1", today)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.trackStreakBroken(3) }
    }
}
