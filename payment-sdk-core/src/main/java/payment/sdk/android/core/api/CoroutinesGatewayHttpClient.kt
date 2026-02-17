package payment.sdk.android.core.api

import android.os.Build
import androidx.annotation.UiThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class CoroutinesGatewayHttpClient : HttpClient {

    private val sslSocketFactoryDelegate: SSLSocketFactory by lazy {
        TLSSocketFactoryDelegate()
    }

    private val sdkVersion = "5.0.0"

    @UiThread
    override fun get(
        url: String, headers: Map<String, String>,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit
    ) {
        httpMethodCall("GET", url, headers, Body.Empty(), success, error, doOutput = false)
    }

    override suspend fun get(
        url: String,
        headers: Map<String, String>,
        body: Body
    ): SDKHttpResponse {
        return try {
            val response = call("GET", url, headers, body, doOutput = false)
            SDKHttpResponse.Success(response.first, response.second.toString())
        } catch (e: java.lang.Exception) {
            SDKHttpResponse.Failed(e)
        }
    }

    @UiThread
    override fun post(
        url: String, headers: Map<String, String>, body: Body,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit
    ) {
        httpMethodCall("POST", url, headers, body, success, error)
    }

    override suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: Body
    ): SDKHttpResponse {
        return try {
            val response = call(
                method = "POST",
                url = url,
                headers = headers,
                body = body,
                doOutput = true
            )
            SDKHttpResponse.Success(response.first, response.second.toString())
        } catch (e: java.lang.Exception) {
            SDKHttpResponse.Failed(e)
        }
    }

    @UiThread
    override fun put(
        url: String, headers: Map<String, String>, body: Body,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit
    ) {
        httpMethodCall("PUT", url, headers, body, success, error)
    }

    override suspend fun put(
        url: String,
        headers: Map<String, String>,
        body: Body
    ): SDKHttpResponse {
        return try {
            val response =
                call(
                    "PUT",
                    url,
                    headers,
                    body,
                    doOutput = true
                )
            SDKHttpResponse.Success(response.first, response.second.toString())
        } catch (e: java.lang.Exception) {
            SDKHttpResponse.Failed(e)
        }
    }

    private fun httpMethodCall(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: Body,
        success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit,
        error: (Exception) -> Unit,
        doOutput: Boolean = true
    ) =
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val data = withContext(Dispatchers.IO) {
                    call(method, url, headers, body, doOutput)
                }
                success(data)
            } catch (e: java.lang.Exception) {
                error(e)
            }
        }

    private suspend fun call(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: Body,
        doOutput: Boolean
    )
            : Pair<Map<String, List<String>>, JSONObject> {
        var connection: HttpURLConnection? = null
        var writer: BufferedWriter? = null
        var reader: BufferedReader? = null

        val bodyStr = if (body.isNotEmpty()) body.encode() else null
        logCurl(method, url, headers, bodyStr)

        try {
            connection = (withContext(Dispatchers.IO) {
                URL(url).openConnection()
            } as HttpURLConnection).apply {
                initConnection(this, headers, doOutput).requestMethod = method
            }

            // Write content body
            if (doOutput && body.isNotEmpty()) {
                writer = connection.outputStream.bufferedWriter()
                withContext(Dispatchers.IO) {
                    writer.write(bodyStr)
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode

            when (responseCode) {
                // Success
                HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> {
                    reader = connection.inputStream.bufferedReader()
                    val responseBody = reader.readText()
                    logResponse(responseCode, url, responseBody)
                    return Pair(connection.headerFields, JSONObject(responseBody))
                }
                // Not Discerned
                -1 -> throw IllegalStateException("Http response code can't be discerned: -1")
                // Other HTTP Codes
                else -> {
                    val errorBody = try {
                        connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                    } catch (e: Exception) {
                        "Failed to read error body: ${e.message}"
                    }
                    logResponse(responseCode, url, errorBody)
                    throw IllegalStateException("HTTP: $responseCode - $errorBody")
                }
            }
        } catch (e: Exception) {
            throw e
        } finally {
            withContext(Dispatchers.IO) {
                writer?.close()
                reader?.close()
            }
            connection?.disconnect()
        }
    }

    private fun logCurl(method: String, url: String, headers: Map<String, String>, body: String?) {
        val parts = StringBuilder("curl -X $method")
        headers.toSortedMap().forEach { (key, value) ->
            parts.append(" \\\n  -H '${key}: ${value.replace("'", "'\\''")}'")
        }
        if (body != null) {
            parts.append(" \\\n  -d '${body.replace("'", "'\\''")}'")
        }
        parts.append(" \\\n  '$url'")
        Log.d(TAG, "🔗 cURL:\n$parts")
    }

    private fun logResponse(code: Int, url: String, body: String) {
        val prettyBody = try {
            JSONObject(body).toString(2)
        } catch (e: Exception) {
            body
        }
        Log.d(TAG, "⬅️ RESPONSE: $code $url\n$prettyBody")
    }

    companion object {
        private const val TAG = "NI-SDK-HTTP"
    }

    private fun initConnection(
        connection: HttpURLConnection,
        headers: Map<String, String>,
        isDoOutput: Boolean
    ) =
        connection.apply {
            readTimeout = 30000
            connectTimeout = 30000
            doOutput = isDoOutput
            useCaches = false
            val device = "${Build.MANUFACTURER}-${Build.MODEL}"
            val os = "OS-${Build.VERSION.SDK_INT}"
            setRequestProperty("Accept-Language", "UTF-8")
            setRequestProperty("User-Agent", "Android Pay Page $device $os SDK:$sdkVersion")

            headers.forEach { (key, value) ->
                setRequestProperty(key, value)
            }

            if (this is HttpsURLConnection) {
                sslSocketFactory = sslSocketFactoryDelegate
            }
        }

}
