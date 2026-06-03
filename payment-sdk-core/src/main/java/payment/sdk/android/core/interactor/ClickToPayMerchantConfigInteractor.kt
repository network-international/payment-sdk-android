package payment.sdk.android.core.interactor

import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

/**
 * Calls `GET /config/merchants/{merchantId}/configs/vctp` on the N-Genius gateway and
 * decodes the DPA credentials needed to launch Click to Pay. Used by the SDK to populate
 * [ClickToPayConfig.dpaId] / [ClickToPayConfig.dpaClientId] / [ClickToPayConfig.dpaName]
 * so the merchant never has to hardcode them.
 */
@Keep
class ClickToPayMerchantConfigInteractor(
    private val httpClient: HttpClient
) {
    suspend fun fetch(
        merchantId: String,
        accessToken: String,
        apiGatewayBaseUrl: String
    ): Result = withContext(Dispatchers.IO) {
        if (merchantId.isBlank()) {
            return@withContext Result.Failure(IllegalArgumentException("merchantId is blank"))
        }
        val base = apiGatewayBaseUrl.trimEnd('/')
        val url = "$base/config/merchants/$merchantId/configs/vctp"
        try {
            val response = httpClient.get(
                url = url,
                headers = mapOf(
                    "Accept" to "application/json",
                    "Authorization" to "Bearer $accessToken"
                ),
                body = Body.Empty()
            )
            when (response) {
                is SDKHttpResponse.Success -> {
                    val json = JSONObject(response.body)
                    val dpaId = json.optString("dpaId").takeIf { it.isNotEmpty() }
                        ?: return@withContext Result.Failure(IllegalStateException("Response missing dpaId"))
                    val dpaClientId = json.optString("dpaClientId").takeIf { it.isNotEmpty() }
                    val dpaName = json.optJSONObject("successResponse")
                        ?.optString("companyPrimaryLegalName")
                        ?.takeIf { it.isNotEmpty() }
                    Result.Success(
                        Resolved(dpaId = dpaId, dpaClientId = dpaClientId, dpaName = dpaName)
                    )
                }
                is SDKHttpResponse.Failed -> Result.Failure(response.error)
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    @Keep
    data class Resolved(
        val dpaId: String,
        val dpaClientId: String?,
        val dpaName: String?
    )

    sealed class Result {
        data class Success(val resolved: Resolved) : Result()
        data class Failure(val error: Throwable) : Result()
    }
}
