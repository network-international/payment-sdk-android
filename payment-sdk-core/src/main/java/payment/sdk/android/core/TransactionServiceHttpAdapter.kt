package payment.sdk.android.core

import android.net.Uri
import com.google.gson.Gson
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.api.HttpClient
import java.util.*
import kotlin.collections.HashMap

class TransactionServiceHttpAdapter : TransactionService {
    val httpClient: HttpClient = CoroutinesGatewayHttpClient()

    override fun authorizePayment(order: Order, onResponse: (authTokens: HashMap<String, String>?, error: Exception?) -> Unit) {
        val authUrl = order?.links?.paymentAuthorizationUrl?.href
        val paymentUrl = order?.links?.paymentUrl?.href

        if (paymentUrl == null) {
            onResponse(null, Exception("No authcode found"))
            return
        }

        if (authUrl == null) {
            onResponse(null, Exception("No payment authorization url found"))
            return
        }

        val authCode = Uri.parse(paymentUrl).getQueryParameter("code")

        if (authCode == null) {
            onResponse(null, Exception("No auth code found"))
            return
        }

        httpClient.post(
                url = authUrl,
                headers = mapOf(
                        HEADER_CONTENT_TYPE to "application/x-www-form-urlencoded",
                        HEADER_ACCEPT to "application/vnd.ni-payment.v2+json"
                ),
                body = Body.Form(mapOf(
                        "code" to authCode
                )),
                success = { (headers) ->
                    val cookies = headers[HEADER_SET_COOKIE]
                    if (cookies == null) {
                        onResponse(null, Exception("No auth cookie found"))
                    } else {
                        val setCookieHeaders: List<String> = ArrayList<String>(cookies)
                        val authTokens = HashMap<String, String>()
                        for (header in setCookieHeaders) {
                            val tokenComponents = header.split(";").toTypedArray() // Split and remove other attributes
                            val token = tokenComponents[0].split("=").toTypedArray() // Split the name and token separately
                            authTokens[token[0]] = token[1]
                        }
                        onResponse(authTokens, null)
                    }
                },
                error = { exception ->
                    onResponse(null, exception)
                })
    }

    override fun acceptSamsungPay(encryptedObject: String,
                                  samsungPayLink: String,
                                  paymentToken: String,
                                  onResponse: (status: Boolean, error: Exception?) -> Unit) {
        val body = HashMap<String, String>()
        body["encryptedObj"] = encryptedObject

        httpClient.put(
                url = samsungPayLink,
                headers = mapOf(
                        HEADER_CONTENT_TYPE to "application/vnd.ni-payment.v2+json",
                        HEADER_ACCEPT to "application/vnd.ni-payment.v2+json",
                        HEADER_AUTHORIZATION to "Bearer $paymentToken"
                ),

                body = Body.JsonStr(body),
                success = { _ ->
                    onResponse(true, null)
                },
                error = { exception ->
                    onResponse(false, exception)
                })
    }

    companion object {
        internal const val HEADER_ACCEPT = "Accept"
        internal const val HEADER_CONTENT_TYPE = "Content-Type"
        internal const val HEADER_SET_COOKIE = "Set-Cookie"
        internal const val HEADER_AUTHORIZATION = "Authorization"
    }

}