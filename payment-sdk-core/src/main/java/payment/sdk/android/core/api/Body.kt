package payment.sdk.android.core.api

import org.json.JSONObject
import java.net.URLEncoder

// Very bad design of a http client. Needs to be rethought :(
abstract class Body(protected val parameters: Map<String, Any>) {

    abstract fun encode(): String

    open fun isNotEmpty(): Boolean = parameters.isNotEmpty()

    class Json(parameters: Map<String, Any>) : Body(parameters) {
        override fun encode() =
                JSONObject(parameters).toString()

        // A JSON body is always sent, even when empty: the encoded form is a valid `{}` and some
        // endpoints (e.g. QPay init) reject a missing body. Use Body.Empty() to send no body.
        override fun isNotEmpty(): Boolean = true
    }

    class Form(parameters: Map<String, Any>) : Body(parameters) {
        override fun encode() =
                StringBuilder().apply {
                    for (element in parameters) {
                        if (element.value is String) {
                            append(URLEncoder.encode(element.key, "UTF-8"))
                                .append('=')
                                .append(URLEncoder.encode(element.value as String, "UTF-8"))
                                .append('&')
                        }
                    }
                    deleteCharAt(length - 1)
                }.toString()
    }

    // Doing this hack to get things running
    class JsonStr(parameters: Map<String, Any>) : Body(parameters) {
        override fun encode(): String {
            for (element in parameters.values) {
                if (element is String) {
                    return element
                }
            }
            return ""
        }
    }

    class StringBody(val value: String): Body(emptyMap()) {
        override fun encode() = value
        override fun isNotEmpty() = value.isNotEmpty()
    }

    class Empty : Body(emptyMap()) {
        override fun encode() = ""

    }
}

