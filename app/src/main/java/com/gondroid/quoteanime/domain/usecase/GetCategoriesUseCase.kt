package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.QuoteRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    operator fun invoke() = repository.getCategories()
}
