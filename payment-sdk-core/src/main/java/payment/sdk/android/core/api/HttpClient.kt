package payment.sdk.android.core.api

import org.json.JSONObject

interface HttpClient {

    fun get(
        url: String, headers: Map<String, String>,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit
    )

    fun post(
        url: String, headers: Map<String, String>, body: Body,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit
    )

    fun put(
        url: String, headers: Map<String, String>, body: Body,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit
    )

    suspend fun get(url: String, headers: Map<String, String>, body: Body): SDKHttpResponse

    suspend fun put(url: String, headers: Map<String, String>, body: Body): SDKHttpResponse

    suspend fun post(url: String, headers: Map<String, String>, body: Body): SDKHttpResponse
}


sealed class SDKHttpResponse {
    data class Success(val headers: Map<String, List<String>>, val body: String) :
        SDKHttpResponse()

    data class Failed(val error: Exception) : SDKHttpResponse()
}