package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOnboardingCompletedUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.isOnboardingCompleted()
}
