package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.data.local.datastore.UserPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePremiumStatusUseCase @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    operator fun invoke(): Flow<Boolean> = dataStore.isPremium
}

/**
 * Pre-billing mock: flips the local entitlement flag directly. Once Google Play Billing
 * is wired in, this call site moves to the purchase-completed callback instead of a
 * button tap, but every reader of [ObservePremiumStatusUseCase] stays unchanged.
 */
class SetPremiumStatusUseCase @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    suspend operator fun invoke(enabled: Boolean) = dataStore.setPremium(enabled)
}
