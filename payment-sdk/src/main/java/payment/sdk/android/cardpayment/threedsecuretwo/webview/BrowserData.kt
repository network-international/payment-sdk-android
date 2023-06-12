package payment.sdk.android.cardpayment.threedsecuretwo.webview

import androidx.annotation.Keep

@Keep
data class BrowserData(
    val browserLanguage: String,
    val browserJavaEnabled: Boolean,
    val browserColorDepth: String,
    val browserScreenHeight: String,
    val browserScreenWidth: String,
    val browserTZ: String,
    val browserUserAgent: String,
    var browserIP: String?
) {
    var browserAcceptHeader: String? = "application/json, text/plain, */*"
    var browserJavascriptEnabled: Boolean? = true
    var challengeWindowSize: String? = "05"

    fun getHashMap(): HashMap<String, Any> {
        val map = HashMap<String, Any>()
        map["browserLanguage"] = browserLanguage
        map["browserJavaEnabled"] = browserJavaEnabled
        map["browserColorDepth"] = browserColorDepth
        map["browserScreenHeight"] = browserScreenHeight
        map["browserScreenWidth"] = browserScreenWidth
        map["browserTZ"] = browserTZ
        map["browserUserAgent"] = browserUserAgent
        map["browserAcceptHeader"] = "application/json, text/plain, */*"
        map["browserJavascriptEnabled"] = true
        map["challengeWindowSize"] = "05"
        map["browserIP"] = browserIP ?: "192.168.0.1"
        return map
    }
}