package payment.sdk.android.core.interactor

import com.google.gson.JsonParser
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class AaniPoolingApiInteractor(
    private val httpClient: HttpClient
) {

    suspend fun startPooling(url: String, accessToken: String): String {
        val response = httpClient.get(
            url, mapOf(
                "Content-Type" to "application/vnd.ni-payment.v2+json",
                "Authorization" to "Bearer $accessToken"
            ), Body.Empty()
        )
        return when (response) {
            is SDKHttpResponse.Failed -> ""

            is SDKHttpResponse.Success -> {
                JsonParser.parseString(response.body).asJsonObject.get("state").asString.orEmpty()
            }
        }
    }
}