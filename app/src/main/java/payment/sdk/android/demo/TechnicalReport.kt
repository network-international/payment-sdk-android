package payment.sdk.android.demo

import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import payment.sdk.android.core.api.SDKDiagnostics
import payment.sdk.android.demo.model.Environment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * "Technical details" behind a failed payment: the error the launcher returned and every gateway
 * call made for this payment, with full response bodies, as text a tester can read and share.
 *
 * The launchers hand the app one string per failure; the gateway's own reason
 * (`errors[0].errorCode`, the payment's final `state`) is in the response body, which
 * [SDKDiagnostics] keeps. Everything is redacted here because a shared report leaves the
 * device: bearer tokens, card tokens, wallet payloads and anything that looks like a card number.
 */
object TechnicalReport {

    fun build(
        flow: String,
        environment: Environment?,
        orderReference: String?,
        message: String,
        error: String?
    ): String = buildString {
        appendLine("N-Genius payment — technical details")
        appendLine("Time: ${isoUtc(System.currentTimeMillis())}")
        appendLine("Flow: $flow")
        appendLine("Platform: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}")
        environment?.let {
            // Never the API key.
            appendLine(
                "Environment: ${it.nickname ?: it.name} (${it.type.value}) · " +
                    "outlet ${it.outletReference} · ${it.region} · ${it.currency ?: "-"}"
            )
        }
        appendLine("Order reference: ${orderReference ?: "-"}")
        appendLine("Shown to shopper: $message")
        appendLine()
        appendLine("Error")
        appendLine("  ${error?.let(::redactText) ?: "(none reported)"}")

        val exchanges = SDKDiagnostics.recentExchanges()
        appendLine()
        appendLine("Gateway calls (oldest first)")
        if (exchanges.isEmpty()) appendLine("  (none recorded)")
        exchanges.forEach { e ->
            appendLine()
            appendLine("${e.method} ${redactText(e.url)}")
            appendLine("HTTP ${e.statusCode ?: "- (no response)"} · ${isoUtc(e.timestampMs)}")
            appendLine(pretty(e.body))
        }
    }

    private fun isoUtc(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(ms))

    private fun pretty(body: String): String {
        if (body.isBlank()) return "(empty body)"
        return try {
            when (val parsed = JSONTokener(body).nextValue()) {
                is JSONObject -> redact(parsed).toString(2)
                is JSONArray -> redact(parsed).toString(2)
                else -> redactText(body)
            }
        } catch (_: Exception) {
            redactText(body)
        }
    }

    /** Keys whose values must never leave the device, compared lower-cased. */
    private val secretKeys = setOf(
        "\$accesstoken", "accesstoken", "access_token", "refresh_token", "token", "cardtoken",
        "authorization", "apikey", "password", "cvv", "csc", "pan", "cardnumber",
        "paymentdata", "encryptedobj", "signature"
    )

    private fun redact(json: JSONObject): JSONObject {
        val out = JSONObject()
        json.keys().forEach { key ->
            out.put(key, if (key.lowercase() in secretKeys) "[redacted]" else redactValue(json.get(key)))
        }
        return out
    }

    private fun redact(json: JSONArray): JSONArray {
        val out = JSONArray()
        for (i in 0 until json.length()) out.put(redactValue(json.get(i)))
        return out
    }

    private fun redactValue(value: Any?): Any? = when (value) {
        is JSONObject -> redact(value)
        is JSONArray -> redact(value)
        is String -> redactText(value)
        else -> value
    }

    /**
     * Pay-page links carry a one-time `code`, and a card number can turn up in free text (an
     * error message echoing the request); neither belongs in a shared report.
     */
    fun redactText(text: String): String = text
        .replace(Regex("""([?&](?:code|access_token|token)=)[^&\s"]+""")) { "${it.groupValues[1]}[redacted]" }
        .replace(Regex("""\b\d{13,19}\b""")) { if (luhn(it.value)) "[card number redacted]" else it.value }

    private fun luhn(digits: String): Boolean {
        var sum = 0
        var alt = false
        for (i in digits.indices.reversed()) {
            var n = digits[i] - '0'
            if (alt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }
}
