package com.gondroid.quoteanime.worker

import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.remote.QuoteRemoteDataSource
import com.gondroid.quoteanime.data.remote.dto.QuoteDto
import com.gondroid.quoteanime.data.repository.QuoteRepositoryImpl
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import io.mockk.coEvery
import io.mockk.mockk

/**
 * The real pick (use case → repository → domain rule) over a fixed `/quotes` snapshot, so the
 * worker tests exercise the anime filter the workers actually run, not a stub of it.
 * Like production, every quote carries emotion categories next to its anime.
 */
internal object WorkerQuoteFixtures {
    val quotes = listOf(
        QuoteDto(id = "1", quote = "Believe it!", author = "Naruto", anime = "Naruto", categories = listOf("motivación", "reflexión"), animeSlug = null),
        QuoteDto(id = "2", quote = "I will be King!", author = "Luffy", anime = "One Piece", categories = listOf("motivación"), animeSlug = null),
        QuoteDto(id = "3", quote = "Bankai!", author = "Ichigo", anime = "Bleach", categories = listOf("motivación"), animeSlug = null),
        QuoteDto(id = "4", quote = "Dattebayo", author = "Naruto", anime = "Naruto", categories = listOf("amistad"), animeSlug = null)
    )

    fun realGetRandomQuote(): GetRandomQuoteUseCase {
        val remote = mockk<QuoteRemoteDataSource>()
        coEvery { remote.getAllQuotesOnce() } returns quotes
        coEvery { remote.getAnimeImages() } returns emptyMap()
        return GetRandomQuoteUseCase(QuoteRepositoryImpl(remote, mockk<FavoriteQuoteDao>()))
    }
}
