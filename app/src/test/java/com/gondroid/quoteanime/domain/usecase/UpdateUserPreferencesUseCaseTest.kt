package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.WidgetSize
import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Scenarios covered:
 *  - setCategories: delegates correct Set<String> to repository
 *  - setCategories with empty set: delegates empty set (means "all categories")
 *  - setNotificationsEnabled(true) and (false): delegates correct boolean
 *  - setNotificationTimeRange: delegates all four time parameters correctly
 *  - setFrequency: delegates correct timesPerDay value
 *  - setWidgetSize: delegates correct WidgetSize enum value
 *  - setWidgetUpdateTimesPerDay: delegates correct int
 */
class UpdateUserPreferencesUseCaseTest {

    private lateinit var repository: UserPreferencesRepository
    private lateinit var useCase: UpdateUserPreferencesUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = UpdateUserPreferencesUseCase(repository)
    }

    @Test
    fun `setCategories with non-empty set delegates to repository`() = runTest {
        val categoryIds = setOf("Naruto", "One Piece", "Bleach")
        coJustRun { repository.updateSelectedCategories(categoryIds) }

        useCase.setCategories(categoryIds)

        coVerify(exactly = 1) { repository.updateSelectedCategories(categoryIds) }
    }

    @Test
    fun `setCategories with empty set delegates empty set to repository`() = runTest {
        coJustRun { repository.updateSelectedCategories(emptySet()) }

        useCase.setCategories(emptySet())

        coVerify(exactly = 1) { repository.updateSelectedCategories(emptySet()) }
    }

    @Test
    fun `setNotificationsEnabled with true delegates true to repository`() = runTest {
        coJustRun { repository.updateNotificationsEnabled(true) }

        useCase.setNotificationsEnabled(true)

        coVerify(exactly = 1) { repository.updateNotificationsEnabled(true) }
    }

    @Test
    fun `setNotificationsEnabled with false delegates false to repository`() = runTest {
        coJustRun { repository.updateNotificationsEnabled(false) }

        useCase.setNotificationsEnabled(false)

        coVerify(exactly = 1) { repository.updateNotificationsEnabled(false) }
    }

    @Test
    fun `setNotificationTimeRange delegates all four time parameters to repository`() = runTest {
        coJustRun { repository.updateNotificationTimeRange(8, 30, 22, 0) }

        useCase.setNotificationTimeRange(
            startHour = 8, startMinute = 30,
            endHour = 22, endMinute = 0
        )

        coVerify(exactly = 1) { repository.updateNotificationTimeRange(8, 30, 22, 0) }
    }

    @Test
    fun `setNotificationTimeRange with midnight values delegates correctly`() = runTest {
        coJustRun { repository.updateNotificationTimeRange(0, 0, 0, 0) }

        useCase.setNotificationTimeRange(
            startHour = 0, startMinute = 0,
            endHour = 0, endMinute = 0
        )

        coVerify(exactly = 1) { repository.updateNotificationTimeRange(0, 0, 0, 0) }
    }

    @Test
    fun `setFrequency delegates timesPerDay to repository`() = runTest {
        coJustRun { repository.updateNotificationFrequency(3) }

        useCase.setFrequency(3)

        coVerify(exactly = 1) { repository.updateNotificationFrequency(3) }
    }

    @Test
    fun `setFrequency with boundary value of 1 delegates correctly`() = runTest {
        coJustRun { repository.updateNotificationFrequency(1) }

        useCase.setFrequency(1)

        coVerify(exactly = 1) { repository.updateNotificationFrequency(1) }
    }

    @Test
    fun `setFrequency with boundary value of 10 delegates correctly`() = runTest {
        coJustRun { repository.updateNotificationFrequency(10) }

        useCase.setFrequency(10)

        coVerify(exactly = 1) { repository.updateNotificationFrequency(10) }
    }

    @Test
    fun `setWidgetSize with SMALL delegates SMALL to repository`() = runTest {
        coJustRun { repository.updateWidgetSize(WidgetSize.SMALL) }

        useCase.setWidgetSize(WidgetSize.SMALL)

        coVerify(exactly = 1) { repository.updateWidgetSize(WidgetSize.SMALL) }
    }

    @Test
    fun `setWidgetSize with LARGE delegates LARGE to repository`() = runTest {
        coJustRun { repository.updateWidgetSize(WidgetSize.LARGE) }

        useCase.setWidgetSize(WidgetSize.LARGE)

        coVerify(exactly = 1) { repository.updateWidgetSize(WidgetSize.LARGE) }
    }

    @Test
    fun `setWidgetUpdateTimesPerDay delegates correct int to repository`() = runTest {
        coJustRun { repository.updateWidgetUpdateTimesPerDay(4) }

        useCase.setWidgetUpdateTimesPerDay(4)

        coVerify(exactly = 1) { repository.updateWidgetUpdateTimesPerDay(4) }
    }
}
