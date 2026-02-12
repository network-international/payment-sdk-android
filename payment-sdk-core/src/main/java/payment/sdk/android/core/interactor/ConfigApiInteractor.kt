package payment.sdk.android.core.interactor

import androidx.annotation.Keep
import com.google.gson.Gson
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import java.net.URI

class ConfigApiInteractor(private val httpClient: HttpClient) {
    suspend fun getTncUrl(orderUrl: String, accessToken: String, outletRef: String): String? {
        val url = with(URI(orderUrl)) {
            "https://$host/config/outlets/$outletRef/configs/invoice"
        }
        val headers = mapOf(TransactionServiceHttpAdapter.HEADER_AUTHORIZATION to "Bearer $accessToken")
        return when (val response = httpClient.get(url, headers, Body.Empty())) {
            is SDKHttpResponse.Failed -> null
            is SDKHttpResponse.Success -> {
                Gson().fromJson(response.body, InvoiceResponse::class.java)?.tncUrl
            }
        }
    }

    @Keep
    internal data class InvoiceResponse(
        val tncUrl: String?
    )
}