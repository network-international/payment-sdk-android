package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.KnetInitResponse
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

/**
 * Starts a KNET (Kuwait debit) payment by POSTing `{}` to the payment's `payment:knet` link. The
 * gateway answers with the hosted page on KNET's site the payer must be sent to.
 */
class KnetApiInteractor(private val httpClient: HttpClient) {

    suspend fun initKnet(url: String, accessToken: String): KnetApiResponse {
        val response = httpClient.post(
            url,
            mapOf(
                "Content-Type" to "application/vnd.ni-payment.v2+json",
                "Accept" to "application/vnd.ni-payment.v2+json",
                "Authorization" to "Bearer $accessToken"
            ),
            Body.Json(emptyMap())
        )
        return when (response) {
            is SDKHttpResponse.Failed -> KnetApiResponse.Error(response.error)
            is SDKHttpResponse.Success -> runCatching {
                Gson().fromJson(response.body, KnetInitResponse::class.java)
            }.fold(
                onSuccess = { KnetApiResponse.Success(it) },
                onFailure = {
                    KnetApiResponse.Error(Exception("Failed to decode KNET init response", it))
                }
            )
        }
    }
}

sealed class KnetApiResponse {
    data class Success(val response: KnetInitResponse) : KnetApiResponse()
    data class Error(val error: Exception) : KnetApiResponse()
}
