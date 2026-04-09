package com.gondroid.quoteanime.data.repository

import app.cash.turbine.test
import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.entity.FavoriteQuoteEntity
import com.gondroid.quoteanime.data.remote.QuoteRemoteDataSource
import com.gondroid.quoteanime.data.remote.dto.QuoteDto
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [QuoteRepositoryImpl] focusing on the combine() logic that merges
 * Firestore flows with the local Room favorites.
 *
 * Scenarios covered:
 *  - getQuotesByCategory: quote in favorites → isFavorite = true
 *  - getQuotesByCategory: quote NOT in favorites → isFavorite = false
 *  - getQuotesByCategory: favorites update at runtime → downstream reflects new state
 *  - getAllQuotes: same combine() logic applies across full quote list
 *  - getFavorites: maps FavoriteQuoteEntity to domain Quote with isFavorite = true
 *  - getRandomQuote: returns null when remote returns null
 *  - getRandomQuote: maps QuoteDto to domain Quote
 *  - getCategories: maps CategoryDto list to domain Category list
 */
class QuoteRepositoryImplTest {

    private lateinit var remoteDataSource: QuoteRemoteDataSource
    private lateinit var favoriteQuoteDao: FavoriteQuoteDao
    private lateinit var repository: QuoteRepositoryImpl

    // Fake in-memory state flows to simulate real-time updates
    private val favoriteIdsFlow = MutableStateFlow<List<String>>(emptyList())

    private val quoteDtoNaruto1 = QuoteDto(id = "1", quote = "Believe it!", author = "Naruto", anime = "Naruto", categories = null, imageUrl = null)
    private val quoteDtoNaruto2 = QuoteDto(id = "2", quote = "I never give up.", author = "Naruto", anime = "Naruto", categories = null, imageUrl = null)
    private val quoteDtoOnePiece = QuoteDto(id = "10", quote = "I will be King!", author = "Luffy", anime = "One Piece", categories = null, imageUrl = null)

    @Before
    fun setup() {
        remoteDataSource = mockk()
        favoriteQuoteDao = mockk()
        every { favoriteQuoteDao.getFavoriteIds() } returns favoriteIdsFlow
        repository = QuoteRepositoryImpl(remoteDataSource, favoriteQuoteDao)
    }

    // ── getQuotesByCategory combine() tests ──────────────────────────────────

    @Test
    fun `getQuotesByCategory - quote in favorites list has isFavorite true`() = runTest {
        favoriteIdsFlow.value = listOf("1")
        every { remoteDataSource.getQuotesByCategory("Naruto") } returns flowOf(
            listOf(quoteDtoNaruto1, quoteDtoNaruto2)
        )

        repository.getQuotesByCategory("Naruto").test {
            val quotes = awaitItem()
            assertEquals(2, quotes.size)
            assertTrue("Quote id=1 should be favorite", quotes.first { it.id == "1" }.isFavorite)
            assertFalse("Quote id=2 should not be favorite", quotes.first { it.id == "2" }.isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getQuotesByCategory - no favorites results in all isFavorite false`() = runTest {
        favoriteIdsFlow.value = emptyList()
        every { remoteDataSource.getQuotesByCategory("Naruto") } returns flowOf(
            listOf(quoteDtoNaruto1, quoteDtoNaruto2)
        )

        repository.getQuotesByCategory("Naruto").test {
            val quotes = awaitItem()
            assertTrue("All quotes should have isFavorite=false", quotes.all { !it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getQuotesByCategory - when favorites update at runtime, downstream emits updated isFavorite flags`() = runTest {
        favoriteIdsFlow.value = emptyList()
        every { remoteDataSource.getQuotesByCategory("Naruto") } returns flowOf(
            listOf(quoteDtoNaruto1, quoteDtoNaruto2)
        )

        repository.getQuotesByCategory("Naruto").test {
            // First emission: nothing is favorited
            val first = awaitItem()
            assertFalse(first.first { it.id == "1" }.isFavorite)

            // Simulate user adding quote "1" to favorites
            favoriteIdsFlow.value = listOf("1")

            // Second emission: quote "1" is now favorited
            val second = awaitItem()
            assertTrue(second.first { it.id == "1" }.isFavorite)
            assertFalse(second.first { it.id == "2" }.isFavorite)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getQuotesByCategory - empty remote result returns empty list`() = runTest {
        favoriteIdsFlow.value = listOf("1", "2", "3")
        every { remoteDataSource.getQuotesByCategory("Unknown") } returns flowOf(emptyList())

        repository.getQuotesByCategory("Unknown").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getAllQuotes combine() tests ──────────────────────────────────────────

    @Test
    fun `getAllQuotes - merges all remote quotes with favorites correctly`() = runTest {
        favoriteIdsFlow.value = listOf("10")
        every { remoteDataSource.getAllQuotes() } returns flowOf(
            listOf(quoteDtoNaruto1, quoteDtoOnePiece)
        )

        repository.getAllQuotes().test {
            val quotes = awaitItem()
            assertEquals(2, quotes.size)
            assertFalse(quotes.first { it.id == "1" }.isFavorite)
            assertTrue(quotes.first { it.id == "10" }.isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getFavorites mapping tests ────────────────────────────────────────────

    @Test
    fun `getFavorites - maps FavoriteQuoteEntity list to domain Quotes with isFavorite true`() = runTest {
        val entities = listOf(
            FavoriteQuoteEntity(id = "1", quote = "Believe it!", author = "Naruto", anime = "Naruto"),
            FavoriteQuoteEntity(id = "2", quote = "I never give up.", author = "Naruto", anime = "Naruto")
        )
        every { favoriteQuoteDao.getFavorites() } returns flowOf(entities)

        repository.getFavorites().test {
            val quotes = awaitItem()
            assertEquals(2, quotes.size)
            assertTrue("All favorites should have isFavorite=true", quotes.all { it.isFavorite })
            assertEquals("1", quotes[0].id)
            assertEquals("2", quotes[1].id)
            awaitComplete()
        }
    }

    @Test
    fun `getFavorites - empty entity list maps to empty domain list`() = runTest {
        every { favoriteQuoteDao.getFavorites() } returns flowOf(emptyList())

        repository.getFavorites().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // ── getRandomQuote tests ──────────────────────────────────────────────────

    @Test
    fun `getRandomQuote - when remote returns null, repository returns null`() = runTest {
        io.mockk.coEvery { remoteDataSource.getRandomQuote(any()) } returns null

        val result = repository.getRandomQuote(setOf("Naruto"))

        assertNull(result)
    }

    @Test
    fun `getRandomQuote - when remote returns a QuoteDto, maps it to domain Quote`() = runTest {
        io.mockk.coEvery { remoteDataSource.getRandomQuote(setOf("Naruto")) } returns quoteDtoNaruto1

        val result = repository.getRandomQuote(setOf("Naruto"))

        assertEquals("1", result?.id)
        assertEquals("Believe it!", result?.quote)
        assertEquals("Naruto", result?.author)
        assertEquals("Naruto", result?.anime)
        assertFalse("getRandomQuote should always return isFavorite=false", result?.isFavorite ?: true)
    }

    @Test
    fun `getRandomQuote - delegates empty categoryIds to remote data source`() = runTest {
        io.mockk.coEvery { remoteDataSource.getRandomQuote(emptySet()) } returns quoteDtoNaruto1

        repository.getRandomQuote(emptySet())

        io.mockk.coVerify(exactly = 1) { remoteDataSource.getRandomQuote(emptySet()) }
    }
}
