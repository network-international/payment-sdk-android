package payment.sdk.android.core.interactor

import com.google.gson.Gson
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class AuthApiInteractor(private val httpClient: HttpClient) {

    suspend fun authenticate(
        authUrl: String,
        authCode: String
    ): AuthResponse {

        val response = httpClient.post(
            url = authUrl,
            headers = mapOf(
                TransactionServiceHttpAdapter.HEADER_CONTENT_TYPE to "application/x-www-form-urlencoded",
                TransactionServiceHttpAdapter.HEADER_ACCEPT to "application/vnd.ni-payment.v2+json"
            ),
            body = Body.Form(
                mapOf(
                    "code" to authCode
                )
            )
        )

        return when (response) {
            is SDKHttpResponse.Failed -> AuthResponse.Error(error = response.error)
            is SDKHttpResponse.Success -> {
                response.headers[TransactionServiceHttpAdapter.HEADER_SET_COOKIE]?.let { cookies ->
                    val body = Gson().fromJson(response.body, AuthResponseBody::class.java)
                    body.links?.orderRef?.href?.let {
                        AuthResponse.Success(cookies = cookies, orderUrl = it)
                    } ?: run {
                        AuthResponse.Error(IllegalArgumentException(AUTH_ERROR_ORDER_URL))
                    }
                } ?: run {
                    AuthResponse.Error(IllegalArgumentException(AUTH_ERROR_COOKIE))
                }
            }
        }
    }

    companion object {
        internal const val AUTH_ERROR_COOKIE = "Auth cookies not found in response"
        internal const val AUTH_ERROR_ORDER_URL = "Order url not found in auth response"
    }
}