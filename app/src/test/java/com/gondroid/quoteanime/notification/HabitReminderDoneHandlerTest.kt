package com.gondroid.quoteanime.notification

import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Scenarios covered:
 *  - "Done" writes today's completion and then refreshes the Mi Rutina widgets
 *  - An already-completed day is not written twice, and the widgets are still refreshed
 *  - A habit past its end date is not marked
 */
class HabitReminderDoneHandlerTest {

    private val today = LocalDate.parse("2026-09-18")
    private val fixedClock: Clock = Clock.fixed(
        today.atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    )

    private lateinit var repository: HabitRepository
    private lateinit var analytics: RoutineAnalytics
    private lateinit var routineWidgetScheduler: RoutineWidgetScheduler
    private lateinit var handler: HabitReminderDoneHandler

    private val habit = Habit(
        id = "h1",
        title = "Leer",
        iconKey = "book",
        colorIndex = 0,
        startDate = today.minusDays(10)
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        analytics = mockk(relaxed = true)
        routineWidgetScheduler = mockk(relaxed = true)
        handler = HabitReminderDoneHandler(repository, analytics, routineWidgetScheduler, fixedClock)
    }

    @Test
    fun `given an unmarked day, when Done is tapped, then the completion is written and the widgets are refreshed`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        coEvery { repository.isCompleted("h1", today) } returns false

        handler.markDone("h1")

        coVerifyOrder {
            repository.setCompletion("h1", today, true)
            routineWidgetScheduler.triggerImmediateUpdate()
        }
        verify(exactly = 1) {
            analytics.trackHabitCompleted("h1", false, RoutineAnalytics.SOURCE_NOTIFICATION)
        }
    }

    @Test
    fun `given the day was already marked from the app, when Done is tapped, then nothing is rewritten and the widgets are still refreshed`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        coEvery { repository.isCompleted("h1", today) } returns true

        handler.markDone("h1")

        coVerify(exactly = 0) { repository.setCompletion(any(), any(), any()) }
        verify(exactly = 1) { routineWidgetScheduler.triggerImmediateUpdate() }
    }

    @Test
    fun `given a habit that already ended, when Done is tapped, then the day is not marked`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit.copy(endDate = today.minusDays(1))

        handler.markDone("h1")

        coVerify(exactly = 0) { repository.setCompletion(any(), any(), any()) }
        verify(exactly = 0) { analytics.trackHabitCompleted(any(), any(), any()) }
    }
}
