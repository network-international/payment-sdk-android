package payment.sdk.android.core.interactor

import androidx.annotation.Keep

@Keep
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
