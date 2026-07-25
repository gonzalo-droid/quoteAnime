package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.QuoteRepository
import javax.inject.Inject

class GetQuotesByCategoryUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    operator fun invoke(categoryId: String) = repository.getQuotesByCategory(categoryId)
}
