package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.BenefitInitResponse
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

/**
 * Starts a Benefit (Bahrain debit) payment. The gateway answers with the hosted page URL the payer
 * must be redirected to.
 */
class BenefitApiInteractor(private val httpClient: HttpClient) {

    suspend fun initBenefit(url: String, accessToken: String): BenefitApiResponse {
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
            is SDKHttpResponse.Failed -> BenefitApiResponse.Error(response.error)
            is SDKHttpResponse.Success -> runCatching {
                Gson().fromJson(response.body, BenefitInitResponse::class.java)
            }.fold(
                onSuccess = { BenefitApiResponse.Success(it) },
                onFailure = {
                    BenefitApiResponse.Error(Exception("Failed to decode Benefit init response", it))
                }
            )
        }
    }
}

sealed class BenefitApiResponse {
    data class Success(val response: BenefitInitResponse) : BenefitApiResponse()
    data class Error(val error: Exception) : BenefitApiResponse()
}
