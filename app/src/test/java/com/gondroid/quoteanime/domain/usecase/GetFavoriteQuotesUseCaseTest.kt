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
 *  - Happy path: repository emits a non-empty list, use case passes it through unchanged
 *  - Empty favorites: repository emits empty list, use case emits empty list
 *  - Multiple emissions: Flow emits several values in sequence
 *  - Delegation: repository.getFavorites() is called exactly once
 */
class GetFavoriteQuotesUseCaseTest {

    private lateinit var repository: QuoteRepository
    private lateinit var useCase: GetFavoriteQuotesUseCase

    private val sampleFavorites = listOf(
        Quote(id = "1", quote = "Quote 1", author = "Author 1", anime = "Naruto", isFavorite = true),
        Quote(id = "2", quote = "Quote 2", author = "Author 2", anime = "One Piece", isFavorite = true)
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetFavoriteQuotesUseCase(repository)
    }

    @Test
    fun `given repository has favorites, when invoked, then emits the list of favorites`() = runTest {
        every { repository.getFavorites() } returns flowOf(sampleFavorites)

        useCase().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("1", result[0].id)
            assertEquals("2", result[1].id)
            assertTrue(result.all { it.isFavorite })
            awaitComplete()
        }
    }

    @Test
    fun `given repository has no favorites, when invoked, then emits empty list`() = runTest {
        every { repository.getFavorites() } returns flowOf(emptyList())

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `when invoked, then delegates to repository getFavorites exactly once`() = runTest {
        every { repository.getFavorites() } returns flowOf(emptyList())

        useCase().test {
            awaitItem()
            awaitComplete()
        }

        verify(exactly = 1) { repository.getFavorites() }
    }

    @Test
    fun `given repository emits multiple values over time, when invoked, then all values are received`() = runTest {
        val firstBatch = listOf(
            Quote(id = "1", quote = "Q1", author = "A1", anime = "Naruto", isFavorite = true)
        )
        val secondBatch = listOf(
            Quote(id = "1", quote = "Q1", author = "A1", anime = "Naruto", isFavorite = true),
            Quote(id = "3", quote = "Q3", author = "A3", anime = "Bleach", isFavorite = true)
        )
        every { repository.getFavorites() } returns kotlinx.coroutines.flow.flow {
            emit(firstBatch)
            emit(secondBatch)
        }

        useCase().test {
            assertEquals(1, awaitItem().size)
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }
}
