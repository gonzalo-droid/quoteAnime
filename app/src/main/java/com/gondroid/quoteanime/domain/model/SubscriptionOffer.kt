package com.gondroid.quoteanime.domain.model

/**
 * One purchasable base plan for [com.gondroid.quoteanime.domain.repository.BillingRepository.PREMIUM_PRODUCT_ID],
 * as returned by Play. Whatever base plans/offers exist in Play Console show up here —
 * nothing about plan count or pricing is hardcoded client-side.
 */
data class SubscriptionOffer(
    val offerToken: String,
    val basePlanId: String,
    val formattedPrice: String,
    /** ISO 8601 duration, e.g. "P1M" (monthly) or "P1Y" (annual). */
    val billingPeriod: String,
    val freeTrialDays: Int?
)
