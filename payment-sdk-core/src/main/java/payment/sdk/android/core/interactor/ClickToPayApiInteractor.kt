package payment.sdk.android.core.interactor

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

/**
 * Interactor for Click to Pay API operations.
 * Handles communication between the SDK and the payment gateway for Click to Pay transactions.
 *
 * API Endpoint format:
 * POST /api/outlets/{outletId}/orders/{orderId}/payments/{paymentRef}/unified-click-to-pay
 */
@Keep
class ClickToPayApiInteractor(
    private val httpClient: HttpClient,
    private val app: Application
) {
    /**
     * Submit the Click to Pay checkout response to the unified-click-to-pay API
     *
     * @param unifiedClickToPayUrl The unified-click-to-pay URL (e.g., .../unified-click-to-pay)
     * @param checkoutResponse The JWS-signed checkout response from Click to Pay SDK
     * @param srcDigitalCardId The digital card ID used for checkout (for saved cards)
     * @param accessToken The access token for authentication
     * @param paymentCookie The payment cookie
     * @return The API response
     */
    suspend fun submitClickToPayPayment(
        unifiedClickToPayUrl: String,
        checkoutResponse: String,
        srcDigitalCardId: String?,
        accessToken: String,
        paymentCookie: String
    ): ClickToPayPaymentResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Submitting Click to Pay payment to: $unifiedClickToPayUrl")

            // Build the request body
            val bodyMap = mutableMapOf<String, Any>(
                "checkoutResponse" to checkoutResponse
            )
            // Only include srcDigitalCardId if it's provided (for saved cards)
            srcDigitalCardId?.let {
                bodyMap["srcDigitalCardId"] = it
            }

            val headers = mutableMapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json",
                "Authorization" to "Bearer $accessToken",
                "Access-Token" to accessToken,
                "Cookie" to paymentCookie
            )
            // Extract token value from cookie format "payment-token=<VALUE>;..."
            extractPaymentToken(paymentCookie)?.let {
                headers["Payment-Token"] = it
            }

            val response = httpClient.post(
                url = unifiedClickToPayUrl,
                headers = headers,
                body = Body.Json(bodyMap)
            )

            when (response) {
                is SDKHttpResponse.Success -> {
                    Log.d(TAG, "Click to Pay response: ${response.body}")
                    val jsonResponse = JSONObject(response.body)
                    val state = jsonResponse.optString("state", "")

                    when (state) {
                        "AUTHORISED" -> ClickToPayPaymentResult.Authorised
                        "PURCHASED" -> ClickToPayPaymentResult.Purchased
                        "CAPTURED" -> ClickToPayPaymentResult.Captured
                        "AWAIT_3DS" -> parse3DSResponse(jsonResponse)
                        "PENDING" -> ClickToPayPaymentResult.Pending
                        "FAILED" -> {
                            val message = jsonResponse.optString("message", "Payment failed")
                            ClickToPayPaymentResult.Failed(message)
                        }
                        "POST_AUTH_REVIEW" -> ClickToPayPaymentResult.PostAuthReview
                        else -> ClickToPayPaymentResult.Failed("Unknown payment state: $state")
                    }
                }
                is SDKHttpResponse.Failed -> {
                    Log.e(TAG, "Click to Pay API failed: ${response.error.message}")
                    ClickToPayPaymentResult.Failed(response.error.message ?: "Unknown error")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Click to Pay exception: ${e.message}", e)
            ClickToPayPaymentResult.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Get order status for polling
     */
    suspend fun getOrder(
        orderUrl: String,
        accessToken: String
    ): ClickToPayPaymentResult = withContext(Dispatchers.IO) {
        try {
            val headers = mapOf(
                "Accept" to "application/vnd.ni-payment.v2+json",
                "Authorization" to "Bearer $accessToken"
            )

            val response = httpClient.get(
                url = orderUrl,
                headers = headers,
                body = Body.Empty()
            )

            when (response) {
                is SDKHttpResponse.Success -> {
                    val jsonResponse = JSONObject(response.body)
                    val embedded = jsonResponse.optJSONObject("_embedded")
                    val payments = embedded?.optJSONArray("payment")
                    val payment = payments?.optJSONObject(0)
                    val state = payment?.optString("state", "") ?: ""

                    when (state) {
                        "AUTHORISED" -> ClickToPayPaymentResult.Authorised
                        "PURCHASED" -> ClickToPayPaymentResult.Purchased
                        "CAPTURED" -> ClickToPayPaymentResult.Captured
                        "AWAIT_3DS" -> {
                            if (payment != null) parse3DSResponse(payment) else ClickToPayPaymentResult.Pending
                        }
                        "PENDING" -> ClickToPayPaymentResult.Pending
                        "FAILED" -> {
                            val message = payment?.optString("message", "Payment failed") ?: "Payment failed"
                            ClickToPayPaymentResult.Failed(message)
                        }
                        "POST_AUTH_REVIEW" -> ClickToPayPaymentResult.PostAuthReview
                        else -> ClickToPayPaymentResult.Pending
                    }
                }
                is SDKHttpResponse.Failed -> {
                    Log.e(TAG, "Get order failed: ${response.error.message}")
                    ClickToPayPaymentResult.Failed(response.error.message ?: "Unknown error")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get order exception: ${e.message}", e)
            ClickToPayPaymentResult.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Build the unified-click-to-pay URL from order details
     */
    fun buildUnifiedClickToPayUrl(
        basePaymentUrl: String,
        outletId: String,
        orderId: String,
        paymentRef: String
    ): String {
        // Extract the base URL (e.g., https://paypage-sandbox.platform.network.ae)
        val baseUrl = basePaymentUrl.substringBefore("/api/")
        return "$baseUrl/api/outlets/$outletId/orders/$orderId/payments/$paymentRef/unified-click-to-pay"
    }

    private fun parse3DSResponse(jsonResponse: JSONObject): ClickToPayPaymentResult {
        // The unified-click-to-pay response nests 3DS data under "order":
        // {
        //   "state": "AWAIT_3DS",
        //   "order": {
        //     "threeDs2": { "threeDSMethodURL", "threeDSServerTransID", "messageVersion",
        //                   "threeDSMethodData", "threeDSMethodNotificationURL" },
        //     "_embedded": { "payment": [{ "3ds2": { "directoryServerID", ... },
        //       "_links": { "cnp:3ds2-authentication": {...}, "cnp:3ds2-challenge-response": {...} },
        //       "outletId": "...", "orderReference": "...", "reference": "..." }] }
        //   }
        // }
        // For getOrder path, jsonResponse IS the payment object directly.
        val order = jsonResponse.optJSONObject("order")
        val payment = order?.optJSONObject("_embedded")
            ?.optJSONArray("payment")
            ?.optJSONObject(0)

        // Look for 3DS2 data in multiple locations
        val threeDSTwo = jsonResponse.optJSONObject("3ds2")
            ?: order?.optJSONObject("threeDs2")
            ?: payment?.optJSONObject("3ds2")

        val threeDSOne = jsonResponse.optJSONObject("3ds")
            ?: order?.optJSONObject("3ds")
            ?: payment?.optJSONObject("3ds")

        if (threeDSTwo != null) {
            // Also check payment-level 3ds2 for directoryServerID
            val paymentThreeDS = payment?.optJSONObject("3ds2")

            // Extract outlet/order/payment refs from payment, order, or jsonResponse (getOrder path)
            val outletRef = payment?.optString("outletId")?.takeIf { it.isNotEmpty() }
                ?: order?.optString("outletId")?.takeIf { it.isNotEmpty() }
                ?: jsonResponse.optString("outletId").takeIf { it.isNotEmpty() }
            val orderRef = payment?.optString("orderReference")?.takeIf { it.isNotEmpty() }
                ?: order?.optString("reference")?.takeIf { it.isNotEmpty() }
                ?: jsonResponse.optString("orderReference").takeIf { it.isNotEmpty() }
            val paymentRef = payment?.optString("reference")?.takeIf { it.isNotEmpty() }
                ?: order?.optString("paymentReference")?.takeIf { it.isNotEmpty() }
                ?: jsonResponse.optString("reference").takeIf { it.isNotEmpty() }

            val threeDSServerTransId = threeDSTwo.optString("threeDSServerTransID").takeIf { it.isNotEmpty() }

            // Read pre-computed threeDSMethodData and threeDSMethodNotificationURL from the response.
            // The server provides these with the correct public URLs; our own computation would
            // fail because the _links URLs use internal service hostnames (http://transaction-service/...).
            val notificationUrl = threeDSTwo.optString("threeDSMethodNotificationURL").takeIf { it.isNotEmpty() }
            val methodData = threeDSTwo.optString("threeDSMethodData").takeIf { it.isNotEmpty() }

            // The _links URLs may be internal service URLs (http://transaction-service/...)
            // that aren't accessible from the device. We need to construct proper gateway URLs.
            // Use the notification URL (which has a real domain) for environment detection.
            val envHint = notificationUrl ?: ""
            val authUrl: String?
            val challengeUrl: String?
            if (outletRef != null && orderRef != null && paymentRef != null) {
                val gatewayBase = resolveGatewayBaseUrl(envHint)
                authUrl = "$gatewayBase/transactions/outlets/$outletRef/orders/$orderRef/payments/$paymentRef/card/3ds2/authentications"
                challengeUrl = "$gatewayBase/transactions/outlets/$outletRef/orders/$orderRef/payments/$paymentRef/card/3ds2/challenge-response"
            } else {
                // Fallback: try to use _links directly (may be internal but best effort)
                val links = payment?.optJSONObject("_links")
                    ?: jsonResponse.optJSONObject("_links")
                authUrl = links?.optJSONObject("cnp:3ds2-authentication")
                    ?.optString("href")?.takeIf { it.isNotEmpty() }
                challengeUrl = links?.optJSONObject("cnp:3ds2-challenge-response")
                    ?.optString("href")?.takeIf { it.isNotEmpty() }
            }

            return ClickToPayPaymentResult.Requires3DSTwo(
                threeDSMethodUrl = threeDSTwo.optString("threeDSMethodURL").takeIf { it.isNotEmpty() },
                threeDSServerTransId = threeDSServerTransId,
                directoryServerId = (paymentThreeDS ?: threeDSTwo).optString("directoryServerID").takeIf { it.isNotEmpty() },
                threeDSMessageVersion = threeDSTwo.optString("messageVersion").takeIf { it.isNotEmpty() },
                acsUrl = threeDSTwo.optString("acsURL").takeIf { it.isNotEmpty() },
                threeDSTwoAuthenticationURL = authUrl,
                threeDSTwoChallengeResponseURL = challengeUrl,
                outletRef = outletRef,
                orderRef = orderRef,
                paymentRef = paymentRef,
                threeDSMethodData = methodData,
                threeDSMethodNotificationURL = notificationUrl
            )
        } else if (threeDSOne != null) {
            return ClickToPayPaymentResult.Requires3DS(
                acsUrl = threeDSOne.optString("acsUrl"),
                acsPaReq = threeDSOne.optString("acsPaReq"),
                acsMd = threeDSOne.optString("acsMd")
            )
        }
        return ClickToPayPaymentResult.Failed("3DS required but data missing")
    }

    /**
     * Resolve the API gateway base URL from an environment hint URL.
     * The hint is typically the threeDSMethodNotificationURL which contains
     * a real public domain with environment markers (sandbox, dev, ksa, etc.).
     */
    private fun resolveGatewayBaseUrl(environmentHintUrl: String): String {
        val isKsa = environmentHintUrl.contains("ksa", ignoreCase = true)
        return when {
            environmentHintUrl.contains("sandbox", ignoreCase = true) ->
                if (isKsa) "https://api-gateway.sandbox.ksa.ngenius-payments.com"
                else "https://api-gateway.sandbox.ngenius-payments.com"
            environmentHintUrl.contains("-uat", ignoreCase = true) ->
                if (isKsa) "https://api-gateway.sandbox.ksa.ngenius-payments.com"
                else "https://api-gateway-uat.ngenius-payments.com"
            environmentHintUrl.contains("-dev", ignoreCase = true) ->
                if (isKsa) "https://api-gateway.infradev.ksa.ngenius-payments.com"
                else "https://api-gateway-dev.ngenius-payments.com"
            else ->
                if (isKsa) "https://api-gateway.ksa.ngenius-payments.com"
                else "https://api-gateway.ngenius-payments.com"
        }
    }

    companion object {
        private const val TAG = "ClickToPayApiInteractor"

        /**
         * Extract the token value from a payment cookie string.
         * Cookie format: "payment-token=<VALUE>;Path=/;..." or just "payment-token=<VALUE>"
         * Returns just the VALUE portion.
         */
        fun extractPaymentToken(paymentCookie: String): String? {
            if (!paymentCookie.contains("payment-token=")) return null
            return paymentCookie
                .substringAfter("payment-token=")
                .substringBefore(";")
                .trim()
                .takeIf { it.isNotEmpty() }
        }
    }
}

/**
 * Result of Click to Pay payment submission
 */
@Keep
sealed class ClickToPayPaymentResult {
    object Authorised : ClickToPayPaymentResult()
    object Purchased : ClickToPayPaymentResult()
    object Captured : ClickToPayPaymentResult()
    object PostAuthReview : ClickToPayPaymentResult()
    object Pending : ClickToPayPaymentResult()

    data class Requires3DS(
        val acsUrl: String,
        val acsPaReq: String,
        val acsMd: String
    ) : ClickToPayPaymentResult()

    data class Requires3DSTwo(
        val threeDSMethodUrl: String?,
        val threeDSServerTransId: String?,
        val directoryServerId: String?,
        val threeDSMessageVersion: String?,
        val acsUrl: String?,
        val threeDSTwoAuthenticationURL: String?,
        val threeDSTwoChallengeResponseURL: String?,
        val outletRef: String?,
        val orderRef: String?,
        val paymentRef: String?,
        val threeDSMethodData: String?,
        val threeDSMethodNotificationURL: String?
    ) : ClickToPayPaymentResult()

    data class Failed(val message: String) : ClickToPayPaymentResult()
}
