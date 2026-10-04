package com.gondroid.quoteanime.di

import com.gondroid.quoteanime.domain.model.HabitTemplate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where plan-based limits live. Pure computation over the caller's current
 * entitlement (see [com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase])
 * rather than owning that state itself, so it stays trivial to test and doesn't care
 * whether the entitlement comes from the local mock flag or, later, a verified Play
 * Billing purchase.
 *
 * While [paymentsEnabled] is false the subscription is not for sale: every entry to the
 * paywall is hidden, so the free plan has no limits to sell against — unlimited habits and
 * every template unlocked. Ads still follow the real entitlement, so they keep showing.
 */
@Singleton
class PremiumGate(val paymentsEnabled: Boolean) {

    @Inject constructor() : this(PAYMENTS_ENABLED)

    fun maxActiveHabits(isPremium: Boolean): Int =
        if (isPremium || !paymentsEnabled) UNLIMITED_HABITS else FREE_HABIT_LIMIT

    /** With payments off nothing is premium-only, so no template shows a lock. */
    fun availableTemplates(templates: List<HabitTemplate>): List<HabitTemplate> =
        if (paymentsEnabled) templates
        else templates.map { if (it.isPremiumOnly) it.copy(isPremiumOnly = false) else it }

    companion object {
        /**
         * Off until `premium_subscription` exists in Play Console. Flipping it back shows the
         * paywall entries and restores the free-plan limits; the billing code never left.
         */
        const val PAYMENTS_ENABLED = false
        const val FREE_HABIT_LIMIT = 3
        const val UNLIMITED_HABITS = Int.MAX_VALUE
    }
}
