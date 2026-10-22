package payment.sdk.android.webview

import android.net.Uri

/**
 * Parameters that are ours, describe no one, and are the whole point of reading these logs: which
 * return leg a redirect was, and which method it belonged to.
 */
private val LOGGABLE_QUERY_PARAMS = setOf("ni_sdk_result", "payment_method", "outcome")

/**
 * Renders a payment URL safe to write to the device log.
 *
 * These flows log every hop a WebView takes, and payment URLs carry credentials in their query
 * string: the paypage's `code` is a live authorisation code for the order, and the providers append
 * their own references and tokens on the way back. `adb logcat` reads the device log, and on a
 * rooted handset so can anything installed on it.
 *
 * Scheme, host and path are kept — they are what makes a log useful — and the query string is
 * dropped except for the parameters named above. The allowlist is the point: a denylist would keep
 * whatever parameter a provider decides to add next.
 */
fun redactUrlForLog(url: String?): String {
    if (url.isNullOrBlank()) return "<none>"
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return "<unparseable>"

    // An opaque URI (mailto:, tel:, a provider's app link) keeps everything after the scheme in one
    // undelimited part, so there is no host to keep and nothing safe to assume about the rest.
    if (uri.isOpaque) return "${uri.scheme.orEmpty()}:<opaque>"

    val base = buildString {
        uri.scheme?.let { append(it).append("://") }
        uri.host?.let { append(it) }
        if (uri.port != -1) append(':').append(uri.port)
        append(uri.path.orEmpty())
    }
    if (base.isEmpty()) return "<unparseable>"

    val names = runCatching { uri.queryParameterNames }.getOrNull().orEmpty()
    if (names.isEmpty()) return base

    val kept = names.filter { it in LOGGABLE_QUERY_PARAMS }.joinToString("&") { name ->
        "$name=${runCatching { uri.getQueryParameter(name) }.getOrNull().orEmpty()}"
    }
    val redacted = names.count { it !in LOGGABLE_QUERY_PARAMS }

    return buildString {
        append(base)
        if (kept.isNotEmpty()) append('?').append(kept)
        if (redacted > 0) append(" (+").append(redacted).append(" redacted)")
    }
}
