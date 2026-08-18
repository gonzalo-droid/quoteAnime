package com.gondroid.quoteanime.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.gondroid.quoteanime.data.local.datastore.UserPreferencesDataStore
import com.gondroid.quoteanime.domain.model.BillingPurchaseResult
import com.gondroid.quoteanime.domain.model.SubscriptionOffer
import com.gondroid.quoteanime.domain.repository.BillingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
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
    @ApplicationContext context: Context,
    private val dataStore: UserPreferencesDataStore
) : BillingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _purchaseEvents = MutableSharedFlow<BillingPurchaseResult>(extraBufferCapacity = 1)
    override val purchaseEvents: SharedFlow<BillingPurchaseResult> = _purchaseEvents.asSharedFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        scope.launch { handlePurchasesUpdated(billingResult, purchases) }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
        .enableAutoServiceReconnection()
        .build()

    private var isConnected = false
    private var cachedProductDetails: ProductDetails? = null

    override suspend fun queryOffers(): List<SubscriptionOffer> {
        ensureConnected()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val result = billingClient.queryProductDetails(params)
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
        val productDetails = cachedProductDetails ?: return
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
        billingClient.launchBillingFlow(activity, params)
    }

    override suspend fun restorePurchases() {
        ensureConnected()
        syncPurchases()
    }

    // MARK: - Private

    private suspend fun ensureConnected() {
        if (isConnected) return
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                }
            })
        }
    }

    private suspend fun syncPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        val purchases = result.purchasesList
        val hasActiveEntitlement = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { acknowledge(it) }

        dataStore.setPremium(hasActiveEntitlement)
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
            else ->
                _purchaseEvents.tryEmit(BillingPurchaseResult.Error(billingResult.debugMessage))
        }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params)
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

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_subscription"
    }
}
