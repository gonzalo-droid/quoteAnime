package com.gondroid.quoteanime.presentation.settings

import android.content.Context
import app.cash.turbine.test
import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.model.WidgetSize
import com.gondroid.quoteanime.domain.usecase.GetCategoriesUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
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
 *  - Initial state: populated from combine(getCategories, getUserPreferences)
 *  - onCategoryToggled: adds category to set, persists, reschedules if notifications enabled
 *  - onCategoryToggled: removes category if already present (toggle behavior)
 *  - onSelectAllCategories: empties selectedCategoryIds set, persists
 *  - allCategoriesSelected: true when set is empty
 *  - onNotificationsEnabled: persists true, schedules worker
 *  - onNotificationsDisabled: persists false, cancels worker (does NOT reschedule)
 *  - onTimeRangeChanged: persists all 4 params and reschedules
 *  - onFrequencyChanged: persists frequency and reschedules
 *  - onPermissionDeniedPermanently: sets permissionDeniedPermanently = true in state
 *  - onWidgetSizeChanged: persists size and triggers immediate widget update
 *  - onWidgetUpdateTimesChanged: persists times and reschedules widget
 *  - Race condition: reschedule uses the new value, not the old reactive state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var getCategories: GetCategoriesUseCase
    private lateinit var getUserPreferences: GetUserPreferencesUseCase
    private lateinit var updatePreferences: UpdateUserPreferencesUseCase
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var widgetScheduler: WidgetScheduler

    private val allCategories = listOf(
        Category(id = "Naruto", name = "Naruto"),
        Category(id = "One Piece", name = "One Piece"),
        Category(id = "Bleach", name = "Bleach")
    )

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
        getCategories = mockk()
        getUserPreferences = mockk()
        updatePreferences = mockk()
        notificationScheduler = mockk()
        widgetScheduler = mockk()

        // Default stubs
        every { getCategories() } returns flowOf(allCategories)
        every { getUserPreferences() } returns flowOf(defaultPrefs)
        coJustRun { updatePreferences.setCategories(any()) }
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
        context, getCategories, getUserPreferences, updatePreferences, notificationScheduler, widgetScheduler
    )

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is populated from categories and user preferences`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.categories.size)
            assertEquals(setOf("Naruto"), state.selectedCategoryIds)
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

    // ── Category toggle ───────────────────────────────────────────────────────

    @Test
    fun `onCategoryToggled with new id adds it to selectedCategoryIds and persists`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onCategoryToggled("One Piece")
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setCategories(setOf("Naruto", "One Piece")) }
    }

    @Test
    fun `onCategoryToggled with existing id removes it and persists`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        // "Naruto" is initially selected — toggling it should remove it
        viewModel.onCategoryToggled("Naruto")
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setCategories(emptySet()) }
    }

    @Test
    fun `onCategoryToggled does NOT schedule notifications when notifications are disabled`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onCategoryToggled("One Piece")
        advanceUntilIdle()

        verify(exactly = 0) { notificationScheduler.schedule(any()) }
    }

    @Test
    fun `onCategoryToggled reschedules notifications when notifications are enabled`() = runTest {
        every { getUserPreferences() } returns flowOf(prefsWithNotificationsEnabled)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onCategoryToggled("One Piece")
        advanceUntilIdle()

        verify(exactly = 1) { notificationScheduler.schedule(any()) }
    }

    // ── Select all categories ─────────────────────────────────────────────────

    @Test
    fun `onSelectAllCategories empties selectedCategoryIds and persists empty set`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onSelectAllCategories()
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreferences.setCategories(emptySet()) }
    }

    @Test
    fun `allCategoriesSelected is true when selectedCategoryIds is empty`() = runTest {
        every { getUserPreferences() } returns flowOf(defaultPrefs.copy(selectedCategoryIds = emptySet()))

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertTrue(awaitItem().allCategoriesSelected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `allCategoriesSelected is false when selectedCategoryIds is not empty`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertFalse(awaitItem().allCategoriesSelected)
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
}
