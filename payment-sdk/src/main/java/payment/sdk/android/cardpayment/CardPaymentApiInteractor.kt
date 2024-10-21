package payment.sdk.android.cardpayment

import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import org.json.JSONObject
import payment.sdk.android.cardpayment.threedsecuretwo.webview.BrowserData
import payment.sdk.android.core.*
import payment.sdk.android.core.interactor.VisaRequest

internal class CardPaymentApiInteractor(private val httpClient: HttpClient) : PaymentApiInteractor {

    override fun authorizePayment(url: String, code: String, success: (List<String>, String) -> Unit, error: (Exception) -> Unit) {
        httpClient.post(
                url = url,
                headers = mapOf(
                        HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                        HEADER_CONTENT_TYPE to "application/x-www-form-urlencoded"
                ),
                body = Body.Form(mapOf(
                        "code" to code
                )),
                success = { (headers, response) ->
                    val cookies = headers[HEADER_SET_COOKIE]
                    val orderUrl = response.json("_links")?.json("cnp:order")
                            ?.string("href")
                    success(cookies!!, orderUrl!!)
                },
                error = { exception ->
                    error(exception)
                })
    }

    override fun getOrder(orderUrl: String,
                          paymentCookie: String,
                          success: (String, String, Set<CardType>, orderAmount: OrderAmount, String, String, JSONObject) -> Unit,
                          error: (Exception) -> Unit) {
        httpClient.get(
                url = orderUrl,
                headers = mapOf(
                        HEADER_COOKIE to paymentCookie
                ),
                success = { (_, response) ->
                    val orderReference = response.string("reference")
                    val paymentUrl = response.json("_embedded")
                            ?.array("payment")?.at(0)
                            ?.json("_links")?.json("payment:card")?.string("href") ?: ""
                    val supportedCards = response.json("paymentMethods")
                            ?.array("card")?.toList<String>()

                    val orderValue = response.json("amount")!!.double("value")!!
                    val currencyCode = response.json("amount")!!.string("currencyCode")!!
                    val outletRef = response.string("outletId")
                    val orderAmount = OrderAmount(orderValue, currencyCode)
                    val selfLink = response.json("_embedded")
                        ?.array("payment")?.at(0)
                        ?.json("_links")?.json("self")?.string("href") ?: ""
                    success(orderReference!!,
                        paymentUrl,
                        CardMapping.mapSupportedCards(supportedCards!!),
                        orderAmount,
                        outletRef!!,
                        selfLink,
                        response
                    )
                },
                error = { exception ->
                    error(exception)
                })
    }

    override fun doPayment(paymentUrl: String, paymentCookie: String, pan: String, expiry: String, cvv: String,
                           cardHolder: String, payerIp: String?, visRequest: VisaRequest?, success: (state: String, response: JSONObject) -> Unit,
                           error: (Exception) -> Unit) {
        val bodyMap = mutableMapOf<String, Any>(
            PAYMENT_FIELD_PAN to pan,
            PAYMENT_FIELD_EXPIRY to expiry,
            PAYMENT_FIELD_CVV to cvv,
            PAYMENT_FIELD_CARDHOLDER to cardHolder
        )
        payerIp?.let {
            bodyMap.put(PAYMENT_FIELD_PAYER_IP, it)
        }
        visRequest?.let {
            bodyMap.put(
                PAYMENT_FIELD_VISA, mapOf(
                    PAYMENT_FIELD_PLAN_SELECTION_INDICATOR to it.planSelectionIndicator,
                    PAYMENT_FIELD_VISA_PLAN_ID to it.vPlanId,
                    PAYMENT_FIELD_VISA_TERMS to it.acceptedTAndCVersion
                )
            )
        }
        httpClient.put(
                url = paymentUrl,
                headers = mapOf(
                        HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                        HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                        HEADER_COOKIE to paymentCookie
                ),
                body = Body.Json(bodyMap),
                success = { (_, response) ->
                    success(response.string("state")!!, response)
                },
                error = { exception ->
                    error(exception)
                })
    }

