package payment.sdk.android.core.interactor

import android.net.Uri
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import payment.sdk.android.core.json
import payment.sdk.android.core.string

class AuthRepository(
    private val httpClient: HttpClient,
) {

    suspend fun run(
        authUrl: String,
        paymentUrl: String
    ): AuthResponse {
        val authCode = Uri.parse(paymentUrl).getQueryParameter("code")
            ?: return AuthResponse.Error(IllegalArgumentException("Auth code not found"))
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
                val cookies = response.headers[TransactionServiceHttpAdapter.HEADER_SET_COOKIE]
                val orderUrl = response.body.json("_links")?.json("cnp:order")
                    ?.string("href")
                if (cookies != null && orderUrl != null) {
                    AuthResponse.Success(cookies = cookies, orderUrl = orderUrl)
                } else {
                    AuthResponse.Error(IllegalArgumentException("Auth cookies not found in response"))
                }
            }
        }
    }
}

sealed class AuthResponse {
    data class Success(val cookies: List<String>, val orderUrl: String) : AuthResponse() {

        fun getPaymentCookie() = cookies.first {
            it.startsWith(PAYMENT_TOKEN)
        }

        fun getAccessToken(): String {
            return cookies.first {
                it.startsWith(ACCESS_TOKEN)
            }.split(";")
                .toTypedArray()
                .first()
                .split("=")
                .toTypedArray()[1]
        }
    }

    data class Error(val error: Exception) : AuthResponse()

    companion object {
        const val PAYMENT_TOKEN = "payment-token"
        const val ACCESS_TOKEN = "access-token"
    }
}