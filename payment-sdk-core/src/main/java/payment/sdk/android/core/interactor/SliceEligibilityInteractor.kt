package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.SliceEligibilityResponse
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class SliceEligibilityInteractor(
    private val httpClient: HttpClient
) {
    suspend fun checkEligibility(
        eligibilityCheckUrl: String,
        token: String,
        pan: String,
        expiry: String,
        isSavedCardToken: Boolean = false
    ): SliceEligibilityResult {
        // Manual entry sends the digit-only PAN in `pan`; saved cards send the previously-issued
        // card token in `cardToken` (the `pan` field is validated as digits-only and rejects tokens).
        val identifierKey = if (isSavedCardToken) "cardToken" else "pan"
        val response = httpClient.post(
            url = eligibilityCheckUrl,
            headers = mapOf(
                HEADER_COOKIE to token,
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json"
            ),
            body = Body.Json(mapOf(identifierKey to pan, "expiry" to expiry))
        )
        return when (response) {
            is SDKHttpResponse.Failed -> SliceEligibilityResult.Error(response.error)
            is SDKHttpResponse.Success -> {
                val result = Gson().fromJson(response.body, SliceEligibilityResponse::class.java)
                SliceEligibilityResult.Success(result)
            }
        }
    }

    companion object {
        const val HEADER_COOKIE = "Cookie"
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}

sealed class SliceEligibilityResult {
    data class Success(val response: SliceEligibilityResponse) : SliceEligibilityResult()
    data class Error(val error: Exception) : SliceEligibilityResult()
}
