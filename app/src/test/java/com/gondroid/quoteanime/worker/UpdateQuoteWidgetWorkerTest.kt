package com.gondroid.quoteanime.worker

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.widget.QuoteWidget
import com.gondroid.quoteanime.widget.QuoteWidgetState
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The quote widget follows the Settings anime selection, matched on `anime`. Glance is faked at
 * its edges (widget ids, state store, redraw) and the state the worker writes is inspected.
 *
 * Scenarios covered:
 *  - A selection of animes: the widget only shows quotes from them, whatever their emotions
 *  - A stale selection of emotions: the widget shows a quote (from any anime), not the error
 */
class UpdateQuoteWidgetWorkerTest {

    private val written = mutableListOf<Preferences>()

    @Before
    fun setup() {
        // GlanceAppWidgetManager reads the platform manager in its constructor.
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns mockk(relaxed = true)
        mockkConstructor(GlanceAppWidgetManager::class)
        coEvery { anyConstructed<GlanceAppWidgetManager>().getGlanceIds(QuoteWidget::class.java) } returns
            listOf(mockk<GlanceId>())

        mockkStatic("androidx.glance.appwidget.state.GlanceAppWidgetStateKt")
        coEvery {
            updateAppWidgetState(any(), any<GlanceStateDefinition<Preferences>>(), any(), any())
        } coAnswers {
            val update = arg<suspend (Preferences) -> Preferences>(3)
            update(emptyPreferences()).also { written += it }
        }

        mockkConstructor(QuoteWidget::class)
        coJustRun { anyConstructed<QuoteWidget>().update(any(), any()) }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun worker(selection: Set<String>): UpdateQuoteWidgetWorker {
        val getUserPreferences = mockk<GetUserPreferencesUseCase>()
        every { getUserPreferences() } returns flowOf(UserPreferences(selectedCategoryIds = selection))
        val params = mockk<WorkerParameters>(relaxed = true)
        every { params.runAttemptCount } returns 0
        return UpdateQuoteWidgetWorker(
            mockk<Context>(relaxed = true), params, WorkerQuoteFixtures.realGetRandomQuote(), getUserPreferences
        )
    }

    @Test
    fun `given an anime selection, then the widget only shows quotes from those animes`() = runTest {
        val worker = worker(setOf("Bleach", "One Piece"))

        repeat(30) { assertEquals(ListenableWorker.Result.success(), worker.doWork()) }

        assertEquals(30, written.size)
        assertTrue(written.all { it[QuoteWidgetState.HAS_ERROR] == false })
        assertEquals(setOf("Bleach", "One Piece"), written.map { it[QuoteWidgetState.QUOTE_ANIME] }.toSet())
    }

    @Test
    fun `given a stale selection of emotions, then the widget shows a quote instead of the error`() = runTest {
        val worker = worker(setOf("motivación"))

        repeat(60) { assertEquals(ListenableWorker.Result.success(), worker.doWork()) }

        assertTrue(written.all { it[QuoteWidgetState.HAS_ERROR] == false })
        assertEquals(setOf("Naruto", "One Piece", "Bleach"), written.map { it[QuoteWidgetState.QUOTE_ANIME] }.toSet())
    }
}
