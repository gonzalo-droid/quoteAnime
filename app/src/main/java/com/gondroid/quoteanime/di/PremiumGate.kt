package com.gondroid.quoteanime.di

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where plan-based limits live. Pure computation over the caller's current
 * entitlement (see [com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase])
 * rather than owning that state itself, so it stays trivial to test and doesn't care
 * whether the entitlement comes from the local mock flag or, later, a verified Play
 * Billing purchase.
 */
@Singleton
class PremiumGate @Inject constructor() {

    fun maxActiveHabits(isPremium: Boolean): Int =
        if (isPremium) UNLIMITED_HABITS else FREE_HABIT_LIMIT

    companion object {
        const val FREE_HABIT_LIMIT = 3
        const val UNLIMITED_HABITS = Int.MAX_VALUE
    }
}
