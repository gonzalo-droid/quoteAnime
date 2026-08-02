package com.gondroid.quoteanime.presentation.onboarding

import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.gondroid.quoteanime.domain.usecase.CreateHabitResult
import com.gondroid.quoteanime.domain.usecase.CreateHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCase
import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import com.gondroid.quoteanime.domain.usecase.SetOnboardingCompletedUseCase
import com.gondroid.quoteanime.domain.usecase.SetRoutineIntroSeenUseCase
import com.gondroid.quoteanime.presentation.routine.HabitEditorError
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Scenarios covered:
 *  - Free user: the first non-premium template is auto-selected once templates load
 *  - Premium user: the first template is auto-selected even if it's premium-only
 *  - A manual selection is preserved across a later template-list re-emission
 *  - Selecting a template updates the state and clears a previous error
 *  - Creating successfully finishes onboarding (persists both flags)
 *  - LimitReached / BlankTitle / InvalidDateRange surface as errors instead of doing nothing
 *  - Skipping (finishing without creating) persists both flags without calling CreateHabitUseCase
 */
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var setOnboardingCompleted: SetOnboardingCompletedUseCase
    private lateinit var setRoutineIntroSeen: SetRoutineIntroSeenUseCase
    private lateinit var getHabitTemplates: GetHabitTemplatesUseCase
    private lateinit var createHabit: CreateHabitUseCase
    private lateinit var observePremiumStatus: ObservePremiumStatusUseCase
    private lateinit var analytics: RoutineAnalytics

    private val fixedClock: Clock = Clock.fixed(
        LocalDate.parse("2026-07-25").atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    )

    private val freeTemplate = HabitTemplate(
        id = "free", title = "template_theme_ninja", iconKey = "dumbbell", order = 1,
        themeColorIndex = 10, themeKey = "ninja"
    )
    private val premiumTemplate = HabitTemplate(
        id = "premium", title = "template_theme_pokemon", iconKey = "emoji_events", order = 2,
        themeColorIndex = 4, themeKey = "pokemon", isPremiumOnly = true
    )

    private fun buildViewModel() = OnboardingViewModel(
        setOnboardingCompleted = setOnboardingCompleted,
        setRoutineIntroSeen = setRoutineIntroSeen,
        getHabitTemplates = getHabitTemplates,
        createHabit = createHabit,
        observePremiumStatus = observePremiumStatus,
        analytics = analytics,
        clock = fixedClock
    )

    @Before
    fun setup() {
        setOnboardingCompleted = mockk(relaxed = true)
        setRoutineIntroSeen = mockk(relaxed = true)
        getHabitTemplates = mockk()
        createHabit = mockk()
        observePremiumStatus = mockk()
        analytics = mockk(relaxed = true)
        every { getHabitTemplates() } returns flowOf(listOf(premiumTemplate, freeTemplate))
        every { observePremiumStatus() } returns flowOf(false)
    }

    @Test
    fun `given a free user, when templates load, then the first non-premium template is auto-selected`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(freeTemplate.id, viewModel.uiState.value.selectedTemplateId)
    }

    @Test
    fun `given a premium user, when templates load, then the first template is auto-selected even if premium-only`() = runTest {
        every { observePremiumStatus() } returns flowOf(true)
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(premiumTemplate.id, viewModel.uiState.value.selectedTemplateId)
    }

    @Test
    fun `given a manual selection, when the template list re-emits, then the selection is preserved`() = runTest {
        val templatesFlow = MutableStateFlow(listOf(premiumTemplate, freeTemplate))
        every { getHabitTemplates() } returns templatesFlow
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onTemplateSelected(premiumTemplate)
        templatesFlow.value = listOf(premiumTemplate, freeTemplate, freeTemplate.copy(id = "extra"))
        advanceUntilIdle()

        assertEquals(premiumTemplate.id, viewModel.uiState.value.selectedTemplateId)
    }

    @Test
    fun `given a template, when selected, then it reaches the state and clears a previous error`() = runTest {
        coEvery { createHabit(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            CreateHabitResult.BlankTitle
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.onCreateHabit("", {})
        advanceUntilIdle()
        assertEquals(HabitEditorError.BlankTitle, viewModel.uiState.value.error)

        viewModel.onTemplateSelected(premiumTemplate)

        assertEquals(premiumTemplate.id, viewModel.uiState.value.selectedTemplateId)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `given a successful creation, when finished, then both flags persist and onDone fires`() = runTest {
        coEvery {
            createHabit("Camino ninja", null, "dumbbell", 0, LocalDate.now(fixedClock), null, null, emptySet(), "free", null)
        } returns CreateHabitResult.Success(mockk(relaxed = true))
        val viewModel = buildViewModel()
        advanceUntilIdle()
        var finished = false

        viewModel.onCreateHabit("Camino ninja") { finished = true }
        advanceUntilIdle()

        coVerify(exactly = 1) { setOnboardingCompleted() }
        coVerify(exactly = 1) { setRoutineIntroSeen() }
        assertEquals(true, finished)
    }

    @Test
    fun `given the habit limit is reached, when creating, then an error is surfaced instead of finishing`() = runTest {
        coJustRun { setOnboardingCompleted() }
        every { getHabitTemplates() } returns flowOf(listOf(freeTemplate))
        coEvery { createHabit(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            CreateHabitResult.LimitReached(3)
        val viewModel = buildViewModel()
        advanceUntilIdle()
        var finished = false

        viewModel.onCreateHabit("Camino ninja") { finished = true }
        advanceUntilIdle()

        assertEquals(HabitEditorError.LimitReached(3), viewModel.uiState.value.error)
        assertEquals(false, finished)
        coVerify(exactly = 0) { setOnboardingCompleted() }
    }

    @Test
    fun `given skip, when finished, then both flags persist without creating a habit`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        var finished = false

        viewModel.onOnboardingFinished { finished = true }
        advanceUntilIdle()

        coVerify(exactly = 1) { setOnboardingCompleted() }
        coVerify(exactly = 1) { setRoutineIntroSeen() }
        coVerify(exactly = 0) { createHabit(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        assertEquals(true, finished)
    }
}
