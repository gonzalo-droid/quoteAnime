package com.gondroid.quoteanime.di

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where plan-based limits live. Billing is not implemented yet:
 * flipping [isPremium] later must not require touching use cases or UI.
 */
@Singleton
class PremiumGate @Inject constructor() {

    val isPremium: Boolean = false

    val maxActiveHabits: Int
        get() = if (isPremium) UNLIMITED_HABITS else FREE_HABIT_LIMIT

    companion object {
        const val FREE_HABIT_LIMIT = 3
        const val UNLIMITED_HABITS = Int.MAX_VALUE
    }
}
