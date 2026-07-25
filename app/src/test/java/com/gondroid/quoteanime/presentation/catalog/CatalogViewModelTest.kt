package com.gondroid.quoteanime.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.usecase.GetAllQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.GetFavoriteQuotesUseCase
import com.gondroid.quoteanime.domain.usecase.GetQuotesByCategoryUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleFavoriteUseCase
import com.gondroid.quoteanime.presentation.navigation.Screen
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [CatalogViewModel] — 3-view hub architecture.
 *
 * Views:
 *  - Selector  : selectedFilter == null
 *  - List      : selectedFilter != null, selectedQuote == null
 *  - Detail    : selectedQuote != null
 *
 * Scenarios covered:
 *  - Initial state without ARG → Selector (selectedFilter = null)
 *  - Initial state with ARG    → List pre-filtered by ByEmotion
 *  - onFilterSelected(Favorites) → loads favorites flow
 *  - onFilterSelected(All)       → loads all-quotes flow
 *  - onFilterSelected(ByEmotion) → loads by-category flow
 *  - onBackFromList()            → returns to Selector
 *  - onQuoteSelected()           → enters Detail view
 *  - onBackFromDetail()          → returns to List
 *  - selectedQuote is reactive   → toggling favorite updates Detail quote
 *  - onToggleFavorite            → delegates to ToggleFavoriteUseCase
 *  - isEmpty computed property
 *  - isLoading while filter transitions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getAllQuotes: GetAllQuotesUseCase
    private lateinit var getQuotesByCategory: GetQuotesByCategoryUseCase
    private lateinit var getFavoriteQuotes: GetFavoriteQuotesUseCase
    private lateinit var toggleFavorite: ToggleFavoriteUseCase

    private val narutoQuotes = listOf(
        Quote(id = "1", quote = "Believe it!", author = "Naruto", anime = "Naruto", isFavorite = false),
        Quote(id = "2", quote = "I never give up.", author = "Naruto", anime = "Naruto", isFavorite = true)
    )

    private val onePieceQuotes = listOf(
        Quote(id = "10", quote = "I will be King!", author = "Luffy", anime = "One Piece", isFavorite = false)
    )

    private val favoriteQuotes = listOf(
        Quote(id = "2", quote = "I never give up.", author = "Naruto", anime = "Naruto", isFavorite = true),
        Quote(id = "10", quote = "I will be King!", author = "Luffy", anime = "One Piece", isFavorite = true)
    )

    @Before
    fun setup() {
        getAllQuotes = mockk()
        getQuotesByCategory = mockk()
        getFavoriteQuotes = mockk()
        toggleFavorite = mockk()

        every { getAllQuotes() } returns flowOf(emptyList())
        every { getQuotesByCategory(any()) } returns flowOf(emptyList())
        every { getFavoriteQuotes() } returns flowOf(favoriteQuotes)
    }

    private fun buildViewModel(initialCategoryId: String? = null): CatalogViewModel {
        val savedStateHandle = SavedStateHandle(
            if (initialCategoryId != null) mapOf(Screen.Catalog.ARG to initialCategoryId) else emptyMap()
        )
        return CatalogViewModel(
            savedStateHandle, getAllQuotes, getQuotesByCategory, getFavoriteQuotes, toggleFavorite
        )
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state without ARG is Selector view - selectedFilter is null`() {
        val viewModel = buildViewModel()
        assertNull(viewModel.uiState.value.selectedFilter)
        assertNull(viewModel.uiState.value.selectedQuote)
    }

    @Test
    fun `initial state with ARG pre-selects ByEmotion filter`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel(initialCategoryId = "Naruto")

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            val filter = state.selectedFilter
            assertNotNull(filter)
            assertTrue(filter is CatalogFilter.ByEmotion)
            assertEquals("Naruto", (filter as CatalogFilter.ByEmotion).categoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Filter selection ──────────────────────────────────────────────────────

    @Test
    fun `onFilterSelected Favorites loads favorite quotes`() = runTest {
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.Favorites)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(CatalogFilter.Favorites, state.selectedFilter)
            assertEquals(2, state.quotes.size)
            assertTrue(state.quotes.all { it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onFilterSelected All loads all quotes`() = runTest {
        every { getAllQuotes() } returns flowOf(narutoQuotes + onePieceQuotes)

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.All)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(CatalogFilter.All, state.selectedFilter)
            assertEquals(3, state.quotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onFilterSelected ByEmotion loads quotes for that category`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.ByEmotion("Naruto", "Naruto"))
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.selectedFilter is CatalogFilter.ByEmotion)
            assertEquals(2, state.quotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching between categories updates quotes via flatMapLatest`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)
        every { getQuotesByCategory("One Piece") } returns flowOf(onePieceQuotes)

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.ByEmotion("Naruto", "Naruto"))
            advanceUntilIdle()
            assertEquals(2, expectMostRecentItem().quotes.size)

            viewModel.onFilterSelected(CatalogFilter.ByEmotion("One Piece", "One Piece"))
            advanceUntilIdle()
            assertEquals(1, expectMostRecentItem().quotes.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Back navigation ───────────────────────────────────────────────────────

    @Test
    fun `onBackFromList resets selectedFilter to null - returns to Selector`() = runTest {
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.Favorites)
            advanceUntilIdle()
            assertNotNull(expectMostRecentItem().selectedFilter)

            viewModel.onBackFromList()
            advanceUntilIdle()
            assertNull(expectMostRecentItem().selectedFilter)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Quote detail ──────────────────────────────────────────────────────────

    @Test
    fun `onQuoteSelected sets selectedQuote - enters Detail view`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.ByEmotion("Naruto", "Naruto"))
            advanceUntilIdle()

            viewModel.onQuoteSelected(narutoQuotes[0])
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertNotNull(state.selectedQuote)
            assertEquals("1", state.selectedQuote?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onBackFromDetail clears selectedQuote - returns to List`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.ByEmotion("Naruto", "Naruto"))
            advanceUntilIdle()
            viewModel.onQuoteSelected(narutoQuotes[0])
            advanceUntilIdle()
            assertNotNull(expectMostRecentItem().selectedQuote)

            viewModel.onBackFromDetail()
            advanceUntilIdle()
            assertNull(expectMostRecentItem().selectedQuote)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectedQuote updates reactively when favorite state changes in quotes list`() = runTest {
        val quotesFlow = MutableStateFlow(narutoQuotes)
        every { getQuotesByCategory("Naruto") } returns quotesFlow

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.ByEmotion("Naruto", "Naruto"))
            advanceUntilIdle()
            viewModel.onQuoteSelected(narutoQuotes[0])
            advanceUntilIdle()

            // Initially not a favorite
            assertFalse(expectMostRecentItem().selectedQuote?.isFavorite ?: true)

            // Simulate quote "1" becoming a favorite via an updated flow emission
            val updated = narutoQuotes.map { if (it.id == "1") it.copy(isFavorite = true) else it }
            quotesFlow.value = updated
            advanceUntilIdle()

            // selectedQuote must now reflect isFavorite = true
            assertTrue(expectMostRecentItem().selectedQuote?.isFavorite ?: false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── isEmpty computed property ─────────────────────────────────────────────

    @Test
    fun `isEmpty is true when not loading and quotes are empty`() = runTest {
        every { getQuotesByCategory("Unknown") } returns flowOf(emptyList())

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.ByEmotion("Unknown", "Unknown"))
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isEmpty is false when quotes are present`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.ByEmotion("Naruto", "Naruto"))
            advanceUntilIdle()

            assertFalse(expectMostRecentItem().isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onToggleFavorite ──────────────────────────────────────────────────────

    @Test
    fun `onToggleFavorite delegates to ToggleFavoriteUseCase`() = runTest {
        coJustRun { toggleFavorite(any()) }

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onToggleFavorite(narutoQuotes[0])
            advanceUntilIdle()

            coVerify(exactly = 1) { toggleFavorite(narutoQuotes[0]) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Reactive favorites update ─────────────────────────────────────────────

    @Test
    fun `when favorites flow emits new list, uiState quotes update`() = runTest {
        val favoritesFlow = MutableStateFlow(favoriteQuotes)
        every { getFavoriteQuotes() } returns favoritesFlow

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onFilterSelected(CatalogFilter.Favorites)
            advanceUntilIdle()
            assertEquals(2, expectMostRecentItem().quotes.size)

            val newFavorite = Quote(id = "99", quote = "Bankai!", author = "Ichigo", anime = "Bleach", isFavorite = true)
            favoritesFlow.value = favoriteQuotes + newFavorite
            advanceUntilIdle()

            assertEquals(3, expectMostRecentItem().quotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
