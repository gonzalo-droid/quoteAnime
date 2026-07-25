package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.repository.QuoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Scenarios covered:
 *  - Happy path: repository returns a quote, use case returns it unchanged
 *  - Null return: repository has no matching quotes, use case returns null
 *  - Empty category set: delegates to repository with empty set (all categories)
 *  - Non-empty category set: delegates with exact set of category ids
 *  - Delegation: repository.getRandomQuote() called exactly once
 */
class GetRandomQuoteUseCaseTest {

    private lateinit var repository: QuoteRepository
    private lateinit var useCase: GetRandomQuoteUseCase

    private val sampleQuote = Quote(
        id = "42",
        quote = "Hard work beats talent when talent doesn't work hard.",
        author = "Rock Lee",
        anime = "Naruto",
        isFavorite = false
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetRandomQuoteUseCase(repository)
    }

    @Test
    fun `given repository returns a quote, when invoked, then returns that quote`() = runTest {
        val categoryIds = setOf("Naruto", "One Piece")
        coEvery { repository.getRandomQuote(categoryIds) } returns sampleQuote

        val result = useCase(categoryIds)

        assertEquals(sampleQuote, result)
    }

    @Test
    fun `given repository returns null, when invoked, then returns null`() = runTest {
        val categoryIds = setOf("UnknownAnime")
        coEvery { repository.getRandomQuote(categoryIds) } returns null

        val result = useCase(categoryIds)

        assertNull(result)
    }

    @Test
    fun `given empty category set, when invoked, then delegates empty set to repository`() = runTest {
        coEvery { repository.getRandomQuote(emptySet()) } returns sampleQuote

        val result = useCase(emptySet())

        assertEquals(sampleQuote, result)
        coVerify(exactly = 1) { repository.getRandomQuote(emptySet()) }
    }

    @Test
    fun `given specific category ids, when invoked, then repository is called with exact ids`() = runTest {
        val categoryIds = setOf("Bleach", "Dragon Ball", "Attack on Titan")
        coEvery { repository.getRandomQuote(categoryIds) } returns null

        useCase(categoryIds)

        coVerify(exactly = 1) { repository.getRandomQuote(categoryIds) }
    }

    @Test
    fun `given repository returns a quote with isFavorite true, when invoked, then returned quote preserves the flag`() = runTest {
        val favoriteQuote = sampleQuote.copy(isFavorite = true)
        coEvery { repository.getRandomQuote(any()) } returns favoriteQuote

        val result = useCase(setOf("Naruto"))

        assertEquals(true, result?.isFavorite)
    }
}
