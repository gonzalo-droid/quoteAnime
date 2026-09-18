package com.gondroid.quoteanime.data.repository

import app.cash.turbine.test
import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.entity.FavoriteQuoteEntity
import com.gondroid.quoteanime.data.remote.QuoteRemoteDataSource
import com.gondroid.quoteanime.data.remote.dto.QuoteDto
import io.mockk.coEvery
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
 * Tests for [QuoteRepositoryImpl]: the combine() that merges the Realtime Database flows with the
 * local Room favorites, and the two classifications of a quote — its anime (Settings selection)
 * and its emotion categories (Catalogue).
 *
 * The fixtures mirror production: every quote carries emotion `categories` next to its `anime`.
 *
 * Scenarios covered:
 *  - getQuotesByCategory (emotion): matches `categories`, never `anime`; favorites merged live
 *  - getAllQuotes: same combine() logic applies across full quote list
 *  - getAnimes: distinct anime names, sorted, no emotions
 *  - getFavorites: maps FavoriteQuoteEntity to domain Quote with isFavorite = true
 *  - getRandomQuote: filters by anime, ignores emotions, reads a stale selection as all
 */
class QuoteRepositoryImplTest {

    private lateinit var remoteDataSource: QuoteRemoteDataSource
    private lateinit var favoriteQuoteDao: FavoriteQuoteDao
    private lateinit var repository: QuoteRepositoryImpl

    // Fake in-memory state flows to simulate real-time updates
    private val favoriteIdsFlow = MutableStateFlow<List<String>>(emptyList())

    private val quoteDtoNaruto1 = QuoteDto(id = "1", quote = "Believe it!", author = "Naruto", anime = "Naruto", categories = listOf("motivación", "reflexión"), animeSlug = null)
    private val quoteDtoNaruto2 = QuoteDto(id = "2", quote = "I never give up.", author = "Naruto", anime = "Naruto", categories = listOf("motivación"), animeSlug = null)
    private val quoteDtoOnePiece = QuoteDto(id = "10", quote = "I will be King!", author = "Luffy", anime = "One Piece", categories = listOf("amistad"), animeSlug = null)
    private val quoteDtoBleach = QuoteDto(id = "20", quote = "Bankai!", author = "Ichigo", anime = "Bleach", categories = listOf("motivación"), animeSlug = null)

    @Before
    fun setup() {
        remoteDataSource = mockk()
        favoriteQuoteDao = mockk()
        every { favoriteQuoteDao.getFavoriteIds() } returns favoriteIdsFlow
        // getAnimeImages() is called internally for every quote-returning method
        coEvery { remoteDataSource.getAnimeImages() } returns emptyMap()
        repository = QuoteRepositoryImpl(remoteDataSource, favoriteQuoteDao)
    }

    // ── getQuotesByCategory: the Catalogue's emotion filter ─────────────────

    @Test
    fun `getQuotesByCategory - keeps the quotes tagged with the emotion`() = runTest {
        every { remoteDataSource.getAllQuotes() } returns flowOf(listOf(quoteDtoNaruto1, quoteDtoOnePiece, quoteDtoNaruto2))

        repository.getQuotesByCategory("motivación").test {
            assertEquals(listOf("1", "2"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getQuotesByCategory - an anime name is not an emotion`() = runTest {
        every { remoteDataSource.getAllQuotes() } returns flowOf(listOf(quoteDtoNaruto1, quoteDtoOnePiece))

        repository.getQuotesByCategory("Naruto").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getQuotesByCategory - quote in favorites list has isFavorite true`() = runTest {
        favoriteIdsFlow.value = listOf("1")
        every { remoteDataSource.getAllQuotes() } returns flowOf(listOf(quoteDtoNaruto1, quoteDtoNaruto2))

        repository.getQuotesByCategory("motivación").test {
            val quotes = awaitItem()
            assertEquals(2, quotes.size)
            assertTrue("Quote id=1 should be favorite", quotes.first { it.id == "1" }.isFavorite)
            assertFalse("Quote id=2 should not be favorite", quotes.first { it.id == "2" }.isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getQuotesByCategory - when favorites update at runtime, downstream emits updated isFavorite flags`() = runTest {
        favoriteIdsFlow.value = emptyList()
        every { remoteDataSource.getAllQuotes() } returns flowOf(listOf(quoteDtoNaruto1, quoteDtoNaruto2))

        repository.getQuotesByCategory("motivación").test {
            assertFalse(awaitItem().first { it.id == "1" }.isFavorite)

            favoriteIdsFlow.value = listOf("1")

            val second = awaitItem()
            assertTrue(second.first { it.id == "1" }.isFavorite)
            assertFalse(second.first { it.id == "2" }.isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getQuotesByCategory - unknown emotion returns empty list`() = runTest {
        favoriteIdsFlow.value = listOf("1", "2", "3")
        every { remoteDataSource.getAllQuotes() } returns flowOf(listOf(quoteDtoNaruto1))

        repository.getQuotesByCategory("Unknown").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getAnimes: the Settings anime selector ────────────────────────────────

    @Test
    fun `getAnimes - lists the distinct anime names sorted, never the emotion categories`() = runTest {
        every { remoteDataSource.getAllQuotes() } returns flowOf(
            listOf(quoteDtoOnePiece, quoteDtoNaruto1, quoteDtoBleach, quoteDtoNaruto2)
        )

        repository.getAnimes().test {
            assertEquals(listOf("Bleach", "Naruto", "One Piece"), awaitItem())
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

    // ── getRandomQuote: notifications and widget ──────────────────────────────

    private fun givenQuotes(vararg dtos: QuoteDto) {
        coEvery { remoteDataSource.getAllQuotesOnce() } returns dtos.toList()
    }

    @Test
    fun `getRandomQuote - when there are no quotes, returns null`() = runTest {
        givenQuotes()

        assertNull(repository.getRandomQuote(setOf("Naruto")))
    }

    @Test
    fun `getRandomQuote - maps the picked QuoteDto to a domain Quote`() = runTest {
        givenQuotes(quoteDtoNaruto1)

        val result = repository.getRandomQuote(setOf("Naruto"))

        assertEquals("1", result?.id)
        assertEquals("Believe it!", result?.quote)
        assertEquals("Naruto", result?.anime)
        assertEquals(listOf("motivación", "reflexión"), result?.categories)
        assertFalse("getRandomQuote should always return isFavorite=false", result?.isFavorite ?: true)
    }

    @Test
    fun `getRandomQuote - only picks quotes of the selected animes`() = runTest {
        givenQuotes(quoteDtoNaruto1, quoteDtoOnePiece, quoteDtoBleach, quoteDtoNaruto2)

        repeat(50) {
            assertEquals("Naruto", repository.getRandomQuote(setOf("Naruto"))?.anime)
        }
    }

    @Test
    fun `getRandomQuote - a stale emotion selection picks from every anime`() = runTest {
        givenQuotes(quoteDtoNaruto1, quoteDtoOnePiece)

        val picked = (1..100).mapNotNull { repository.getRandomQuote(setOf("motivación"))?.id }.toSet()

        assertEquals(setOf("1", "10"), picked)
    }

    @Test
    fun `getRandomQuote - avoids the excluded quote when there is another`() = runTest {
        givenQuotes(quoteDtoNaruto1, quoteDtoNaruto2, quoteDtoOnePiece)

        repeat(50) {
            assertEquals("2", repository.getRandomQuote(setOf("Naruto"), excludeId = "1")?.id)
        }
    }
}
