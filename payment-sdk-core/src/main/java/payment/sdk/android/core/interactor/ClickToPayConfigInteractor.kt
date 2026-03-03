package payment.sdk.android.core.interactor

import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

/**
 * Interactor to fetch Click to Pay configuration from the gateway.
 * This retrieves the DPA configuration needed to initialize the Visa SDK.
 */
@Keep
class ClickToPayConfigInteractor(
    private val httpClient: HttpClient
) {
    /**
     * Fetch Click to Pay configuration from the gateway
     *
     * @param configUrl The URL to fetch CTP config from
     * @param accessToken The access token for authentication
     * @return The CTP configuration or null if not available
     */
    suspend fun getConfig(
        configUrl: String,
        accessToken: String,
        paymentCookie: String? = null
    ): ClickToPayGatewayConfig? = withContext(Dispatchers.IO) {
        try {
            val headers = mutableMapOf(
                "Accept" to "application/json",
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $accessToken",
                "Access-Token" to accessToken
            )
            if (!paymentCookie.isNullOrEmpty()) {
                headers["Cookie"] = paymentCookie
                ClickToPayApiInteractor.extractPaymentToken(paymentCookie)?.let {
                    headers["Payment-Token"] = it
                }
            }

            val response = httpClient.get(
                url = configUrl,
                headers = headers,
                body = Body.Empty()
            )

            when (response) {
                is SDKHttpResponse.Success -> {
                    val json = JSONObject(response.body)
                    ClickToPayGatewayConfig(
                        dpaId = json.optString("dpaId"),
                        dpaName = json.optString("dpaName"),
                        dpaPresentationName = json.optString("dpaPresentationName"),
                        dpaClientId = json.optString("dpaClientId").takeIf { it.isNotEmpty() },
                        cardBrands = parseCardBrands(json.optJSONArray("cardBrands")),
                        sdkUrl = json.optString("sdkUrl"),
                        isSandbox = json.optBoolean("sandbox", true),
                        locale = json.optString("locale", "en_US"),
                        kid = json.optString("kid").takeIf { it.isNotEmpty() },
                        publicKey = json.optString("publicKey").takeIf { it.isNotEmpty() },
                        merchantConfig = parseMerchantConfig(json.optJSONObject("merchantConfig"))
                    )
                }
                is SDKHttpResponse.Failed -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCardBrands(jsonArray: org.json.JSONArray?): List<String> {
        if (jsonArray == null) return listOf("visa", "mastercard")
        return (0 until jsonArray.length()).map { jsonArray.getString(it) }
    }

    private fun parseMerchantConfig(json: JSONObject?): MerchantConfig? {
        if (json == null) return null
        return MerchantConfig(
            acquirerMerchantId = json.optString("acquirerMerchantId").takeIf { it.isNotEmpty() },
            merchantCountryCode = json.optString("merchantCountryCode").takeIf { it.isNotEmpty() },
            acquirerBins = parseAcquirerBins(json.optJSONObject("acquirerBins")),
            mcc = json.optString("mcc").takeIf { it.isNotEmpty() }
        )
    }

    private fun parseAcquirerBins(json: JSONObject?): Map<String, AcquirerBin>? {
        if (json == null) return null
        val result = mutableMapOf<String, AcquirerBin>()
        json.keys().forEach { key ->
            val binObj = json.optJSONObject(key)
            if (binObj != null) {
                result[key] = AcquirerBin(
                    acquirerBin = binObj.optString("acquirerBin").takeIf { it.isNotEmpty() }
                )
            }
        }
        return result.takeIf { it.isNotEmpty() }
    }
}

/**
 * Click to Pay configuration received from the gateway
 */
@Keep
data class ClickToPayGatewayConfig(
    val dpaId: String,
    val dpaName: String,
    val dpaPresentationName: String?,
    val dpaClientId: String?,
    val cardBrands: List<String>,
    val sdkUrl: String,
    val isSandbox: Boolean,
    val locale: String,
    val kid: String? = null,
    val publicKey: String? = null,
    val merchantConfig: MerchantConfig? = null
)

@Keep
data class MerchantConfig(
    val acquirerMerchantId: String?,
    val merchantCountryCode: String?,
    val acquirerBins: Map<String, AcquirerBin>?,
    val mcc: String?
) {
    fun toJsonString(): String {
        val json = JSONObject()
        acquirerMerchantId?.let { json.put("acquirerMerchantId", it) }
        merchantCountryCode?.let { json.put("merchantCountryCode", it) }
        mcc?.let { json.put("mcc", it) }
        acquirerBins?.let { bins ->
            val binsJson = JSONObject()
            bins.forEach { (key, value) ->
                val binJson = JSONObject()
                value.acquirerBin?.let { binJson.put("acquirerBin", it) }
                binsJson.put(key, binJson)
            }
            json.put("acquirerBins", binsJson)
        }
        return json.toString()
    }
}

@Keep
data class AcquirerBin(
    val acquirerBin: String?
)
