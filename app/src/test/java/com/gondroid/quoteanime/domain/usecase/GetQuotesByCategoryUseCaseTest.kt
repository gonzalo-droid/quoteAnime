package com.gondroid.quoteanime.domain.usecase

import app.cash.turbine.test
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.repository.QuoteRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Scenarios covered:
 *  - Happy path: quotes returned for a given category id
 *  - Empty result: repository emits empty list for category
 *  - Correct delegation: repository called with exact categoryId argument
 *  - isFavorite flag is preserved in the emitted domain models
 *  - Multiple categories: each call to invoke uses its own categoryId
 */
class GetQuotesByCategoryUseCaseTest {

    private lateinit var repository: QuoteRepository
    private lateinit var useCase: GetQuotesByCategoryUseCase

    private val narutoQuotes = listOf(
        Quote(id = "1", quote = "Believe it!", author = "Naruto", anime = "Naruto", isFavorite = false),
        Quote(id = "2", quote = "I never go back on my word.", author = "Naruto", anime = "Naruto", isFavorite = true)
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetQuotesByCategoryUseCase(repository)
    }

    @Test
    fun `given category with quotes, when invoked, then emits quotes for that category`() = runTest {
        every { repository.getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        useCase("Naruto").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Naruto", result[0].anime)
            awaitComplete()
        }
    }

    @Test
    fun `given category with no quotes, when invoked, then emits empty list`() = runTest {
        every { repository.getQuotesByCategory("UnknownAnime") } returns flowOf(emptyList())

        useCase("UnknownAnime").test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `when invoked with categoryId, then repository is called with exact same id`() = runTest {
        every { repository.getQuotesByCategory("One Piece") } returns flowOf(emptyList())

        useCase("One Piece").test {
            awaitItem()
            awaitComplete()
        }

        verify(exactly = 1) { repository.getQuotesByCategory("One Piece") }
    }

    @Test
    fun `given quotes with mixed isFavorite states, when emitted, then flags are preserved`() = runTest {
        every { repository.getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)

        useCase("Naruto").test {
            val result = awaitItem()
            assertEquals(false, result[0].isFavorite)
            assertEquals(true, result[1].isFavorite)
            awaitComplete()
        }
    }

    @Test
    fun `given two different categories, when each is invoked separately, then each returns its own quotes`() = runTest {
        val onePieceQuotes = listOf(
            Quote(id = "10", quote = "I will be King!", author = "Luffy", anime = "One Piece", isFavorite = false)
        )
        every { repository.getQuotesByCategory("Naruto") } returns flowOf(narutoQuotes)
        every { repository.getQuotesByCategory("One Piece") } returns flowOf(onePieceQuotes)

        useCase("Naruto").test {
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }

        useCase("One Piece").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("One Piece", result[0].anime)
            awaitComplete()
        }
    }
}
