package com.bookmer.browser.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class GoogleSubscriptionRegistration(val active: Boolean, val expiresMs: Long)

/**
 * PRO subscription via Google Play Billing. The purchase unlocks the device; the Bookmer account
 * becomes PRO when `POST /pay/google` accepts the purchase token — same row Stripe / Apple write.
 */
class BookmerProStore(
    context: Context,
    private val api: BookmerApiClient,
    private val session: SecureSessionStore,
    private val onAccountSynced: () -> Unit = {},
) : PurchasesUpdatedListener {
    companion object {
        /** Play Console: auto-renewable yearly subscription, 29 EUR. */
        const val YEARLY_PRODUCT_ID = "com.bookmer.browser.pro.yearly"
    }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private var productDetails by mutableStateOf<ProductDetails?>(null)
    var isLoadingProduct by mutableStateOf(false)
        private set
    var isPurchasing by mutableStateOf(false)
        private set
    var isRestoring by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set
    var hasPlayEntitlement by mutableStateOf(false)
        private set
    var storeExpiryMs by mutableStateOf<Long?>(null)
        private set
    var isSyncingAccount by mutableStateOf(false)
        private set
    var accountSyncFailed by mutableStateOf(false)
        private set
    private var activePurchaseToken by mutableStateOf<String?>(null)

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enableAutoServiceReconnection()
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    val isPro: Boolean
        get() = hasPlayEntitlement || session.value.hasPro

    val displayPrice: String
        get() {
            val offer = productDetails?.subscriptionOfferDetails?.firstOrNull()
            val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
            val micros = phase?.priceAmountMicros
            val currency = phase?.priceCurrencyCode
            if (micros != null && !currency.isNullOrBlank()) {
                val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
                format.currency = Currency.getInstance(currency)
                return format.format(micros / 1_000_000.0)
            }
            return phase?.formattedPrice ?: "€29.00"
        }

    fun start() {
        if (billingClient.isReady) {
            loadProduct()
            refreshEntitlement(syncAccount = true)
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProduct()
                    refreshEntitlement(syncAccount = true)
                } else {
                    lastError = result.debugMessage.ifBlank { "Google Play Billing is unavailable" }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Will reconnect on next purchase / restore attempt.
            }
        })
    }

    fun loadProduct() {
        if (!billingClient.isReady || isLoadingProduct) return
        isLoadingProduct = true
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(YEARLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            main.post {
                isLoadingProduct = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    productDetails = queryResult.productDetailsList.firstOrNull()
                } else {
                    lastError = result.debugMessage.ifBlank { "The subscription is not available right now." }
                }
            }
        }
    }

    fun purchase(activity: Activity) {
        if (isPurchasing) return
        if (!session.value.isSignedIn) {
            lastError = "Sign in first so the subscription is tied to your Bookmer account."
            return
        }
        if (!billingClient.isReady) {
            start()
            lastError = "Google Play Billing is starting — try again in a moment."
            return
        }
        val details = productDetails
        if (details == null) {
            loadProduct()
            lastError = "The subscription is not available right now."
            return
        }
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken.isNullOrBlank()) {
            lastError = "The subscription is not available right now."
            return
        }
        isPurchasing = true
        lastError = null
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        val launch = billingClient.launchBillingFlow(activity, params)
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            isPurchasing = false
            lastError = launch.debugMessage.ifBlank { "Could not start the Google Play purchase" }
        }
    }

    fun restore() {
        if (isRestoring) return
        isRestoring = true
        lastError = null
        refreshEntitlement(syncAccount = true) {
            isRestoring = false
            if (!hasPlayEntitlement) {
                lastError = "No active subscription found for this Google account."
            }
        }
    }

    fun openManageSubscriptions() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/account/subscriptions?sku=$YEARLY_PRODUCT_ID&package=${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    fun refreshEntitlement(syncAccount: Boolean, done: (() -> Unit)? = null) {
        if (!billingClient.isReady) {
            done?.invoke()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            main.post {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    done?.invoke()
                    return@post
                }
                val active = purchases.firstOrNull { purchase ->
                    purchase.products.contains(YEARLY_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                hasPlayEntitlement = active != null
                activePurchaseToken = active?.purchaseToken
                storeExpiryMs = null
                if (active != null) {
                    acknowledgeIfNeeded(active)
                    if (syncAccount) syncToAccount(active, done) else done?.invoke()
                } else {
                    done?.invoke()
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        main.post {
            isPurchasing = false
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases.orEmpty().forEach { purchase ->
                        if (purchase.products.contains(YEARLY_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        ) {
                            hasPlayEntitlement = true
                            activePurchaseToken = purchase.purchaseToken
                            acknowledgeIfNeeded(purchase)
                            syncToAccount(purchase)
                        }
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                else -> lastError = result.debugMessage.ifBlank { "Purchase failed" }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { /* best-effort; server also acknowledges */ }
    }

    private fun syncToAccount(purchase: Purchase, done: (() -> Unit)? = null) {
        val token = session.value.token
        if (token.isNullOrBlank()) {
            done?.invoke()
            return
        }
        isSyncingAccount = true
        api.registerGoogleSubscription(
            token = token,
            productId = YEARLY_PRODUCT_ID,
            purchaseToken = purchase.purchaseToken,
            packageName = appContext.packageName,
            orderId = purchase.orderId,
        ) { result ->
            result.onSuccess {
                accountSyncFailed = false
                storeExpiryMs = it.expiresMs.takeIf { ms -> ms > 0 }
                api.fetchUser(token) { profile ->
                    isSyncingAccount = false
                    profile.onSuccess { json ->
                        applyProfilePro(json)
                        onAccountSynced()
                    }.onFailure {
                        accountSyncFailed = true
                    }
                    done?.invoke()
                }
            }.onFailure {
                isSyncingAccount = false
                accountSyncFailed = true
                lastError = it.message
                done?.invoke()
            }
        }
    }

    private fun applyProfilePro(json: JSONObject) {
        val data = json.optJSONObject("data") ?: json
        val extras = data.optJSONObject("extras") ?: JSONObject()
        val subscription = extras.optJSONObject("currentSubscription") ?: JSONObject()
        val accountType = data.optString("accountType")
        val pro = extras.optBoolean("lifetimeDeal") ||
            subscription.optBoolean("active") ||
            accountType in setOf("LIFETIME", "FREE_TRIAL", "PRO", "SUBSCRIPTION")
        session.updateProfile(
            email = data.optString("email").takeIf { it.isNotBlank() } ?: session.value.email,
            name = data.optString("name").ifBlank { data.optString("displayName") }.takeIf { it.isNotBlank() },
            avatarUrl = data.optString("picture").ifBlank { data.optString("avatar") }.takeIf { it.isNotBlank() },
            accountType = accountType.takeIf { it.isNotBlank() },
            hasPro = pro,
        )
        if (subscription.has("end")) {
            val end = subscription.opt("end")
            storeExpiryMs = when (end) {
                is Number -> if (end.toLong() > 1_000_000_000_000L) end.toLong() else end.toLong() * 1000
                is String -> end.toLongOrNull()?.let { if (it > 1_000_000_000_000L) it else it * 1000 }
                else -> storeExpiryMs
            }
        }
    }
}
