package com.gondroid.quoteanime.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.usecase.GetCategoriesUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Scenarios covered:
 *  - Initial state: isLoading=true, empty categories/quotes
 *  - Initial category from SavedStateHandle: correct category pre-selected
 *  - Null SavedStateHandle (favorites tab): selectedCategoryId is null
 *  - onCategorySelected: switching to a category id loads that category's quotes via flatMapLatest
 *  - onCategorySelected null: switching to null loads favorites
 *  - Categories list is populated from GetCategoriesUseCase
 *  - isFavoritesTab computed property: true when selectedCategoryId is null
 *  - isEmpty computed property: true when not loading and quotes are empty
 *  - onToggleFavorite: delegates to ToggleFavoriteUseCase
 *  - Reactive updates: favorites flow re-emission updates uiState
 *
 * NOTE: CatalogViewModel.uiState uses SharingStarted.WhileSubscribed(5_000) via stateIn.
 * The upstream only starts collecting when there is an active subscriber. Therefore tests
 * must subscribe first (entering the Turbine .test {} block) and then call advanceUntilIdle()
 * inside the block to drive coroutine execution. advanceUntilIdle() called before .test {}
 * has no effect because no subscriber exists yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getCategories: GetCategoriesUseCase
    private lateinit var getQuotesByCategory: GetQuotesByCategoryUseCase
    private lateinit var getFavoriteQuotes: GetFavoriteQuotesUseCase
    private lateinit var toggleFavorite: ToggleFavoriteUseCase

    private val categories = listOf(
        Category(id = "Naruto", name = "Naruto"),
        Category(id = "One Piece", name = "One Piece"),
        Category(id = "Bleach", name = "Bleach")
    )

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
        getCategories = mockk()
        getQuotesByCategory = mockk()
        getFavoriteQuotes = mockk()
        toggleFavorite = mockk()

        // Default stubs
        every { getCategories() } returns flowOf(categories)
        every { getQuotesByCategory(any()) } returns flowOf(emptyList())
        every { getFavoriteQuotes() } returns flowOf(favoriteQuotes)
    }

    private fun buildViewModel(initialCategoryId: String? = null): CatalogViewModel {
        val savedStateHandle = SavedStateHandle(
            if (initialCategoryId != null) mapOf(Screen.Catalog.ARG to initialCategoryId) else emptyMap()
        )
        return CatalogViewModel(
            savedStateHandle, getCategories, getQuotesByCategory, getFavoriteQuotes, toggleFavorite
        )
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has isLoading true before subscriber attaches`() {
        // Before anyone subscribes, stateIn emits its initialValue (isLoading=true)
        val viewModel = buildViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.categories.isEmpty())
        assertTrue(viewModel.uiState.value.quotes.isEmpty())
    }

    @Test
    fun `when no initial category, selectedCategoryId is null and favorites tab is active`() = runTest {
        val viewModel = buildViewModel(initialCategoryId = null)

        viewModel.uiState.test {
            advanceUntilIdle()
            // Skip any initial loading emission
            val state = expectMostRecentItem()
            assertNull(state.selectedCategoryId)
            assertTrue(state.isFavoritesTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when initial category is provided via SavedStateHandle, it is pre-selected`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel(initialCategoryId = "Naruto")

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("Naruto", state.selectedCategoryId)
            assertFalse(state.isFavoritesTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Categories population ─────────────────────────────────────────────────

    @Test
    fun `categories are populated from GetCategoriesUseCase`() = runTest {
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(3, state.categories.size)
            assertEquals("Naruto", state.categories[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Favorites tab ─────────────────────────────────────────────────────────

    @Test
    fun `when favorites tab is active (null category), quotes come from getFavoriteQuotes`() = runTest {
        val viewModel = buildViewModel(initialCategoryId = null)

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(2, state.quotes.size)
            assertTrue(state.quotes.all { it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isFavoritesTab is true when selectedCategoryId is null`() = runTest {
        val viewModel = buildViewModel(initialCategoryId = null)

        viewModel.uiState.test {
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().isFavoritesTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isFavoritesTab is false when a category is selected`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel(initialCategoryId = "Naruto")

        viewModel.uiState.test {
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().isFavoritesTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onCategorySelected / flatMapLatest ────────────────────────────────────

    @Test
    fun `when onCategorySelected called with categoryId, quotes for that category are loaded`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel(initialCategoryId = null)

        viewModel.uiState.test {
            advanceUntilIdle()  // settle initial state (favorites)

            viewModel.onCategorySelected("Naruto")
            advanceUntilIdle()  // let flatMapLatest switch to Naruto flow

            val state = expectMostRecentItem()
            assertEquals("Naruto", state.selectedCategoryId)
            assertEquals(2, state.quotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when onCategorySelected called with null, favorites tab is shown`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel(initialCategoryId = "Naruto")

        viewModel.uiState.test {
            advanceUntilIdle()  // settle on Naruto

            viewModel.onCategorySelected(null)
            advanceUntilIdle()  // switch back to favorites

            val state = expectMostRecentItem()
            assertNull(state.selectedCategoryId)
            assertTrue(state.isFavoritesTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when switching categories, quotes update to reflect new category`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)
        every { getQuotesByCategory("One Piece") } returns flowOf(onePieceQuotes)

        val viewModel = buildViewModel(initialCategoryId = "Naruto")

        viewModel.uiState.test {
            advanceUntilIdle()  // settle on Naruto (2 quotes)

            viewModel.onCategorySelected("One Piece")
            advanceUntilIdle()  // switch to One Piece (1 quote)

            val state = expectMostRecentItem()
            assertEquals("One Piece", state.selectedCategoryId)
            assertEquals(1, state.quotes.size)
            assertEquals("One Piece", state.quotes[0].anime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── isEmpty computed property ─────────────────────────────────────────────

    @Test
    fun `isEmpty is true when not loading and quotes list is empty`() = runTest {
        every { getQuotesByCategory("Unknown") } returns flowOf(emptyList())

        val viewModel = buildViewModel(initialCategoryId = "Unknown")

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.quotes.isEmpty())
            assertTrue(state.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isEmpty is false when quotes are present`() = runTest {
        every { getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        val viewModel = buildViewModel(initialCategoryId = "Naruto")

        viewModel.uiState.test {
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

    @Test
    fun `onToggleFavorite can be called for quotes from the favorites list`() = runTest {
        coJustRun { toggleFavorite(any()) }

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()

            viewModel.onToggleFavorite(favoriteQuotes[0])
            advanceUntilIdle()

            coVerify(exactly = 1) { toggleFavorite(favoriteQuotes[0]) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Reactive state updates ────────────────────────────────────────────────

    @Test
    fun `when favorite quotes flow emits new list, uiState reflects update`() = runTest {
        val favoritesFlow = MutableStateFlow(favoriteQuotes)
        every { getFavoriteQuotes() } returns favoritesFlow

        val viewModel = buildViewModel(initialCategoryId = null)

        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals(2, expectMostRecentItem().quotes.size)

            val newFavorite = Quote(id = "99", quote = "Shikai!", author = "Rukia", anime = "Bleach", isFavorite = true)
            favoritesFlow.value = favoriteQuotes + newFavorite
            advanceUntilIdle()

            assertEquals(3, expectMostRecentItem().quotes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
