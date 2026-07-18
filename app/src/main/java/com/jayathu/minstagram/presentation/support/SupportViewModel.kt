package com.jayathu.minstagram.presentation.support

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

// The tip jar. Tips are consumable so a generous person can tip again.
// If Play or the products aren't available, the screen degrades quietly.
@HiltViewModel
class SupportViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel(), PurchasesUpdatedListener {

    data class Tip(val details: ProductDetails, val label: String, val price: String)

    sealed interface State {
        data object Loading : State
        data class Ready(val tips: List<Tip>) : State
        data object Unavailable : State
    }

    var state by mutableStateOf<State>(State.Loading)
        private set

    var thanked by mutableStateOf(false)
        private set

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                } else {
                    update(State.Unavailable)
                }
            }

            override fun onBillingServiceDisconnected() {
                update(State.Unavailable)
            }
        })
    }

    private fun queryProducts() {
        val products = listOf("tip_small", "tip_medium", "tip_large").map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(products).build()
        ) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && details.isNotEmpty()) {
                val tips = details
                    .sortedBy { it.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0 }
                    .map { d ->
                        Tip(
                            details = d,
                            label = d.name.ifBlank { "Tip" },
                            price = d.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                        )
                    }
                update(State.Ready(tips))
            } else {
                update(State.Unavailable)
            }
        }
    }

    fun tip(activity: Activity, tip: Tip) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(tip.details)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                client.consumeAsync(
                    ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                ) { _, _ -> }
                viewModelScope.launch { thanked = true }
            }
    }

    private fun update(new: State) {
        viewModelScope.launch { state = new }
    }

    override fun onCleared() {
        client.endConnection()
    }
}
