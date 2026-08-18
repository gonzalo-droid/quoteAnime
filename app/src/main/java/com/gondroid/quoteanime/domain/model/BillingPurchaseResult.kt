package com.gondroid.quoteanime.domain.model

/** One-shot outcomes emitted while a purchase is in flight, for UI feedback (snackbar/toast). */
sealed interface BillingPurchaseResult {
    data object Success : BillingPurchaseResult
    data object Pending : BillingPurchaseResult
    data object UserCancelled : BillingPurchaseResult
    data class Error(val message: String) : BillingPurchaseResult
}
