package com.gondroid.quoteanime.presentation.settings

import android.content.Context
import app.cash.turbine.test
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.model.WidgetSize
import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import com.gondroid.quoteanime.domain.usecase.GetAnimesUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import com.gondroid.quoteanime.domain.usecase.ReconcileAnimeSelectionUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateUserPreferencesUseCase
import com.gondroid.quoteanime.notification.NotificationScheduler
import com.gondroid.quoteanime.notification.WidgetScheduler
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Scenarios covered:
 *  - Initial state: populated from combine(getAnimes, getUserPreferences)
 *  - onAnimeToggled: adds the anime to the set, persists, reschedules if notifications enabled
 *  - onAnimeToggled: removes the anime if already present (toggle behavior)
 *  - onSelectAllAnimes: empties selectedAnimes set, persists
 *  - allAnimesSelected: true when set is empty
 *  - onNotificationsEnabled: persists true, schedules worker
 *  - onNotificationsDisabled: persists false, cancels worker (does NOT reschedule)
 *  - onTimeRangeChanged: persists all 4 params and reschedules
 *  - onFrequencyChanged: persists frequency and reschedules
 *  - onPermissionDeniedPermanently: sets permissionDeniedPermanently = true in state
 *  - onWidgetSizeChanged: persists size and triggers immediate widget update
 *  - onWidgetUpdateTimesChanged: persists times and reschedules widget
 *  - Race condition: reschedule uses the new value, not the old reactive state
 *  - Anime list still loading (RTDB offline): the rest of the screen is not blocked
 *  - Anime list fails: empty list, the screen keeps working
 *  - Toggles show at once and stack (two quick taps keep both animes)
 *  - Stale selection (emotions saved by the old selector): all stale → cleared; mixed → only
 *    the animes; left alone while the anime list is unknown
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var getAnimes: GetAnimesUseCase
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var getUserPreferences: GetUserPreferencesUseCase
    private lateinit var updatePreferences: UpdateUserPreferencesUseCase
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var widgetScheduler: WidgetScheduler
    private lateinit var observePremiumStatus: ObservePremiumStatusUseCase

    private val allAnimes = listOf("Bleach", "Naruto", "One Piece")

    private val defaultPrefs = UserPreferences(
        selectedCategoryIds = setOf("Naruto"),
        notificationsEnabled = false,
        notificationStartHour = 8,
        notificationStartMinute = 0,
        notificationEndHour = 22,
        notificationEndMinute = 0,
        notificationFrequency = 1,
        widgetSize = WidgetSize.MEDIUM,
        widgetUpdateTimesPerDay = 2
    )

    private val prefsWithNotificationsEnabled = defaultPrefs.copy(notificationsEnabled = true)

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        getAnimes = mockk()
        preferencesRepository = mockk()
        coJustRun { preferencesRepository.updateSelectedCategories(any()) }
        getUserPreferences = mockk()
        updatePreferences = mockk()
        notificationScheduler = mockk()
        widgetScheduler = mockk()
        observePremiumStatus = mockk()

        // Default stubs
        every { getAnimes() } returns flowOf(allAnimes)
        every { getUserPreferences() } returns flowOf(defaultPrefs)
        every { observePremiumStatus() } returns flowOf(false)
        coJustRun { updatePreferences.setSelectedAnimes(any()) }
        coJustRun { updatePreferences.setNotificationsEnabled(any()) }
        coJustRun { updatePreferences.setNotificationTimeRange(any(), any(), any(), any()) }
        coJustRun { updatePreferences.setFrequency(any()) }
        coJustRun { updatePreferences.setWidgetSize(any()) }
        coJustRun { updatePreferences.setWidgetUpdateTimesPerDay(any()) }
        justRun { notificationScheduler.schedule(any()) }
        justRun { notificationScheduler.cancel() }
        justRun { widgetScheduler.schedule(any()) }
        justRun { widgetScheduler.triggerImmediateUpdate() }
    }

    private fun buildViewModel() = SettingsViewModel(
        context, getAnimes, getUserPreferences, updatePreferences, notificationScheduler, widgetScheduler,
        observePremiumStatus, ReconcileAnimeSelectionUseCase(preferencesRepository)
    )

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is populated from animes and user preferences`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.animes.size)
            assertEquals(setOf("Naruto"), state.selectedAnimes)
            assertFalse(state.notificationsEnabled)
            assertEquals(8, state.notificationStartHour)
            assertEquals(0, state.notificationStartMinute)
            assertEquals(22, state.notificationEndHour)
            assertEquals(1, state.notificationFrequency)
            assertEquals(WidgetSize.MEDIUM, state.widgetSize)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Anime toggle ───────────────────────────────────────────────────────

    @Test
    fun `onAnimeToggled with new id adds it to selectedAnimes and persists`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onAnimeToggled("One Piece")
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setSelectedAnimes(setOf("Naruto", "One Piece")) }
    }

    @Test
    fun `onAnimeToggled with existing id removes it and persists`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        // "Naruto" is initially selected — toggling it should remove it
        viewModel.onAnimeToggled("Naruto")
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setSelectedAnimes(emptySet()) }
    }

    @Test
    fun `onAnimeToggled does NOT schedule notifications when notifications are disabled`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onAnimeToggled("One Piece")
        advanceUntilIdle()

        verify(exactly = 0) { notificationScheduler.schedule(any()) }
    }

    @Test
    fun `onAnimeToggled reschedules notifications when notifications are enabled`() = runTest {
        every { getUserPreferences() } returns flowOf(prefsWithNotificationsEnabled)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onAnimeToggled("One Piece")
        advanceUntilIdle()

        verify(exactly = 1) { notificationScheduler.schedule(any()) }
    }

    // ── Select all animes ─────────────────────────────────────────────────

    @Test
    fun `onSelectAllAnimes empties selectedAnimes and persists empty set`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onSelectAllAnimes()
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setSelectedAnimes(emptySet()) }
    }

    @Test
    fun `allAnimesSelected is true when selectedAnimes is empty`() = runTest {
        every { getUserPreferences() } returns flowOf(defaultPrefs.copy(selectedCategoryIds = emptySet()))

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertTrue(awaitItem().allAnimesSelected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `allAnimesSelected is false when selectedAnimes is not empty`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertFalse(awaitItem().allAnimesSelected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Notifications enable/disable ──────────────────────────────────────────

    @Test
    fun `onNotificationsEnabled persists true and schedules notification worker`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onNotificationsEnabled()
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setNotificationsEnabled(true) }
        verify(exactly = 1) { notificationScheduler.schedule(any()) }
    }

    @Test
    fun `onNotificationsEnabled schedules with state that has notificationsEnabled=true`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onNotificationsEnabled()
        advanceUntilIdle()

        // The schedule call must use a UserPreferences with notificationsEnabled=true
        // (this verifies the race condition fix: we pass the new state directly, not waiting for the flow)
        verify {
            notificationScheduler.schedule(
                match { prefs -> prefs.notificationsEnabled }
            )
        }
    }

    @Test
    fun `onNotificationsDisabled persists false and cancels worker without rescheduling`() = runTest {
        every { getUserPreferences() } returns flowOf(prefsWithNotificationsEnabled)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onNotificationsDisabled()
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setNotificationsEnabled(false) }
        verify(exactly = 1) { notificationScheduler.cancel() }
        verify(exactly = 0) { notificationScheduler.schedule(any()) }
    }

    // ── Time range change ─────────────────────────────────────────────────────

    @Test
    fun `onTimeRangeChanged persists all four parameters and reschedules if notifications enabled`() = runTest {
        every { getUserPreferences() } returns flowOf(prefsWithNotificationsEnabled)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onTimeRangeChanged(
            startHour = 9, startMinute = 30,
            endHour = 21, endMinute = 0
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setNotificationTimeRange(9, 30, 21, 0) }
        verify(exactly = 1) { notificationScheduler.schedule(any()) }
    }

    @Test
    fun `onTimeRangeChanged does NOT reschedule if notifications are disabled`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onTimeRangeChanged(9, 30, 21, 0)
        advanceUntilIdle()

        verify(exactly = 0) { notificationScheduler.schedule(any()) }
    }

    @Test
    fun `onTimeRangeChanged schedules with updated time values`() = runTest {
        every { getUserPreferences() } returns flowOf(prefsWithNotificationsEnabled)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onTimeRangeChanged(
            startHour = 7, startMinute = 15,
            endHour = 20, endMinute = 45
        )
        advanceUntilIdle()

        verify {
            notificationScheduler.schedule(
                match { prefs ->
                    prefs.notificationStartHour == 7 &&
                    prefs.notificationStartMinute == 15 &&
                    prefs.notificationEndHour == 20 &&
                    prefs.notificationEndMinute == 45
                }
            )
        }
    }

    // ── Frequency change ──────────────────────────────────────────────────────

    @Test
    fun `onFrequencyChanged persists frequency and reschedules if notifications enabled`() = runTest {
        every { getUserPreferences() } returns flowOf(prefsWithNotificationsEnabled)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onFrequencyChanged(3)
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setFrequency(3) }
        verify(exactly = 1) { notificationScheduler.schedule(any()) }
    }

    @Test
    fun `onFrequencyChanged schedules with the new frequency value`() = runTest {
        every { getUserPreferences() } returns flowOf(prefsWithNotificationsEnabled)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onFrequencyChanged(5)
        advanceUntilIdle()

        verify {
            notificationScheduler.schedule(
                match { prefs -> prefs.notificationFrequency == 5 }
            )
        }
    }

    @Test
    fun `onFrequencyChanged does NOT reschedule if notifications are disabled`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onFrequencyChanged(5)
        advanceUntilIdle()

        verify(exactly = 0) { notificationScheduler.schedule(any()) }
    }

    // ── Permission denied ─────────────────────────────────────────────────────

    @Test
    fun `onPermissionDeniedPermanently sets permissionDeniedPermanently to true`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.permissionDeniedPermanently)

        viewModel.onPermissionDeniedPermanently()

        assertTrue(viewModel.uiState.value.permissionDeniedPermanently)
    }

    @Test
    fun `onPermissionDeniedPermanently is idempotent when called multiple times`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onPermissionDeniedPermanently()
        viewModel.onPermissionDeniedPermanently()

        assertTrue(viewModel.uiState.value.permissionDeniedPermanently)
    }

    // ── Widget settings ───────────────────────────────────────────────────────

    @Test
    fun `onWidgetSizeChanged persists size and triggers immediate widget update`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onWidgetSizeChanged(WidgetSize.LARGE)
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setWidgetSize(WidgetSize.LARGE) }
        verify(exactly = 1) { widgetScheduler.triggerImmediateUpdate() }
    }

    @Test
    fun `onWidgetUpdateTimesChanged persists times and schedules widget`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onWidgetUpdateTimesChanged(4)
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setWidgetUpdateTimesPerDay(4) }
        verify(exactly = 1) { widgetScheduler.schedule(4) }
    }

    // ── Anime list independence ───────────────────────────────────────────────

    @Test
    fun `given the anime list never arrives, then the screen still loads with the section loading`() = runTest {
        every { getAnimes() } returns kotlinx.coroutines.flow.flow { kotlinx.coroutines.awaitCancellation() }

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.animesLoading)
        assertTrue(state.animes.isEmpty())
        assertEquals(setOf("Naruto"), state.selectedAnimes)
    }

    @Test
    fun `given the anime list fails, then the section is empty and the screen keeps working`() = runTest {
        every { getAnimes() } returns kotlinx.coroutines.flow.flow { throw RuntimeException("offline") }

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.animesLoading)
        assertTrue(state.animes.isEmpty())
    }

    @Test
    fun `given the anime list arrives, then animesLoading is false`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.animesLoading)
        assertEquals(3, viewModel.uiState.value.animes.size)
    }

    @Test
    fun `given two quick toggles before DataStore echoes, then both animes stay selected`() = runTest {
        // defaultPrefs never re-emits, like a DataStore that hasn't written yet.
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onAnimeToggled("One Piece")
        viewModel.onAnimeToggled("Bleach")

        assertEquals(setOf("Naruto", "One Piece", "Bleach"), viewModel.uiState.value.selectedAnimes)
        advanceUntilIdle()
        coVerify { updatePreferences.setSelectedAnimes(setOf("Naruto", "One Piece", "Bleach")) }
    }

    @Test
    fun `given a selection, when choosing all, then the state is empty at once`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onSelectAllAnimes()

        assertTrue(viewModel.uiState.value.allAnimesSelected)
    }

    // ── Stale selection saved by the selector that listed emotions ────────────

    @Test
    fun `given the anime list, then it holds the anime names`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(allAnimes, viewModel.uiState.value.animes)
    }

    @Test
    fun `given only emotions were saved, when the anime list arrives, then the selection is cleared and saved`() = runTest {
        every { getUserPreferences() } returns flowOf(defaultPrefs.copy(selectedCategoryIds = setOf("motivación", "reflexión")))

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.allAnimesSelected)
        coVerify(exactly = 1) { preferencesRepository.updateSelectedCategories(emptySet()) }
    }

    @Test
    fun `given animes and emotions were saved, when the anime list arrives, then only the animes stay selected`() = runTest {
        every { getUserPreferences() } returns flowOf(defaultPrefs.copy(selectedCategoryIds = setOf("Naruto", "motivación")))

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(setOf("Naruto"), viewModel.uiState.value.selectedAnimes)
        coVerify(exactly = 1) { preferencesRepository.updateSelectedCategories(setOf("Naruto")) }
    }

    @Test
    fun `given the anime list never arrives, then a stale selection is not touched`() = runTest {
        every { getUserPreferences() } returns flowOf(defaultPrefs.copy(selectedCategoryIds = setOf("motivación")))
        every { getAnimes() } returns kotlinx.coroutines.flow.flow { kotlinx.coroutines.awaitCancellation() }

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(setOf("motivación"), viewModel.uiState.value.selectedAnimes)
        coVerify(exactly = 0) { preferencesRepository.updateSelectedCategories(any()) }
    }

    @Test
    fun `given a valid selection, then nothing is rewritten`() = runTest {
        buildViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { preferencesRepository.updateSelectedCategories(any()) }
    }
}
