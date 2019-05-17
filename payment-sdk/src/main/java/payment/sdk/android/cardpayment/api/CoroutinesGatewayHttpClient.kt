package payment.sdk.android.cardpayment.api

import android.support.annotation.UiThread
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.lang.IllegalStateException
import java.net.HttpURLConnection

import java.net.URL

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

internal class CoroutinesGatewayHttpClient : HttpClient {

    private val sslSocketFactoryDelegate: SSLSocketFactory by lazy {
        TLSSocketFactoryDelegate()
    }

    @UiThread
    override fun get(url: String, headers: Map<String, String>,
                     success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit) {
        httpMethodCall("GET", url, headers, Body.Empty(), success, error, doOutput = false)
    }

    @UiThread
    override fun post(url: String, headers: Map<String, String>, body: Body,
                      success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit) {
        httpMethodCall("POST", url, headers, body, success, error)
    }

    @UiThread
    override fun put(url: String, headers: Map<String, String>, body: Body,
                     success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit) {
        httpMethodCall("PUT", url, headers, body, success, error)
    }

    private fun httpMethodCall(method: String, url: String, headers: Map<String, String>, body: Body,
                               success: (Pair<Map<String, List<String>>, JSONObject>) -> Unit, error: (Exception) -> Unit, doOutput: Boolean = true) =
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


    private fun call(method: String, url: String, headers: Map<String, String>, body: Body, doOutput: Boolean)
            : Pair<Map<String, List<String>>, JSONObject> {
        var connection: HttpURLConnection? = null
        var writer: BufferedWriter? = null
        var reader: BufferedReader? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                initConnection(this, headers, doOutput).requestMethod = method
            }

            // Write content body
            if (doOutput && body.isNotEmpty()) {
                writer = connection.outputStream.bufferedWriter()
                writer.write(body.encode())
                writer.flush()
            }

            val responseCode = connection.responseCode

            when (responseCode) {
                // Success
                HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> {
                    reader = connection.inputStream.bufferedReader()
                    return Pair(connection.headerFields, JSONObject(reader.readText()))
                }
                // Not Discerned
                -1 -> throw IllegalStateException("Http response code can't be discerned: -1")
                // Other HTTP Codes
                else -> throw IllegalStateException("HTTP: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        } finally {
            writer?.close()
            reader?.close()
            connection?.disconnect()
        }
    }

    private fun initConnection(connection: HttpURLConnection, headers: Map<String, String>, isDoOutput: Boolean) =
            connection.apply {
                readTimeout = 30000
                connectTimeout = 30000
                doOutput = isDoOutput
                useCaches = false
                setRequestProperty("Accept-Language", "UTF-8")
                setRequestProperty("User-Agent", "Android Pay Page")

                headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }

                if (this is HttpsURLConnection) {
                    sslSocketFactory = sslSocketFactoryDelegate
                }
            }

}
