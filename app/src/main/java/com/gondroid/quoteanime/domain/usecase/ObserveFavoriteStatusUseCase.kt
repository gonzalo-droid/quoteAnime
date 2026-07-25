package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.QuoteRepository
import javax.inject.Inject

class ObserveFavoriteStatusUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    operator fun invoke(quoteId: String) = repository.isFavorite(quoteId)
}
