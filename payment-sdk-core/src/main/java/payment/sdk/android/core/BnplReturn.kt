package payment.sdk.android.core

import android.net.Uri

/**
 * Decides whether a navigation is the payer coming back from a buy-now-pay-later checkout.
 *
 * The marker alone cannot decide this. The gateway builds the provider's checkout URL by pasting
 * our return URLs into its query string **without encoding them**, so the checkout page's own
 * address arrives carrying `ni_sdk_result=success`:
 *
 * ```
 * .../fake/tabby/checkout/payment?orderRef=…&merchantUrl=https://paypage/v2?code=X
 *                                &payment_method=tabby&ni_sdk_result=success
 * ```
 *
 * Matching the marker anywhere would end the checkout the moment it opened — the payer never sees
 * the provider's page, no `/accept` is ever sent, and the payment sits at PENDING until it times
 * out. So a return leg has to be a navigation to one of the addresses we actually asked the
 * provider to send the payer to, compared on origin and path: the providers append their own
 * parameters to those addresses, so the query cannot be part of the comparison.
 */
object BnplReturn {

    /** Scheme, host, port and path of [url] — what identifies a return address; the query cannot. */
    fun originAndPath(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "$scheme://$host$port${uri.path.orEmpty().trimEnd('/')}"
    }

    /** True when [url] is heading to one of [returnUrls]. */
    fun isReturnTarget(url: String?, returnUrls: List<String>): Boolean {
        val target = originAndPath(url) ?: return false
        return returnUrls.any { originAndPath(it) == target }
    }

    /**
     * The marker's value when [url] is a genuine return leg, otherwise null — including when the
     * marker is present but the navigation is going somewhere we never nominated.
     */
    fun markerOf(url: String?, returnUrls: List<String>): String? {
        if (!isReturnTarget(url, returnUrls)) return null
        return runCatching { Uri.parse(url).getQueryParameter(BnplProvider.RESULT_PARAM) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
