package payment.sdk.android.core.interactor

import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class GooglePayAcceptInteractor(private val httpClient: HttpClient) {

    suspend fun accept(url: String, accessToken: String, token: String): SDKHttpResponse {
        return httpClient.post(
            url, headers = mapOf(
                TransactionServiceHttpAdapter.HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                TransactionServiceHttpAdapter.HEADER_AUTHORIZATION to "Bearer $accessToken"
            ), body = Body.StringBody(token)
        )
    }
}