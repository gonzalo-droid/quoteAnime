package com.gondroid.quoteanime.data.repository

import android.app.Activity
import android.os.SystemClock
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.gondroid.quoteanime.data.local.datastore.UserPreferencesDataStore
import com.gondroid.quoteanime.data.remote.BillingClientFactory
import com.gondroid.quoteanime.domain.model.BillingPurchaseResult
import com.gondroid.quoteanime.domain.model.SubscriptionOffer
import com.gondroid.quoteanime.domain.repository.BillingRepository
import com.gondroid.quoteanime.worker.PurchaseAcknowledgementScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Client-only Play Billing integration — there's no backend to verify receipts server-side,
 * so [Purchase.PurchaseState.PURCHASED] + acknowledgement is treated as sufficient proof of
 * entitlement. [UserPreferencesDataStore.setPremium] stays the local cache every other reader
 * (`ObservePremiumStatusUseCase`) already depends on; this class is now the only writer of
 * a *real* `true`, and [restorePurchases] is what catches a cancellation made outside the app.
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    billingClientFactory: BillingClientFactory,
    private val dataStore: UserPreferencesDataStore,
    private val acknowledgementScheduler: PurchaseAcknowledgementScheduler
) : BillingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _purchaseEvents = MutableSharedFlow<BillingPurchaseResult>(extraBufferCapacity = 1)
    override val purchaseEvents: SharedFlow<BillingPurchaseResult> = _purchaseEvents.asSharedFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        scope.launch { handlePurchasesUpdated(billingResult, purchases) }
    }

    private val billingClient: BillingClient = billingClientFactory.create(purchasesUpdatedListener)

    /** Serialises `startConnection`, so the app-start restore and a paywall open don't race. */
    private val connectionMutex = Mutex()
    private var cachedProductDetails: ProductDetails? = null

    /** Last [restorePurchases] that actually hit Play, for the throttle described there. */
    private var lastSyncElapsedMs: Long? = null

    override suspend fun queryOffers(): List<SubscriptionOffer> {
        if (!ensureConnected()) return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingRepository.PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return emptyList()
        val productDetails = result.productDetailsList?.firstOrNull() ?: return emptyList()
        cachedProductDetails = productDetails

        return productDetails.subscriptionOfferDetails.orEmpty().map { offer ->
            val phases = offer.pricingPhases.pricingPhaseList
            val paidPhase = phases.firstOrNull { it.priceAmountMicros > 0 } ?: phases.first()
            val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
            SubscriptionOffer(
                offerToken = offer.offerToken,
                basePlanId = offer.basePlanId,
                formattedPrice = paidPhase.formattedPrice,
                billingPeriod = paidPhase.billingPeriod,
                freeTrialDays = trialPhase?.let { parseTrialDays(it.billingPeriod) }
            )
        }
    }

    override fun launchPurchaseFlow(activity: Activity, offer: SubscriptionOffer) {
        val productDetails = cachedProductDetails ?: run {
            _purchaseEvents.tryEmit(BillingPurchaseResult.Error("No hay detalles del producto"))
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseEvents.tryEmit(BillingPurchaseResult.Error(result.debugMessage))
        }
    }

    /**
     * Throttled because this now runs on every return to the foreground, not just once per
     * process: an entitlement can only change on Play's side, so re-querying on each alt-tab
     * would cost network for nothing.
     */
    override suspend fun restorePurchases() {
        val now = SystemClock.elapsedRealtime()
        lastSyncElapsedMs?.let { if (now - it < MIN_SYNC_INTERVAL_MS) return }
        if (!ensureConnected()) return
        syncPurchases()
        lastSyncElapsedMs = now
    }

    override suspend fun acknowledgePendingPurchases(): Boolean {
        if (!ensureConnected()) return false
        return syncPurchases()?.allAcknowledged == true
    }

    // MARK: - Private

    /** @return `true` only when the client is usable; callers must not query otherwise. */
    private suspend fun ensureConnected(): Boolean = connectionMutex.withLock {
        if (billingClient.isReady) return@withLock true
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                /** Resumes so a drop mid-setup fails the caller instead of suspending forever;
                 *  `enableAutoServiceReconnection` still retries underneath for the next call. */
                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    /**
     * @return `null` if the query itself failed. A failed query must *not* write the flag —
     * otherwise a cold start without network would revoke premium from a paying user.
     */
    private suspend fun syncPurchases(): SyncResult? {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return null
        val purchases = result.purchasesList
        val hasActiveEntitlement = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        val allAcknowledged = purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .map { acknowledge(it) }
            .all { it }

        dataStore.setPremium(hasActiveEntitlement)
        return SyncResult(hasActiveEntitlement, allAcknowledged)
    }

    private suspend fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchased = purchases.orEmpty()
                if (purchased.isEmpty()) {
                    _purchaseEvents.tryEmit(BillingPurchaseResult.Error("No se recibió la compra"))
                    return
                }
                purchased.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            if (!purchase.isAcknowledged) acknowledge(purchase)
                            dataStore.setPremium(true)
                            _purchaseEvents.tryEmit(BillingPurchaseResult.Success)
                        }
                        Purchase.PurchaseState.PENDING -> _purchaseEvents.tryEmit(BillingPurchaseResult.Pending)
                        else -> Unit
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _purchaseEvents.tryEmit(BillingPurchaseResult.UserCancelled)
            // Already subscribed (e.g. bought on another device): re-sync instead of erroring out.
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                _purchaseEvents.tryEmit(
                    if (syncPurchases()?.hasEntitlement == true) BillingPurchaseResult.Success
                    else BillingPurchaseResult.Error(billingResult.debugMessage)
                )
            else ->
                _purchaseEvents.tryEmit(BillingPurchaseResult.Error(billingResult.debugMessage))
        }
    }

    /**
     * Play auto-refunds and revokes anything left unacknowledged for 72 h, so a failure here
     * silently costs the user their subscription. Retries in place for the common case (a few
     * seconds of bad network right after paying) and hands the rest to
     * [PurchaseAcknowledgementScheduler], which survives the process being killed.
     *
     * @return whether Play accepted the acknowledgement.
     */
    private suspend fun acknowledge(purchase: Purchase): Boolean {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        for (attempt in 0 until ACK_MAX_ATTEMPTS) {
            val responseCode = billingClient.acknowledgePurchase(params).responseCode
            if (responseCode == BillingClient.BillingResponseCode.OK) return true
            // A permanent rejection (already refunded, developer error) won't fix itself.
            if (responseCode !in RETRYABLE_RESPONSE_CODES) break
            if (attempt < ACK_MAX_ATTEMPTS - 1) delay(ACK_BASE_DELAY_MS shl attempt)
        }

        acknowledgementScheduler.scheduleRetry()
        return false
    }

    /** Trial phases are billed at zero for one period — e.g. "P7D" → 7, "P1M" → 30 (approx). */
    private fun parseTrialDays(isoPeriod: String): Int? {
        val match = Regex("""P(\d+)([DWM])""").find(isoPeriod) ?: return null
        val (amount, unit) = match.destructured
        val days = when (unit) {
            "D" -> amount.toInt()
            "W" -> amount.toInt() * 7
            "M" -> amount.toInt() * 30
            else -> return null
        }
        return days
    }

    private data class SyncResult(val hasEntitlement: Boolean, val allAcknowledged: Boolean)

    private companion object {
        /** Codes worth another try — network or Play-service hiccups, not rejections. */
        val RETRYABLE_RESPONSE_CODES = setOf(
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.ERROR
        )
        const val ACK_MAX_ATTEMPTS = 3
        const val ACK_BASE_DELAY_MS = 1_000L
        const val MIN_SYNC_INTERVAL_MS = 15 * 60 * 1000L
    }
}
