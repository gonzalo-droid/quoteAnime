package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.QuoteRepository
import javax.inject.Inject

class GetRandomQuoteUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    suspend operator fun invoke(categoryIds: Set<String>, excludeId: String? = null) =
        repository.getRandomQuote(categoryIds, excludeId)
}
