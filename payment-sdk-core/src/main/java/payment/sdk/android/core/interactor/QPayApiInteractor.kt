package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.QPayInitResponse
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

/**
 * Calls the backend QPay endpoint (e.g. `/orders/{id}/payments/{id}/qpay`) which returns the
 * form fields needed to redirect the user to the QCB gateway.
 */
class QPayApiInteractor(private val httpClient: HttpClient) {

    suspend fun initQPay(url: String, accessToken: String): QPayApiResponse {
        val response = httpClient.post(
            url,
            mapOf(
                "Content-Type" to "application/vnd.ni-payment.v2+json",
                "Authorization" to "Bearer $accessToken"
            ),
            // Sends `{}` (Body.Json always serialises). The backend rejects a missing body with
            // "initiatePaymentRequest is null"; the gateway derives payerIp from the request,
            // matching the web which also posts `{}`.
            Body.Json(emptyMap())
        )
        return when (response) {
            is SDKHttpResponse.Failed -> QPayApiResponse.Error(response.error)
            is SDKHttpResponse.Success -> {
                runCatching {
                    Gson().fromJson(response.body, QPayInitResponse::class.java)
                }.fold(
                    onSuccess = { QPayApiResponse.Success(it) },
                    onFailure = { QPayApiResponse.Error(Exception("Failed to decode QPay init response", it)) }
                )
            }
        }
    }
}

sealed class QPayApiResponse {
    data class Success(val response: QPayInitResponse) : QPayApiResponse()
    data class Error(val error: Exception) : QPayApiResponse()
}
