package com.gondroid.quoteanime.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateUserPreferencesUseCase
import com.gondroid.quoteanime.notification.NotificationHelper
import com.gondroid.quoteanime.notification.NotificationScheduler
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The quote notification follows the Settings anime selection, matched on `anime`.
 *
 * Scenarios covered:
 *  - A selection of animes: every notification comes from them, whatever their emotions
 *  - A stale selection of emotions: a notification is still delivered, from any anime
 *  - An empty selection: any anime
 */
class QuoteNotificationWorkerTest {

    private lateinit var notificationHelper: NotificationHelper
    private val shown = mutableListOf<Quote>()

    @Before
    fun setup() {
        notificationHelper = mockk()
        every { notificationHelper.showQuoteNotification(any()) } answers { shown += firstArg<Quote>() }
    }

    private fun worker(selection: Set<String>): QuoteNotificationWorker {
        // Start == end is a 24-hour window, so the run is always inside it.
        val prefs = UserPreferences(
            selectedCategoryIds = selection,
            notificationsEnabled = true,
            notificationStartHour = 0, notificationStartMinute = 0,
            notificationEndHour = 0, notificationEndMinute = 0
        )
        val getUserPreferences = mockk<GetUserPreferencesUseCase>()
        every { getUserPreferences() } returns flowOf(prefs)
        val updatePreferences = mockk<UpdateUserPreferencesUseCase>()
        coJustRun { updatePreferences.setLastNotificationQuoteId(any()) }
        val scheduler = mockk<NotificationScheduler>()
        justRun { scheduler.schedule(any()) }
        val params = mockk<WorkerParameters>(relaxed = true)
        every { params.runAttemptCount } returns 0

        return QuoteNotificationWorker(
            mockk<Context>(relaxed = true), params,
            WorkerQuoteFixtures.realGetRandomQuote(), getUserPreferences, updatePreferences,
            notificationHelper, scheduler
        )
    }

    @Test
    fun `given an anime selection, then every notification comes from those animes`() = runTest {
        val worker = worker(setOf("Naruto"))

        repeat(30) { assertEquals(ListenableWorker.Result.success(), worker.doWork()) }

        assertEquals(30, shown.size)
        assertTrue(shown.all { it.anime == "Naruto" })
    }

    @Test
    fun `given a stale selection of emotions, then a notification is still delivered from any anime`() = runTest {
        val worker = worker(setOf("motivación"))

        repeat(60) { assertEquals(ListenableWorker.Result.success(), worker.doWork()) }

        assertEquals(setOf("Naruto", "One Piece", "Bleach"), shown.map { it.anime }.toSet())
    }

    @Test
    fun `given no selection, then notifications come from every anime`() = runTest {
        val worker = worker(emptySet())

        repeat(60) { worker.doWork() }

        assertEquals(setOf("Naruto", "One Piece", "Bleach"), shown.map { it.anime }.toSet())
    }
}
