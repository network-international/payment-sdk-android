package payment.sdk.android.core.interactor

import androidx.annotation.Keep
import com.google.gson.Gson
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class CardPaymentInteractor(
    private val httpClient: HttpClient
) {
    suspend fun makeCardPayment(request: MakeCardPaymentRequest): CardPaymentResponse {
        val bodyMap = mutableMapOf<String, Any>(
            PAYMENT_FIELD_PAN to request.pan,
            PAYMENT_FIELD_EXPIRY to request.expiry,
            PAYMENT_FIELD_CVV to request.cvv,
            PAYMENT_FIELD_CARDHOLDER to request.cardHolder
        )
        request.visaRequest?.let {
            bodyMap.put(
                PAYMENT_FIELD_VISA, mapOf(
                    PAYMENT_FIELD_PLAN_SELECTION_INDICATOR to it.planSelectionIndicator,
                    PAYMENT_FIELD_VISA_PLAN_ID to it.vPlanId,
                    PAYMENT_FIELD_VISA_TERMS to it.acceptedTAndCVersion
                )
            )
        }
        request.payerIp?.let {
            bodyMap.put(KEY_PAYER_IP, it)
        }
        val response = httpClient.put(
            url = request.paymentUrl,
            headers = mapOf(
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                HEADER_COOKIE to request.paymentCookie
            ),
            body = Body.Json(bodyMap)
        )
        return when (response) {
            is SDKHttpResponse.Failed -> CardPaymentResponse.Error(response.error)
            is SDKHttpResponse.Success -> {
                val paymentResponse = Gson().fromJson(response.body, PaymentResponse::class.java)
                CardPaymentResponse.Success(paymentResponse)
            }
        }
    }

    companion object {
        internal const val PAYMENT_FIELD_PAN = "pan"
        internal const val PAYMENT_FIELD_EXPIRY = "expiry"
        internal const val PAYMENT_FIELD_CVV = "cvv"
        internal const val PAYMENT_FIELD_CARDHOLDER = "cardholderName"
        internal const val PAYMENT_FIELD_PLAN_SELECTION_INDICATOR = "planSelectionIndicator"
        internal const val PAYMENT_FIELD_VISA_PLAN_ID = "vPlanId"
        internal const val PAYMENT_FIELD_VISA_TERMS = "acceptedTAndCVersion"
        internal const val HEADER_CONTENT_TYPE = "Content-Type"
        internal const val HEADER_COOKIE = "Cookie"
        internal const val HEADER_ACCEPT = "Accept"
        internal const val KEY_PAYER_IP = "payerIp"
        internal const val PAYMENT_FIELD_VISA = "vis"
    }
}

data class MakeCardPaymentRequest(
    val paymentCookie: String,
    val paymentUrl: String,
    val pan: String,
    val cvv: String,
    val cardHolder: String,
    val expiry: String,
    val payerIp: String? = null,
    val visaRequest: VisaRequest? = null
)

@Keep
sealed class CardPaymentResponse {
    @Keep
    data class Success(val paymentResponse: PaymentResponse) : CardPaymentResponse()

    @Keep
    data class Error(val error: Exception) : CardPaymentResponse()
}