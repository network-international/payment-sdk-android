package payment.sdk.android.demo.http

import android.util.Log
import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.demo.model.toMap
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import payment.sdk.android.core.Order
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

private const val TAG = "ApiServiceAdapter"
private const val CURL_TAG = "CURL_COMMAND"

class ApiServiceAdapter(
    private val httpClient: HttpClient
) : ApiService {

    private val prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()

    override suspend fun getAccessToken(url: String, apiKey: String, realm: String): String? {
        val body = mapOf("realmName" to realm)
        val bodyJson = Gson().toJson(body)

        // Log cURL command
        val curl = buildString {
            append("curl -X POST '${url}' \\\n")
            append("  -H 'Content-Type: application/vnd.ni-identity.v1+json' \\\n")
            append("  -H 'Authorization: Basic ${apiKey}' \\\n")
            append("  -d '${bodyJson}'")
        }

        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "getAccessToken REQUEST")
        Log.d(CURL_TAG, "\n$curl")
        Log.d(TAG, "═══════════════════════════════════════════════════════════")

        val response = httpClient.post(
            url = url,
            headers = mapOf(
                HEADER_CONTENT_TYPE to "application/vnd.ni-identity.v1+json",
                HEADER_AUTH to "Basic $apiKey"
            ),
            body = Body.Json(body)
        )
        return when (response) {
            is SDKHttpResponse.Failed -> {
                Log.e(TAG, "───────────────────────────────────────────────────────────")
                Log.e(TAG, "getAccessToken FAILED")
                Log.e(TAG, "Error: ${response.error.message}", response.error)
                Log.e(TAG, "───────────────────────────────────────────────────────────")
                null
            }
            is SDKHttpResponse.Success -> {
                Log.d(TAG, "───────────────────────────────────────────────────────────")
                Log.d(TAG, "getAccessToken SUCCESS")
                Log.d(TAG, "Response: ${response.body}")
                Log.d(TAG, "───────────────────────────────────────────────────────────")
                JsonParser.parseString(response.body).asJsonObject.get("access_token")?.asString
            }
        }
    }

    override suspend fun getOrder(
        url: String,
        orderReference: String,
        accessToken: String
    ): Order? {
        val fullUrl = "$url/$orderReference"

        // Log cURL command
        val curl = buildString {
            append("curl -X GET '${fullUrl}' \\\n")
            append("  -H 'Content-Type: application/vnd.ni-payment.v2+json' \\\n")
            append("  -H 'Authorization: Bearer ${accessToken}'")
        }

        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "getOrder REQUEST")
        Log.d(CURL_TAG, "\n$curl")
        Log.d(TAG, "═══════════════════════════════════════════════════════════")

        val response = httpClient.get(
            url = fullUrl,
            headers = mapOf(
                HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                HEADER_AUTH to "Bearer $accessToken"
            ),
            body = Body.Empty()
        )
        return when (response) {
            is SDKHttpResponse.Failed -> {
                Log.e(TAG, "───────────────────────────────────────────────────────────")
                Log.e(TAG, "getOrder FAILED")
                Log.e(TAG, "Error: ${response.error.message}", response.error)
                Log.e(TAG, "───────────────────────────────────────────────────────────")
                null
            }
            is SDKHttpResponse.Success -> {
                Log.d(TAG, "───────────────────────────────────────────────────────────")
                Log.d(TAG, "getOrder SUCCESS")
                Log.d(TAG, "Response: ${response.body}")
                Log.d(TAG, "───────────────────────────────────────────────────────────")
                return Gson().fromJson(response.body, Order::class.java)
            }
        }
    }

    override suspend fun createOrder(
        url: String,
        accessToken: String,
        orderRequest: OrderRequest
    ): Order? {
        var createOrderUrl = url
        var contentType = "application/vnd.ni-payment.v2+json"
        if (orderRequest.type == "RECURRING" || orderRequest.type == "INSTALLMENT") {
            createOrderUrl = url.replace("transactions", "recurring-payment")
            contentType = "application/vnd.ni-recurring-payment.v2+json"
        }

        val bodyMap = orderRequest.toMap()
        val bodyJson = Gson().toJson(bodyMap)
        val bodyJsonPretty = prettyGson.toJson(bodyMap)

        // Log cURL command
        val curl = buildString {
            append("curl -X POST '${createOrderUrl}' \\\n")
            append("  -H 'Content-Type: ${contentType}' \\\n")
            append("  -H 'Authorization: Bearer ${accessToken}' \\\n")
            append("  -d '${bodyJson}'")
        }

        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "createOrder REQUEST")
        Log.d(CURL_TAG, "\n$curl")
        Log.d(TAG, "───────────────────────────────────────────────────────────")
        Log.d(TAG, "Request Body (Pretty):\n$bodyJsonPretty")
        Log.d(TAG, "═══════════════════════════════════════════════════════════")

        val response = httpClient.post(
            url = createOrderUrl,
            headers = mapOf(
                HEADER_CONTENT_TYPE to contentType,
                HEADER_ACCEPT to contentType,
                HEADER_AUTH to "Bearer $accessToken"
            ),
            body = Body.Json(bodyMap)
        )
        return when (response) {
            is SDKHttpResponse.Failed -> {
                Log.e(TAG, "───────────────────────────────────────────────────────────")
                Log.e(TAG, "createOrder FAILED")
                Log.e(TAG, "Error: ${response.error.message}", response.error)
                Log.e(TAG, "───────────────────────────────────────────────────────────")
                null
            }
            is SDKHttpResponse.Success -> {
                Log.d(TAG, "───────────────────────────────────────────────────────────")
                Log.d(TAG, "createOrder SUCCESS")
                Log.d(TAG, "Response: ${response.body}")
                Log.d(TAG, "───────────────────────────────────────────────────────────")
                return Gson().fromJson(response.body, Order::class.java)
            }
        }
    }

    companion object {
        const val HEADER_AUTH = "Authorization"
        const val HEADER_ACCEPT = "Content-Type"
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
