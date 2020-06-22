package payment.sdk.android.core.api

import org.json.JSONObject

interface HttpClient {

    fun get(url: String, headers: Map<String, String>,
            success: (Pair<Map<String,List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit)

    fun post(url: String, headers: Map<String, String>, body: Body,
             success: (Pair<Map<String,List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit)

    fun put(url: String, headers: Map<String, String>, body: Body,
            success: (Pair<Map<String,List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit)
}