    override fun postThreeDSTwoBrowserAuthentications(
        browserData: BrowserData,
        threeDSCompInd: String,
        threeDSAuthenticationsUrl: String,
        paymentCookie: String,
        notificationUrl: String,
        success: (response: JSONObject) -> Unit,
        error: (Exception) -> Unit
    ) {
        httpClient.post(
            url = threeDSAuthenticationsUrl,
            headers = mapOf(
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                HEADER_COOKIE to paymentCookie
            ),
            body = Body.Json(mapOf(
                DEVICE_CHANNEL_KEY to "BRW",
                THREE_DS_COMP_IND to threeDSCompInd,
                NOTIFICATION_URL to notificationUrl,
                BROWSER_INFO to browserData.getHashMap()
            )),
            success = { (_, response) ->
                success(response)
            },
            error = { exception ->
                    error(exception)
            })
    }

    override fun postThreeDSTwoChallengeResponse(
        threeDSTwoChallengeResponseURL: String,
        paymentCookie: String,
        success: (state: String, response: JSONObject) -> Unit,
        error: (Exception) -> Unit
    ) {
        httpClient.post(
            url = threeDSTwoChallengeResponseURL,
            headers = mapOf(
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                HEADER_COOKIE to paymentCookie
            ),
            body = Body.Json(emptyMap()),
            success = { (_, response) ->
                success(response.string("state")!!, response)
            },
            error = { exception ->
                error(exception)
            })
    }

    override fun getPayerIP(
        requestIpUrl: String,
        paymentCookie: String,
        success: (response: JSONObject) -> Unit,
        error: (Exception) -> Unit
    ) {
        httpClient.get(
            url = requestIpUrl,
            headers = mapOf(
                HEADER_COOKIE to paymentCookie
            ),
            success = { (_, response) ->
                success(response)
            },
            error = { exception ->
                error(exception)
            }
        )
    }

    override fun getPayerIp(
        url: String,
        success: (response: String?) -> Unit,
        error: (Exception) -> Unit
    ) {
        httpClient.get(
            url = url,
            headers = emptyMap(),
            success = { (_, response) ->
                success(response.string("requesterIp"))
            },
            error = { exception ->
                error(exception)
            }
        )
    }

    override fun visaEligibilityCheck(
        url: String,
        token: String,
        cardNumber: String,
        success: (isEligible: Boolean, plans: VisaPlans) -> Unit,
        error: (Exception) -> Unit
    ) {
        httpClient.post(
            url = url,
            headers = mapOf(
                HEADER_COOKIE to token,
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json"
            ),
            body = Body.Json(mapOf(
                PAYMENT_FIELD_PAN to cardNumber
            )),
            success = { (_, response) ->
                val plans = Gson().fromJson(response.toString(), VisaPlans::class.java)
                success(plans.matchedPlans.isNotEmpty(), plans)
            },
            error = { exception ->
                error(exception)
            }
        )
    }

    companion object {
        @VisibleForTesting
        internal const val PAYMENT_FIELD_PAN = "pan"
        @VisibleForTesting
        internal const val PAYMENT_FIELD_EXPIRY = "expiry"
        @VisibleForTesting
        internal const val PAYMENT_FIELD_CVV = "cvv"
        @VisibleForTesting
        internal const val PAYMENT_FIELD_CARDHOLDER = "cardholderName"

        @VisibleForTesting
        internal const val PAYMENT_FIELD_PAYER_IP = "payerIp"

        internal const val HEADER_ACCEPT = "Accept"
        internal const val HEADER_CONTENT_TYPE = "Content-Type"
        internal const val HEADER_COOKIE = "Cookie"
        internal const val HEADER_SET_COOKIE = "Set-Cookie"

        internal const val DEVICE_CHANNEL_KEY = "deviceChannel"
        internal const val THREE_DS_COMP_IND = "threeDSCompInd"
        internal const val BROWSER_INFO = "browserInfo"
        internal const val NOTIFICATION_URL = "notificationURL"
        internal const val PAYMENT_FIELD_VISA = "vis"
        internal const val PAYMENT_FIELD_PLAN_SELECTION_INDICATOR = "planSelectionIndicator"
        internal const val PAYMENT_FIELD_VISA_PLAN_ID = "vPlanId"
        internal const val PAYMENT_FIELD_VISA_TERMS = "acceptedTAndCVersion"
    }
}


