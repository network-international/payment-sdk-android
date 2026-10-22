package payment.sdk.android.googlepay

import android.content.Context
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Device-level Google Pay availability. Use this to show or hide a merchant-owned Google Pay
 * button. A ready device is not enough to pay — the order must also list `GOOGLE_PAY` in
 * `paymentMethods.wallet`.
 */
object GooglePayAvailability {

    private val defaultNetworks = listOf("AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA")
    private val defaultAuthMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")

    suspend fun isReady(context: Context, googlePayConfig: GooglePayConfig): Boolean {
        return try {
            val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(googlePayConfig.env())
                .build()
            val client = Wallet.getPaymentsClient(context.applicationContext, walletOptions)
            val jsonConfig = GooglePayJsonConfig()
            val methods = jsonConfig.getAllowedPaymentMethods(
                allowedAuthMethods = defaultAuthMethods,
                allowedCardNetworks = defaultNetworks,
                merchantGatewayId = googlePayConfig.merchantGatewayId,
                gateway = "networkintl"
            )
            val request = IsReadyToPayRequest.fromJson(jsonConfig.isReadyToPayRequest(methods))
            client.isReadyToPay(request).await()
        } catch (_: Exception) {
            false
        }
    }

    fun isReady(
        context: Context,
        googlePayConfig: GooglePayConfig,
        callback: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            callback(isReady(context, googlePayConfig))
        }
    }
}
