package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.repository.QuoteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    suspend operator fun invoke(quote: Quote) {
        if (quote.isFavorite) {
            repository.removeFavorite(quote.id)
        } else {
            repository.addFavorite(quote)
        }
    }
}
