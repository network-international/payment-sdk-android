package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.BnplInitResponse
import payment.sdk.android.core.BnplProvider
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

/**
 * Starts and finalises a buy-now-pay-later checkout (Tamara or Tabby — one endpoint shape, one
 * body). The gateway answers initiation with the provider's hosted checkout URL and its own
 * reference; the accept call hands that reference back so the payment can be finalised.
 */
class BnplApiInteractor(private val httpClient: HttpClient) {

    suspend fun initBnpl(
        url: String,
        accessToken: String,
        successUrl: String,
        cancelUrl: String,
        failureUrl: String
    ): BnplApiResponse {
        val response = httpClient.post(
            url,
            headers(accessToken),
            Body.Json(
                mapOf(
                    "type" to BnplProvider.CHECKOUT_TYPE,
                    "successUrl" to successUrl,
                    "cancelUrl" to cancelUrl,
                    "failureUrl" to failureUrl
                )
            )
        )
        return when (response) {
            is SDKHttpResponse.Failed -> BnplApiResponse.Error(response.error)
            is SDKHttpResponse.Success -> runCatching {
                Gson().fromJson(response.body, BnplInitResponse::class.java)
            }.fold(
                onSuccess = { BnplApiResponse.Success(it) },
                onFailure = {
                    BnplApiResponse.Error(Exception("Failed to decode the checkout response", it))
                }
            )
        }
    }

    /**
     * Hands the provider's outcome to the backend. The field name differs per provider, so it
     * travels with the value rather than being hard-coded here.
     */
    suspend fun acceptBnpl(
        url: String,
        accessToken: String,
        idField: String,
        idValue: String
    ): Boolean {
        val response = httpClient.post(url, headers(accessToken), Body.Json(mapOf(idField to idValue)))
        return response is SDKHttpResponse.Success
    }

    private fun headers(accessToken: String) = mapOf(
        "Content-Type" to "application/vnd.ni-payment.v2+json",
        "Accept" to "application/vnd.ni-payment.v2+json",
        "Authorization" to "Bearer $accessToken"
    )
}

sealed class BnplApiResponse {
    data class Success(val response: BnplInitResponse) : BnplApiResponse()
    data class Error(val error: Exception) : BnplApiResponse()
}
