package com.gondroid.quoteanime.presentation.home

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.usecase.GetAllQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleFavoriteUseCase
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Scenarios covered:
 *  - Initial state: isLoading=true before data arrives
 *  - Quotes loaded: isLoading=false, quotes list populated
 *  - Error state: isLoading=false, error message set when flow throws
 *  - scrollToPage: set to correct index when widgetQuoteId matches a quote
 *  - scrollToPage null: when widgetQuoteId is null, scrollToPage stays null
 *  - scrollToPage not found: when widgetQuoteId doesn't match any quote, scrollToPage is null
 *  - onScrollToPageConsumed: clears scrollToPage
 *  - onToggleFavorite: delegates to ToggleFavoriteUseCase
 *  - Empty quotes: list is empty after loading
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getAllQuotes: GetAllQuotesUseCase
    private lateinit var toggleFavorite: ToggleFavoriteUseCase

    private val sampleQuotes = listOf(
        Quote(id = "1", quote = "Believe it!", author = "Naruto", anime = "Naruto", isFavorite = false),
        Quote(id = "2", quote = "I will be King!", author = "Luffy", anime = "One Piece", isFavorite = true),
        Quote(id = "3", quote = "Bankai!", author = "Ichigo", anime = "Bleach", isFavorite = false)
    )

    @Before
    fun setup() {
        getAllQuotes = mockk()
        toggleFavorite = mockk()
    }

    private fun buildViewModel(
        widgetQuoteId: String? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            if (widgetQuoteId != null) mapOf("quoteId" to widgetQuoteId) else emptyMap()
        )
    ): HomeViewModel = HomeViewModel(savedStateHandle, getAllQuotes, toggleFavorite)

    // ── Initial / loading state ───────────────────────────────────────────────

    @Test
    fun `initial state has isLoading true before quotes arrive`() = runTest {
        // Use a flow that never emits so we can observe the initial state
        every { getAllQuotes() } returns kotlinx.coroutines.flow.flow { kotlinx.coroutines.delay(Long.MAX_VALUE) }

        val viewModel = buildViewModel()

        assertEquals(true, viewModel.uiState.value.isLoading)
        assertEquals(emptyList<Quote>(), viewModel.uiState.value.quotes)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `when quotes load successfully, isLoading becomes false and quotes are populated`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.quotes.size)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when quotes flow throws, error message is set and isLoading is false`() = runTest {
        every { getAllQuotes() } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("Network failure")
        }

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network failure", state.error)
        assertTrue(state.quotes.isEmpty())
    }

    @Test
    fun `when quotes load with empty list, quotes is empty and isLoading is false`() = runTest {
        every { getAllQuotes() } returns flowOf(emptyList())

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.quotes.isEmpty())
        assertNull(state.error)
    }

    // ── scrollToPage (widget deep-link) ───────────────────────────────────────

    @Test
    fun `given widgetQuoteId matches second quote, when quotes load, scrollToPage is set to 1`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)

        val viewModel = buildViewModel(widgetQuoteId = "2")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.scrollToPage)
    }

    @Test
    fun `given widgetQuoteId matches first quote, when quotes load, scrollToPage is set to 0`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)

        val viewModel = buildViewModel(widgetQuoteId = "1")
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.scrollToPage)
    }

    @Test
    fun `given no widgetQuoteId, when quotes load, scrollToPage remains null`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)

        val viewModel = buildViewModel(widgetQuoteId = null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.scrollToPage)
    }

    @Test
    fun `given widgetQuoteId does not match any quote, when quotes load, scrollToPage is null`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)

        val viewModel = buildViewModel(widgetQuoteId = "999")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.scrollToPage)
    }

    // ── onScrollToPageConsumed ────────────────────────────────────────────────

    @Test
    fun `when onScrollToPageConsumed is called, scrollToPage becomes null`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)

        val viewModel = buildViewModel(widgetQuoteId = "2")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.scrollToPage)

        viewModel.onScrollToPageConsumed()

        assertNull(viewModel.uiState.value.scrollToPage)
    }

    @Test
    fun `calling onScrollToPageConsumed when scrollToPage is already null is idempotent`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.scrollToPage)
        viewModel.onScrollToPageConsumed()
        assertNull(viewModel.uiState.value.scrollToPage)
    }

    // ── onToggleFavorite ──────────────────────────────────────────────────────

    @Test
    fun `onToggleFavorite delegates to ToggleFavoriteUseCase`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)
        coJustRun { toggleFavorite(any()) }

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onToggleFavorite(sampleQuotes[0])
        advanceUntilIdle()

        coVerify(exactly = 1) { toggleFavorite(sampleQuotes[0]) }
    }

    @Test
    fun `onToggleFavorite can be called multiple times for different quotes`() = runTest {
        every { getAllQuotes() } returns flowOf(sampleQuotes)
        coJustRun { toggleFavorite(any()) }

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onToggleFavorite(sampleQuotes[0])
        viewModel.onToggleFavorite(sampleQuotes[1])
        advanceUntilIdle()

        coVerify(exactly = 1) { toggleFavorite(sampleQuotes[0]) }
        coVerify(exactly = 1) { toggleFavorite(sampleQuotes[1]) }
    }

    // ── Reactive flow updates ─────────────────────────────────────────────────

    @Test
    fun `when quotes flow emits new values, uiState reflects latest emission`() = runTest {
        val quotesFlow = MutableStateFlow(sampleQuotes)
        every { getAllQuotes() } returns quotesFlow

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.quotes.size)

        // Simulate new data from Firestore
        val updatedQuotes = sampleQuotes + Quote(
            id = "4", quote = "Plus Ultra!", author = "All Might", anime = "MHA", isFavorite = false
        )
        quotesFlow.value = updatedQuotes
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.quotes.size)
    }
}
