package payment.sdk.android.webview

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Confines a payment WebView's cookies and DOM storage to the SDK so that starting a payment never
 * clears the host app's own web session (divergence Q2).
 *
 * When the installed WebView supports multiple profiles the payment runs on a dedicated SDK profile
 * and only that profile is ever cleared. Otherwise the reset is scoped to the payment origins and
 * the process-global cookie/storage stores are left untouched.
 */
object PaymentWebSession {

    private const val SDK_PROFILE = "ni-payment-sdk"

    /**
     * Attach the SDK's isolated web profile to [webView]. Must be called once, after the WebView is
     * constructed and before it loads anything.
     *
     * @return true if the WebView is now running on the isolated SDK profile.
     */
    fun isolate(webView: WebView): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return false
        return runCatching {
            ProfileStore.getInstance().getOrCreateProfile(SDK_PROFILE)
            WebViewCompat.setProfile(webView, SDK_PROFILE)
            true
        }.getOrDefault(false)
    }

    /**
     * The [CookieManager] governing [webView] — the SDK profile's when [isolated], otherwise the
     * process-global one. Enables cookies and (for cross-origin payment flows) third-party cookies
     * on the correct store.
     */
    fun configureCookies(webView: WebView, isolated: Boolean) {
        cookieManager(isolated).apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    /**
     * Start the payment from a clean web session without disturbing the host app.
     *
     * @param isolated whether [isolate] attached the SDK profile to this WebView.
     * @param origins  payment origins ("scheme://host[:port]") to wipe when not isolated — e.g. the
     *                 provider's hosted page and the api-gateway. Ignored on the isolated path.
     */
    fun reset(isolated: Boolean, origins: Collection<String>) {
        if (isolated) {
            // Scoped to the SDK profile only — the host app's default profile is untouched.
            val profile = ProfileStore.getInstance().getOrCreateProfile(SDK_PROFILE)
            profile.cookieManager.apply {
                removeAllCookies(null)
                flush()
            }
            profile.webStorage.deleteAllData()
            return
        }
        // Fallback: clear only the payment origins. Never removeAllCookies()/deleteAllData(), which
        // are process-global and would sign the payer out of the host app.
        val cookieManager = CookieManager.getInstance()
        origins.forEach { origin ->
            cookieManager.getCookie(origin)?.split(";")?.forEach { pair ->
                val name = pair.substringBefore('=').trim()
                if (name.isNotEmpty()) {
                    // An expired cookie for the origin removes it.
                    cookieManager.setCookie(origin, "$name=; Max-Age=0; Path=/")
                }
            }
            WebStorage.getInstance().deleteOrigin(origin)
        }
        cookieManager.flush()
    }

    /** "scheme://host[:port]" for a URL, or null if it has no usable scheme/host. */
    fun originOf(url: String?): String? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null
        return if (uri.port != -1) "$scheme://$host:${uri.port}" else "$scheme://$host"
    }

    private fun cookieManager(isolated: Boolean): CookieManager =
        if (isolated) ProfileStore.getInstance().getOrCreateProfile(SDK_PROFILE).cookieManager
        else CookieManager.getInstance()
}
