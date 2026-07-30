package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.SavedStateHandle
import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.data.remote.QuoteRemoteDataSource
import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.CreateHabitResult
import com.gondroid.quoteanime.domain.usecase.CreateHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateHabitResult
import com.gondroid.quoteanime.domain.usecase.UpdateHabitUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Scenarios covered:
 *  - Templates are loaded into the state
 *  - Selecting a template fills title and icon
 *  - Saving a new habit calls CreateHabitUseCase and schedules its reminder
 *  - Reaching the limit surfaces an error and does not close the sheet
 *  - Editing an existing habit loads it, calls UpdateHabitUseCase and reschedules the reminder
 *  - Turning the reminder off clears time and days
 */
class HabitEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getTemplates: GetHabitTemplatesUseCase
    private lateinit var createHabit: CreateHabitUseCase
    private lateinit var updateHabit: UpdateHabitUseCase
    private lateinit var repository: HabitRepository
    private lateinit var reminderScheduler: HabitReminderScheduler
    private lateinit var analytics: RoutineAnalytics
    private lateinit var quoteRemoteDataSource: QuoteRemoteDataSource

    private val today = LocalDate.parse("2026-07-25")

    private val fixedClock: Clock = Clock.fixed(
        LocalDate.parse("2026-07-25").atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    )

    private fun buildViewModel(habitId: String? = null) = HabitEditorViewModel(
        savedStateHandle = SavedStateHandle(mapOf("habitId" to habitId)),
        getHabitTemplates = getTemplates,
        createHabit = createHabit,
        updateHabit = updateHabit,
        repository = repository,
        reminderScheduler = reminderScheduler,
        analytics = analytics,
        quoteRemoteDataSource = quoteRemoteDataSource,
        clock = fixedClock
    )

    @Before
    fun setup() {
        getTemplates = mockk()
        createHabit = mockk()
        updateHabit = mockk()
        repository = mockk()
        reminderScheduler = mockk(relaxed = true)
        analytics = mockk(relaxed = true)
        quoteRemoteDataSource = mockk()
        every { getTemplates() } returns flowOf(DefaultHabitTemplates.ALL)
        coEvery { quoteRemoteDataSource.getAnimeImages() } returns emptyMap()
    }

    @Test
    fun `given the editor opens, when templates load, then they reach the state`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(DefaultHabitTemplates.ALL, viewModel.uiState.value.templates)
    }

    @Test
    fun `given a template, when selected, then the resolved title and icon are filled`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        val template = DefaultHabitTemplates.ALL.first()
        // No Compose context in a JVM unit test, so the resolved title is passed as a
        // literal — mirroring what resolveTemplateTitle would produce at the call site.
        val resolvedTitle = "Entrenar"

        viewModel.onTemplateSelected(template, resolvedTitle)

        assertEquals(resolvedTitle, viewModel.uiState.value.title)
        assertEquals(template.iconKey, viewModel.uiState.value.iconKey)
        assertEquals(template.id, viewModel.uiState.value.templateId)
    }

    @Test
    fun `given a new habit, when saved, then it is created and its reminder scheduled`() = runTest {
        val created = Habit(id = "h1", title = "Leer", iconKey = "book", colorIndex = 0, startDate = today)
        coEvery { createHabit(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            CreateHabitResult.Success(created)
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.onTitleChanged("Leer")

        viewModel.onSave()
        advanceUntilIdle()

        coVerify(exactly = 1) { createHabit(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { reminderScheduler.schedule(created) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `given the limit is reached, when saving, then an error is exposed and it is not saved`() = runTest {
        coEvery { createHabit(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            CreateHabitResult.LimitReached(3)
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.onTitleChanged("Leer")

        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(HabitEditorError.LimitReached(3), viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isSaved)
    }

    @Test
    fun `given an existing habit id, when the editor opens, then it is loaded and updated on save`() = runTest {
        val existing = Habit(
            id = "h1",
            title = "Entrenar",
            iconKey = "dumbbell",
            colorIndex = 2,
            startDate = today.minusDays(5),
            reminderTime = LocalTime.of(7, 0),
            reminderDays = setOf(DayOfWeek.MONDAY)
        )
        coEvery { repository.getHabit("h1") } returns existing
        coEvery { updateHabit(any()) } returns UpdateHabitResult.Success(existing)

        val viewModel = buildViewModel(habitId = "h1")
        advanceUntilIdle()
        assertEquals("Entrenar", viewModel.uiState.value.title)

        viewModel.onSave()
        advanceUntilIdle()

        coVerify(exactly = 1) { updateHabit(any()) }
        coVerify(exactly = 1) { reminderScheduler.schedule(any()) }
    }

    @Test
    fun `given the reminder is turned off, when saving, then time and days are cleared`() = runTest {
        var captured: Set<DayOfWeek>? = null
        coEvery {
            createHabit(any(), any(), any(), any(), any(), any(), null, any(), any(), any())
        } answers {
            captured = arg(7)
            CreateHabitResult.Success(Habit(id = "h1", title = "Leer", iconKey = "book", colorIndex = 0, startDate = today))
        }
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.onTitleChanged("Leer")
        viewModel.onReminderToggled(true)
        viewModel.onReminderDayToggled(DayOfWeek.MONDAY)
        viewModel.onReminderToggled(false)

        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(captured)
        assertEquals(emptySet<DayOfWeek>(), captured)
    }
}
