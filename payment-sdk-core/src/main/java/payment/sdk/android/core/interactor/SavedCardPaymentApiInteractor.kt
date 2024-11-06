package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import java.lang.Exception

class SavedCardPaymentApiInteractor(
    private val httpClient: HttpClient,
) {
    suspend fun doSavedCardPayment(request: SavedCardPaymentApiRequest): SavedCardResponse {
        val bodyMap = mutableMapOf<String, Any>(
            KEY_EXPIRY to request.savedCard.expiry,
            KEY_CARD_TOKEN to request.savedCard.cardToken,
            KEY_CARDHOLDER_NAME to request.savedCard.cardholderName
        )
        request.cvv?.let {
            bodyMap.put(KEY_CVV, it)
        }
        request.visaRequest?.let {
            bodyMap.put(
                CardPaymentInteractor.PAYMENT_FIELD_VISA, mapOf(
                    CardPaymentInteractor.PAYMENT_FIELD_PLAN_SELECTION_INDICATOR to it.planSelectionIndicator,
                    CardPaymentInteractor.PAYMENT_FIELD_VISA_PLAN_ID to it.vPlanId,
                    CardPaymentInteractor.PAYMENT_FIELD_VISA_TERMS to it.acceptedTAndCVersion
                )
            )
        }
        request.payerIp?.let {
            bodyMap.put(KEY_PAYER_IP, it)
        }
        val response = httpClient.put(
            url = request.savedCardUrl,
            headers = mapOf(
                TransactionServiceHttpAdapter.HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                TransactionServiceHttpAdapter.HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                TransactionServiceHttpAdapter.HEADER_AUTHORIZATION to "Bearer ${request.accessToken}"
            ),
            body = Body.Json(bodyMap)
        )

        return when (response) {
            is SDKHttpResponse.Failed -> SavedCardResponse.Error(response.error)
            is SDKHttpResponse.Success -> {
                val paymentResponse = Gson().fromJson(response.body, PaymentResponse::class.java)
                SavedCardResponse.Success(paymentResponse)
            }
        }
    }

    companion object {
        internal const val KEY_EXPIRY = "expiry"
        internal const val KEY_CARD_TOKEN = "cardToken"
        internal const val KEY_CARDHOLDER_NAME = "cardholderName"
        internal const val KEY_CVV = "cvv"
        internal const val KEY_PAYER_IP = "payerIp"
    }
}

data class SavedCardPaymentApiRequest(
    val accessToken: String,
    val savedCardUrl: String,
    val savedCard: SavedCard,
    val payerIp: String?,
    val cvv: String?,
    val visaRequest: VisaRequest? = null
)

sealed class SavedCardResponse {
    data class Success(val paymentResponse: PaymentResponse) : SavedCardResponse()

    data class Error(val error: Exception) : SavedCardResponse()
}