package payment.sdk.android.demo.http

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import java.util.concurrent.TimeUnit

class OkHttpClientAdapter(context: Context) : HttpClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            ChuckerInterceptor.Builder(context)
                .collector(
                    ChuckerCollector(
                        context = context,
                        showNotification = true,
                        retentionPeriod = RetentionManager.Period.ONE_HOUR
                    )
                )
                .maxContentLength(250_000L)
                .alwaysReadResponseBody(true)
                .build()
        )
        .build()

    override fun get(
        url: String,
        headers: Map<String, String>,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit,
        error: (Exception) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    executeRequest("GET", url, headers, Body.Empty())
                }
                success(result)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    override suspend fun get(
        url: String,
        headers: Map<String, String>,
        body: Body
    ): SDKHttpResponse {
        return execute("GET", url, headers, body)
    }

    override fun post(
        url: String,
        headers: Map<String, String>,
        body: Body,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit,
        error: (Exception) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    executeRequest("POST", url, headers, body)
                }
                success(result)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    override suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: Body
    ): SDKHttpResponse {
        return execute("POST", url, headers, body)
    }

    override fun put(
        url: String,
        headers: Map<String, String>,
        body: Body,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit,
        error: (Exception) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    executeRequest("PUT", url, headers, body)
                }
                success(result)
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    override suspend fun put(
        url: String,
        headers: Map<String, String>,
        body: Body
    ): SDKHttpResponse {
        return execute("PUT", url, headers, body)
    }

    override suspend fun delete(
        url: String,
        headers: Map<String, String>
    ): SDKHttpResponse {
        return execute("DELETE", url, headers, Body.Empty())
    }

    private suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: Body
    ): SDKHttpResponse {
        return try {
            val result = withContext(Dispatchers.IO) {
                executeRequest(method, url, headers, body)
            }
            SDKHttpResponse.Success(result.first, result.second.toString())
        } catch (e: Exception) {
            SDKHttpResponse.Failed(e)
        }
    }

    private fun executeRequest(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: Body
    ): Pair<Map<String, List<String>>, JSONObject> {
        val requestBody = if (body.isNotEmpty()) {
            val contentType = headers["Content-Type"] ?: "application/json"
            body.encode().toRequestBody(contentType.toMediaTypeOrNull())
        } else {
            null
        }

        val request = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
            }
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: "{}"
        val responseHeaders = response.headers.toMultimap()

        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP: ${response.code} - $responseBody")
        }

        return Pair(responseHeaders, JSONObject(responseBody))
    }
}
