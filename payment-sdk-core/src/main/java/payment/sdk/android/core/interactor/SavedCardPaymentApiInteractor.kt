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
    suspend fun doSavedCardPayment(
        accessToken: String,
        savedCardUrl: String,
        savedCard: SavedCard,
        payerIp: String?,
        cvv: String?,
        visaRequest: VisaRequest? = null
    ): SavedCardResponse {
        val bodyMap = mutableMapOf<String, Any>(
            KEY_EXPIRY to savedCard.expiry,
            KEY_CARD_TOKEN to savedCard.cardToken,
            KEY_CARDHOLDER_NAME to savedCard.cardholderName
        )
        cvv?.let {
            bodyMap.put(KEY_CVV, it)
        }
        visaRequest?.let {
            bodyMap.put(
                CardPaymentInteractor.PAYMENT_FIELD_VISA, mapOf(
                    CardPaymentInteractor.PAYMENT_FIELD_PLAN_SELECTION_INDICATOR to it.planSelectionIndicator,
                    CardPaymentInteractor.PAYMENT_FIELD_VISA_PLAN_ID to it.vPlanId,
                    CardPaymentInteractor.PAYMENT_FIELD_VISA_TERMS to it.acceptedTAndCVersion
                )
            )
        }
        payerIp?.let {
            bodyMap.put(KEY_PAYER_IP, it)
        }
        val response = httpClient.put(
            url = savedCardUrl,
            headers = mapOf(
                TransactionServiceHttpAdapter.HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                TransactionServiceHttpAdapter.HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                TransactionServiceHttpAdapter.HEADER_AUTHORIZATION to "Bearer $accessToken"
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
        internal const val PAYMENT_FIELD_VISA = "vis"
    }
}

sealed class SavedCardResponse {
    data class Success(val paymentResponse: PaymentResponse) : SavedCardResponse()

    data class Error(val error: Exception) : SavedCardResponse()
}