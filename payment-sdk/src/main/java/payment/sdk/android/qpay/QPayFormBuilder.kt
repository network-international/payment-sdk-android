package payment.sdk.android.qpay

import payment.sdk.android.core.QPayInitResponse

/**
 * Builds the QCB auto-submitting HTML form. Loaded into the WebView via `loadDataWithBaseURL`
 * with the PayPageV2 origin, so the cross-origin POST to the QCB gateway carries
 * `Origin: https://paypage-sandbox.platform.network.ae` (or prod equivalent) — the value QCB
 * whitelists for our merchant.
 */
internal object QPayFormBuilder {

    /**
     * QCB redirect URLs sometimes contain visually-identical Unicode dashes (U+2010..U+2015,
     * U+2212, U+FE58) that the gateway rejects. Replace with ASCII `-` (U+002D).
     */
    private val UNICODE_DASHES = setOf(
        '‐', '‑', '‒', '–', '—', '―',
        '−', '﹘'
    )

    fun normalizeRedirectUri(uri: String): String =
        uri.map { if (UNICODE_DASHES.contains(it)) '-' else it }.joinToString("")

    /**
     * Build the auto-submitting HTML form. Returns null if `redirectUri` is missing.
     */
    fun buildAutoSubmitHTML(response: QPayInitResponse): String? {
        val raw = response.redirectUri?.takeIf { it.isNotEmpty() } ?: return null
        val action = htmlEscape(normalizeRedirectUri(raw))

        val inputs = response.orderedFormFields().joinToString("\n    ") { (name, value) ->
            "<input type=\"hidden\" name=\"${htmlEscape(name)}\" value=\"${htmlEscape(value)}\" />"
        }

        return """
            <!DOCTYPE html>
            <html>
              <head><meta charset="utf-8"><title>QPay</title></head>
              <body>
                <form id="QPayRedirectForm" method="post" action="$action">
                $inputs
                </form>
                <script>document.getElementById('QPayRedirectForm').submit();</script>
              </body>
            </html>
        """.trimIndent()
    }

    private fun htmlEscape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
