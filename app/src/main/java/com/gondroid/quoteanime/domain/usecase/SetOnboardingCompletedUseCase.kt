package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetOnboardingCompletedUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke() = repository.setOnboardingCompleted()
}
