package payment.sdk.android.cardpayment

import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import androidx.annotation.VisibleForTesting
import org.json.JSONObject
import payment.sdk.android.core.*

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

    override fun getOrder(orderUrl: String, paymentCookie: String, success: (String, String, Set<CardType>, orderAmount: OrderAmount) -> Unit,
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
                            ?.json("_links")?.json("payment:card")?.string("href")
                    val supportedCards = response.json("paymentMethods")
                            ?.array("card")?.toList<String>()

                    val orderValue = response.json("amount")!!.double("value")!!
                    val currencyCode = response.json("amount")!!.string("currencyCode")!!

                    val orderAmount = OrderAmount(orderValue, currencyCode)

                    success(orderReference!!, paymentUrl!!, CardMapping.mapSupportedCards(supportedCards!!), orderAmount)
                },
                error = { exception ->
                    error(exception)
                })
    }

    override fun doPayment(paymentUrl: String, paymentCookie: String, pan: String, expiry: String, cvv: String,
                           cardHolder: String, success: (state: String, response: JSONObject) -> Unit,
                           error: (Exception) -> Unit) {
        httpClient.put(
                url = paymentUrl,
                headers = mapOf(
                        HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                        HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                        HEADER_COOKIE to paymentCookie
                ),
                body = Body.Json(mapOf(
                        PAYMENT_FIELD_PAN to pan,
                        PAYMENT_FIELD_EXPIRY to expiry,
                        PAYMENT_FIELD_CVV to cvv,
                        PAYMENT_FIELD_CARDHOLDER to cardHolder
                )),
                success = { (_, response) ->
                    success(response.string("state")!!, response)
                },
                error = { exception ->
                    error(exception)
                })
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

        internal const val HEADER_ACCEPT = "Accept"
        internal const val HEADER_CONTENT_TYPE = "Content-Type"
        internal const val HEADER_COOKIE = "Cookie"
        internal const val HEADER_SET_COOKIE = "Set-Cookie"
    }
}


