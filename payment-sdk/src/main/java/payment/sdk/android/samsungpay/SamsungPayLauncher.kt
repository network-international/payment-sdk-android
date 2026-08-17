package payment.sdk.android.samsungpay

import android.content.Context
import android.os.Parcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.Order
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.payments.SamsungPayConfig

/**
 * Launches Samsung Pay without opening the unified payment page.
 *
 * The launcher authorizes the order (when no payment-token is supplied), opens the Samsung Pay
 * sheet, posts the credential to the gateway, and reports Success / Failed / Cancelled.
 *
 * Usage:
 * ```
 * private val samsungPayLauncher = SamsungPayLauncher(this)
 *
 * samsungPayLauncher.launch(
 *     order = order,
 *     config = SamsungPayConfig(serviceId = "…", merchantName = "…")
 * ) { result ->
 *     when (result) {
 *         SamsungPayLauncher.Result.Success -> …
 *         is SamsungPayLauncher.Result.Failed -> …
 *         SamsungPayLauncher.Result.Cancelled -> …
 *     }
 * }
 * ```
 */
class SamsungPayLauncher(
    private val context: Context,
    private val starter: (Order, SamsungPayConfig, SamsungPayResponse) -> Unit = { order, config, response ->
        SamsungPayClient(context, config.serviceId, CoroutinesGatewayHttpClient())
            .startSamsungPay(
                order = order,
                merchantName = config.merchantName,
                paymentToken = null,
                samsungPayResponse = response
            )
    }
) {
    fun launch(
        order: Order,
        config: SamsungPayConfig,
        callback: (Result) -> Unit
    ) {
        starter(
            order,
            config,
            object : SamsungPayResponse {
                override fun onSuccess() = callback(Result.Success)
                override fun onFailure(error: String) = callback(Result.Failed(error))
                override fun onCancelled() = callback(Result.Cancelled)
            }
        )
    }

    suspend fun isAvailable(serviceId: String): Boolean =
        SamsungPayClient(context, serviceId, CoroutinesGatewayHttpClient()).isSamsungPayAvailable()

    fun isAvailable(serviceId: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            callback(isAvailable(serviceId))
        }
    }

    sealed class Result : Parcelable {
        @Parcelize
        data object Success : Result()

        @Parcelize
        data class Failed(val error: String) : Result()

        @Parcelize
        data object Cancelled : Result()
    }
}
