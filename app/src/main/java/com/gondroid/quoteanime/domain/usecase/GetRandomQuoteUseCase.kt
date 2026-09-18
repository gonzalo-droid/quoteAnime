package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.QuoteRepository
import javax.inject.Inject

/** A random quote of the anime selection [animes] (empty = every anime), for notifications and the widget. */
class GetRandomQuoteUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    suspend operator fun invoke(animes: Set<String>, excludeId: String? = null) =
        repository.getRandomQuote(animes, excludeId)
}
