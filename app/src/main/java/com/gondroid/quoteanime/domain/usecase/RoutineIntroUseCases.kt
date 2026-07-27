package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.data.local.datastore.UserPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsRoutineIntroSeenUseCase @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    operator fun invoke(): Flow<Boolean> = dataStore.isRoutineIntroSeen
}

class SetRoutineIntroSeenUseCase @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    suspend operator fun invoke() = dataStore.setRoutineIntroSeen()
}
