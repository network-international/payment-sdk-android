package payment.sdk.android.core.interactor

import androidx.annotation.Keep
import com.google.gson.Gson
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import java.net.URI

class GetPayerIpInteractor(
    private val httpClient: HttpClient,
) {
    suspend fun getPayerIp(payPageUrl: String): String? {
        val url = with(URI(payPageUrl)) {
            "https://$host/api/requester-ip"
        }
        return when (val response = httpClient.get(url, emptyMap(), Body.Empty())) {
            is SDKHttpResponse.Failed -> null
            is SDKHttpResponse.Success -> {
                Gson().fromJson(response.body, PayerIpResponse::class.java)?.requesterIp
            }
        }
    }

    @Keep
    internal data class PayerIpResponse(
        val requesterIp: String?
    )
}