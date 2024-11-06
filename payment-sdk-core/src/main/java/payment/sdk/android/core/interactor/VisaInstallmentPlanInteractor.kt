package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class VisaInstallmentPlanInteractor(
    private val httpClient: HttpClient
) {
    suspend fun getPlans(
        selfUrl: String,
        cardToken: String? = null,
        cardNumber: String? = null,
        token: String
    ): VisaPlansResponse {
        val bodyMap = mutableMapOf<String, String>()
        cardToken?.let {
            bodyMap.put("cardToken", cardToken)
        }

        cardNumber?.let {
            bodyMap.put("pan", cardNumber)
        }

        val response = httpClient.post(
            url = "$selfUrl/vis/eligibility-check",
            headers = mapOf(
                HEADER_COOKIE to token,
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json"
            ),
            body = Body.Json(bodyMap)
        )
        return when (response) {
            is SDKHttpResponse.Failed -> VisaPlansResponse.Error(response.error)
            is SDKHttpResponse.Success -> {
                val visaPlans = Gson().fromJson(response.body, VisaPlans::class.java)
                return VisaPlansResponse.Success(visaPlans)
            }
        }
    }

    companion object {
        const val HEADER_COOKIE = "Cookie"
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}

sealed class VisaPlansResponse {
    data class Success(val visaPlans: VisaPlans) : VisaPlansResponse()
    data class Error(val error: Exception) : VisaPlansResponse()
}