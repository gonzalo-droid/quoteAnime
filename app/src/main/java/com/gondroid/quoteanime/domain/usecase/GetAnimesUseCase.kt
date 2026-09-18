package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.QuoteRepository
import javax.inject.Inject

/**
 * The anime names offered by the Settings anime selector: the distinct `anime` values of every
 * quote, sorted like iOS. Not the emotions in `categories` — those belong to the Catalogue.
 */
class GetAnimesUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    operator fun invoke() = repository.getAnimes()
}
