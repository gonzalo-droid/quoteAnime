package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.repository.QuoteRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Scenarios covered:
 *  - When quote.isFavorite == true  → removeFavorite(id) is called, addFavorite is NOT called
 *  - When quote.isFavorite == false → addFavorite(quote) is called, removeFavorite is NOT called
 *  - Edge case: quote with empty id still delegates correctly
 */
class ToggleFavoriteUseCaseTest {

    private lateinit var repository: QuoteRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    private val favoriteQuote = Quote(
        id = "1",
        quote = "The only way to do great work is to love what you do.",
        author = "Steve Jobs",
        anime = "Naruto",
        isFavorite = true
    )

    private val nonFavoriteQuote = Quote(
        id = "2",
        quote = "It always seems impossible until it's done.",
        author = "Nelson Mandela",
        anime = "One Piece",
        isFavorite = false
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = ToggleFavoriteUseCase(repository)
    }

    @Test
    fun `given quote is favorite, when invoked, then removeFavorite is called with its id`() = runTest {
        coJustRun { repository.removeFavorite(favoriteQuote.id) }

        useCase(favoriteQuote)

        coVerify(exactly = 1) { repository.removeFavorite("1") }
        coVerify(exactly = 0) { repository.addFavorite(any()) }
    }

    @Test
    fun `given quote is not favorite, when invoked, then addFavorite is called with the quote`() = runTest {
        coJustRun { repository.addFavorite(nonFavoriteQuote) }

        useCase(nonFavoriteQuote)

        coVerify(exactly = 1) { repository.addFavorite(nonFavoriteQuote) }
        coVerify(exactly = 0) { repository.removeFavorite(any()) }
    }

    @Test
    fun `given quote with empty id is favorite, when invoked, then removeFavorite is called with empty id`() = runTest {
        val quoteWithEmptyId = Quote(id = "", quote = "text", author = "auth", anime = "anime", isFavorite = true)
        coJustRun { repository.removeFavorite("") }

        useCase(quoteWithEmptyId)

        coVerify(exactly = 1) { repository.removeFavorite("") }
    }

    @Test
    fun `given non-favorite quote, when invoked twice, then addFavorite is called twice`() = runTest {
        coJustRun { repository.addFavorite(any()) }

        useCase(nonFavoriteQuote)
        useCase(nonFavoriteQuote)

        coVerify(exactly = 2) { repository.addFavorite(nonFavoriteQuote) }
    }
}
