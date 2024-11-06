package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.AaniPayResponse
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class AaniPayApiInterator(private val httpClient: HttpClient) {

    suspend fun makePayment(url: String, accessToken: String, request: Body): AaniPayApiResponse {
        val b = request.encode()
        println(b)
        val response = httpClient.post(
            url, mapOf(
                "Content-Type" to "application/vnd.ni-payment.v2+json",
                "Authorization" to "Bearer $accessToken"
            ), request
        )
        return when (response) {
            is SDKHttpResponse.Failed -> AaniPayApiResponse.Error(response.error)
            is SDKHttpResponse.Success -> AaniPayApiResponse.Success(
                Gson().fromJson(
                    response.body,
                    AaniPayResponse::class.java
                )
            )
        }
    }
}

sealed class AaniPayApiResponse {
    data class Success(val aaniPayResponse: AaniPayResponse) : AaniPayApiResponse()
    data class Error(val error: Exception) : AaniPayApiResponse()
}