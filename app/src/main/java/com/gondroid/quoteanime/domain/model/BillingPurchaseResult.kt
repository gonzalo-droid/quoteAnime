package com.gondroid.quoteanime.domain.model

/** One-shot outcomes emitted while a purchase is in flight, for UI feedback (snackbar/toast). */
sealed interface BillingPurchaseResult {
    data object Success : BillingPurchaseResult
    data object Pending : BillingPurchaseResult
    data object UserCancelled : BillingPurchaseResult

    /**
     * [reason] is what the UI may act on; [diagnostic] is developer text for logs and crash
     * reports and must never reach the screen — Play's `debugMessage` is English, internal,
     * and occasionally leaks implementation detail.
     */
    data class Error(
        val reason: BillingErrorReason,
        val diagnostic: String
    ) : BillingPurchaseResult
}

/** Play's response codes, reduced to the cases a user can actually do something about. */
enum class BillingErrorReason {
    /** Play Store missing, out of date, or unavailable in this country. */
    PLAY_UNAVAILABLE,
    NETWORK,
    UNKNOWN
}
