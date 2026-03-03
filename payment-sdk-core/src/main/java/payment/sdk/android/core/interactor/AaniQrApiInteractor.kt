package payment.sdk.android.core.interactor

import com.google.gson.Gson
import com.google.gson.JsonParser
import payment.sdk.android.core.AaniPayResponse
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class AaniQrApiInteractor(private val httpClient: HttpClient) {

    suspend fun createQr(url: String, accessToken: String): AaniQrCreateResponse {
        val response = httpClient.post(
            url, mapOf(
                "Content-Type" to "application/vnd.ni-payment.v2+json",
                "Authorization" to "Bearer $accessToken"
            ), Body.StringBody("{}")
        )
        return when (response) {
            is SDKHttpResponse.Failed -> AaniQrCreateResponse.Error(response.error)
            is SDKHttpResponse.Success -> AaniQrCreateResponse.Success(
                Gson().fromJson(
                    response.body,
                    AaniPayResponse::class.java
                )
            )
        }
    }

    suspend fun pollQrStatus(
        url: String,
        accessToken: String,
        qrCodeId: String,
        qrTransactionId: String
    ): String {
        val response = httpClient.get(
            "$url/status?qrCodeId=$qrCodeId&qrTransactionId=$qrTransactionId",
            mapOf(
                "Content-Type" to "application/vnd.ni-payment.v2+json",
                "Authorization" to "Bearer $accessToken"
            ),
            Body.Empty()
        )
        return when (response) {
            is SDKHttpResponse.Failed -> "FAILED"
            is SDKHttpResponse.Success -> {
                JsonParser.parseString(response.body).asJsonObject.get("state").asString.orEmpty()
            }
        }
    }

    suspend fun cancelQr(
        url: String,
        accessToken: String,
        qrCodeId: String,
        qrTransactionId: String
    ) {
        httpClient.delete(
            "$url?qrCodeId=$qrCodeId&qrTransactionId=$qrTransactionId",
            mapOf(
                "Content-Type" to "application/vnd.ni-payment.v2+json",
                "Authorization" to "Bearer $accessToken"
            )
        )
    }
}
