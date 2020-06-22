package payment.sdk.android.core.api

import org.json.JSONObject
import java.net.URLEncoder

// Very bad design of a http client. Needs to be rethought :(
abstract class Body(protected val parameters: Map<String, String>) {

    abstract fun encode(): String

    fun isNotEmpty(): Boolean = parameters.isNotEmpty()

    class Json(parameters: Map<String, String>) : Body(parameters) {
        override fun encode() =
                JSONObject(parameters).toString()
    }

    class Form(parameters: Map<String, String>) : Body(parameters) {
        override fun encode() =
                StringBuilder().apply {
                    for (element in parameters) {
                        append(URLEncoder.encode(element.key, "UTF-8"))
                                .append('=')
                                .append(URLEncoder.encode(element.value, "UTF-8"))
                                .append('&')
                    }
                    deleteCharAt(length - 1)
                }.toString()
    }

    // Doing this hack to get things running
    class JsonStr(parameters: Map<String, String>) : Body(parameters) {
        override fun encode(): String {
            for (element in parameters.values) {
                return element
            }
            return ""
        }
    }

    class Empty : Body(emptyMap()) {
        override fun encode() = ""

    }
}

