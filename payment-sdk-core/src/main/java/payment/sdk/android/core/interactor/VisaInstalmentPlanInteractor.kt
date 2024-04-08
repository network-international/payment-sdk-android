package payment.sdk.android.core.interactor

import android.util.Log
import com.google.gson.Gson
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class VisaInstalmentPlanInteractor(
    private val httpClient: HttpClient
) {
    suspend fun getPlans(
        selfUrl: String,
        cardToken: String,
        token: String
    ): VisaPlans? {
        val response = httpClient.post(
            url = "$selfUrl/vis/eligibility-check",
            headers = mapOf(
                HEADER_COOKIE to token,
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json"
            ),
            body = Body.Json(
                mapOf("cardToken" to cardToken)
            )
        )
        return when(response) {
            is SDKHttpResponse.Failed -> {
                Log.i("VisaInstalmentPlan", "error")
                return null
            }
            is SDKHttpResponse.Success -> {
                return Gson().fromJson(response.body, VisaPlans::class.java)
            }
        }
    }

    companion object {
        const val HEADER_COOKIE = "Cookie"
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}