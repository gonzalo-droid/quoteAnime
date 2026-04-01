package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class GetUserPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    operator fun invoke() = repository.getUserPreferences()
}
