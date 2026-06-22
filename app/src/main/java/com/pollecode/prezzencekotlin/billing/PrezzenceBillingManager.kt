package com.pollecode.prezzencekotlin.billing

import android.app.Activity
import android.content.Context
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
import com.pollecode.prezzencekotlin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class PrezzenceBillingManager(
    context: Context,
    private val onEntitlementChanged: (entitled: Boolean, status: String, purchaseToken: String?) -> Unit,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    val productId: String = BuildConfig.PREZZENCE_SUBSCRIPTION_PRODUCT_ID

    suspend fun refresh(): BillingUiState = withContext(Dispatchers.IO) {
        val connected = connect()
        if (!connected) return@withContext BillingUiState(false, false, productId, "Google Play Billing is unavailable on this device.")
        productDetails = querySubscriptionProduct()
        val entitled = queryActiveSubscription()
        val status = when {
            entitled.first -> "Active subscription restored from Google Play."
            productDetails == null -> "Subscription product '$productId' was not found in Google Play Console."
            else -> "Subscription is available."
        }
        onEntitlementChanged(entitled.first, status, entitled.second)
        BillingUiState(true, entitled.first, productId, status, productDetails?.displayPrice(), entitled.second)
    }

    suspend fun purchase(activity: Activity): BillingUiState {
        val connected = connect()
        if (!connected) return BillingUiState(false, false, productId, "Google Play Billing is unavailable on this device.")
        val details = productDetails ?: querySubscriptionProduct()?.also { productDetails = it }
        if (details == null) return BillingUiState(true, false, productId, "Subscription product '$productId' was not found in Google Play Console.")
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken.isNullOrBlank()) return BillingUiState(true, false, productId, "No active base plan or offer token is configured for '$productId'.")

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        return BillingUiState(true, false, productId, billingMessage(result))
    }

    suspend fun restore(): BillingUiState = refresh()

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases.isNullOrEmpty()) {
            onEntitlementChanged(false, billingMessage(billingResult), null)
            return
        }
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                acknowledgeIfNeeded(purchase)
                onEntitlementChanged(true, "Subscription active.", purchase.purchaseToken)
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    private suspend fun connect(): Boolean = suspendCancellableCoroutine { cont ->
        if (billingClient.isReady) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private suspend fun querySubscriptionProduct(): ProductDetails? = suspendCancellableCoroutine { cont ->
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { result, products ->
            cont.resume(if (result.responseCode == BillingClient.BillingResponseCode.OK) products.firstOrNull() else null)
        }
    }

    private suspend fun queryActiveSubscription(): Pair<Boolean, String?> = suspendCancellableCoroutine { cont ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            val activePurchase = purchases.firstOrNull { purchase ->
                purchase.products.contains(productId) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            val active = result.responseCode == BillingClient.BillingResponseCode.OK && activePurchase != null
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach(::acknowledgeIfNeeded)
            cont.resume(active to activePurchase?.purchaseToken)
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                onEntitlementChanged(true, "Subscription acknowledged.", purchase.purchaseToken)
            }
        }
    }

    private fun ProductDetails.displayPrice(): String? {
        return subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
    }

    private fun billingMessage(result: BillingResult): String {
        return result.debugMessage.ifBlank {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> "Billing request completed."
                BillingClient.BillingResponseCode.USER_CANCELED -> "Purchase canceled."
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Subscription already owned."
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Google Play Billing service is unavailable."
                else -> "Billing response ${result.responseCode}."
            }
        }
    }
}

data class BillingUiState(
    val connected: Boolean,
    val entitled: Boolean,
    val productId: String,
    val status: String,
    val price: String? = null,
    val purchaseToken: String? = null,
)