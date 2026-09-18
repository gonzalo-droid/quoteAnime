package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.QuoteRepository
import javax.inject.Inject

/**
 * The quotes tagged with one emotion (`Quote.categories`) — the Catalogue's `ByEmotion` filter.
 * "Category" here means emotion; the anime selection is [GetAnimesUseCase]'s concern.
 */
class GetQuotesByCategoryUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    operator fun invoke(categoryId: String) = repository.getQuotesByCategory(categoryId)
}
