package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.GooglePayConfigResponse
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class GooglePayConfigInteractor(private val httpClient: HttpClient) {

    suspend fun getConfig(url: String, accessToken: String): GooglePayConfigResponse? {
        val response = httpClient.get(
            url, headers = mapOf(
                TransactionServiceHttpAdapter.HEADER_AUTHORIZATION to "Bearer $accessToken"
            ), body = Body.Empty()
        )
        return when (response) {
            is SDKHttpResponse.Failed -> null
            is SDKHttpResponse.Success -> Gson().fromJson(
                response.body,
                GooglePayConfigResponse::class.java
            )
        }
    }
}