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
        cvv: String?
    ): SavedCardResponse {
        val bodyMap = mutableMapOf(
            KEY_EXPIRY to savedCard.expiry,
            KEY_CARD_TOKEN to savedCard.cardToken,
            KEY_CARDHOLDER_NAME to savedCard.cardholderName
        )
        cvv?.let {
            bodyMap.put(KEY_CVV, it)
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
    }
}

sealed class SavedCardResponse {
    data class Success(val paymentResponse: PaymentResponse) : SavedCardResponse()

    data class Error(val error: Exception) : SavedCardResponse()
}