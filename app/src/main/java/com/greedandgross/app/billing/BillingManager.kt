package com.greedandgross.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener {
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium
    
    private val _connectionState = MutableStateFlow(BillingClient.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState
    
    companion object {
        const val PREMIUM_SUBSCRIPTION_ID = "premium_monthly_14_99"
        const val PREMIUM_SUBSCRIPTION_OFFER_ID = "monthly-offer"
    }
    
    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingClient.ConnectionState.CONNECTED
                    querySubscriptions()
                }
            }
            
            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingClient.ConnectionState.DISCONNECTED
            }
        })
    }
    
    private fun querySubscriptions() {
        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
            
        billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }
    
    fun purchasePremium(activity: Activity) {
        val productDetailsParams = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_SUBSCRIPTION_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
            
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(productDetailsParams))
            .build()
            
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: ""
                
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    )
                    .build()
                    
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }
    
    private fun handlePurchases(purchases: List<Purchase>) {
        val hasPremium = purchases.any { purchase ->
            purchase.products.contains(PREMIUM_SUBSCRIPTION_ID) && 
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        _isPremium.value = hasPremium
    }
    
    fun restorePurchases() {
        querySubscriptions()
    }
    
    fun endConnection() {
        billingClient.endConnection()
    }
}