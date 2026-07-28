package payment.sdk.android.qpay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.QPayApiInteractor
import payment.sdk.android.core.interactor.QPayApiResponse

/**
 * Activity that drives the QPay (QCB Doha Bank EZConnect) checkout in a WebView.
 *
 * Why we use `loadDataWithBaseURL` rather than `loadUrl(payPageUrl)`:
 * QCB's gateway whitelists merchant origins. The merchant order's pay page (e.g.
 * `paypage-sandbox.platform.network.ae`) is registered with QCB. The order's auth code is
 * single-use, and the SDK / unified payment screen has already consumed it; reloading the full
 * pay page here would fail with "payment link does not exist".
 *
 * Instead we load just an auto-submitting HTML form using the pay page's origin as the base URL.
 * That makes the WebView treat the form as if served from `paypage-sandbox.platform.network.ae`,
 * so the cross-origin POST to `pguat.qcb.gov.qa/qcb-pg/api/gateway/2.0` carries
 * `Origin: https://paypage-sandbox.platform.network.ae` — the value QCB whitelists.
 */
class QPayActivity : AppCompatActivity() {

    private lateinit var args: QPayLauncher.Config
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    /// Opaque view above the WebView; while visible the user sees only white + spinner, never the
    /// page being loaded mid-redirect (incl. the brief broken-link/error page).
    private lateinit var coverView: FrameLayout

    private var sawAcceptCallback = false
    private var sawCancelCallback = false
    private var didStartRefetch = false
    private var didDispatchResult = false

    private val mainHandler = Handler(Looper.getMainLooper())
    /// Pending "reveal the WebView" work. Each navigation hop cancels it and re-shows the cover.
    private var revealRunnable: Runnable? = null

    private val httpClient by lazy { CoroutinesGatewayHttpClient() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cfg = QPayLauncher.Config.fromIntent(intent)
        if (cfg == null) {
            finishWith(QPayLauncher.Result.InvalidRequest)
            return
        }
        args = cfg

        if (!args.currencyCode.equals("QAR", ignoreCase = true)) {
            finishWith(QPayLauncher.Result.InvalidRequest)
            return
        }

        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                displayZoomControls = false
                setSupportZoom(false)
            }
            webViewClient = qpayWebViewClient
        }
        container.addView(webView)

        progressBar = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

        // Cover sits above the WebView and hides intermediate redirect pages. Added after the
        // WebView so it's drawn on top; opaque white so nothing behind it shows through.
        coverView = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        coverView.addView(progressBar)
        container.addView(coverView)
        setContentView(container)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWith(QPayLauncher.Result.Canceled)
            }
        })

        resetWebSession()
        startCheckout()
    }

    /**
     * Start every QPay payment from a clean web session.
     *
     * The paypage keeps `paypage_browser_session_id` and `paypage_used_auth_codes` in localStorage,
     * and the gateway sets session cookies. Android's WebView storage is process-global and
     * persistent, so without this a failed payment leaves stale state behind and the *next* order
     * reuses the dead session → "your session has expired or marked as invalid". Clearing cookies +
     * web storage here guarantees each order gets a fresh session.
     *
     * Note: this clears WebView cookies/localStorage for the whole app, which is acceptable — the
     * SDK's WebView flows (QPay, 3DS) all expect to start from a clean session.
     */
    private fun resetWebSession() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        // QCB <-> paypage is a cross-origin POST; allow third-party cookies so the session survives
        // *within* this single flow.
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        debug("web session reset (cookies + web storage cleared)")
    }

    private fun startCheckout() {
        showCover()
        lifecycleScope.launch {
            val response = QPayApiInteractor(httpClient).initQPay(args.qpayUrl, args.accessToken)
            when (response) {
                is QPayApiResponse.Error -> {
                    Log.e(TAG, "initQPay failed: ${response.error.message}")
                    finishWith(QPayLauncher.Result.Failed(response.error.message ?: "QPay init failed"))
                }
                is QPayApiResponse.Success -> {
                    val r = response.response
                    if (r.cancelled == true) {
                        finishWith(QPayLauncher.Result.Canceled)
                        return@launch
                    }
                    val html = QPayFormBuilder.buildAutoSubmitHTML(r)
                    val baseUrl = derivePayPageOrigin(args.payPageUrl)
                    if (html == null || baseUrl == null) {
                        finishWith(QPayLauncher.Result.Failed("Missing redirectUri or payPageUrl"))
                        return@launch
                    }
                    Log.d(TAG, "Loading auto-submit form, baseUrl=$baseUrl action=${r.redirectUri}")
                    webView.loadDataWithBaseURL(
                        baseUrl,
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        }
    }

    private val qpayWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            debug("shouldOverride method=${request.method} url=$url")
            // Backend's QPay accept callback — process the redirect (so the server records the
            // payment) but mark a flag so we refetch the order on the next page load.
            // (User-cancel is detected in onPageStarted instead: it arrives via a POST, and Android
            // does not call shouldOverrideUrlLoading for POST navigations.)
            if (url.contains("/qpay/accept")) {
                Log.d(TAG, "callback URL seen — letting it through; will refetch order on next page load")
                sawAcceptCallback = true
            }
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            debug("onPageStarted url=$url")
            // QCB's user-cancel endpoint. Detected here (not in shouldOverrideUrlLoading) because
            // the payer reaches it via a POST, and Android does NOT call shouldOverrideUrlLoading
            // for POST navigations — but onPageStarted/onPageFinished do fire. onPageFinished then
            // reports Canceled so the host app returns to the order page.
            if (url != null && url.contains("/d3gw/cancel")) {
                Log.d(TAG, "QCB cancel URL seen — user canceled at gateway")
                sawCancelCallback = true
            }
            // A new hop began — re-cover so the page being loaded is never shown until it settles.
            showCover()
        }

        // KEY for finding the broken-link page: a 4xx/5xx response on the main frame is almost
        // certainly the page that briefly flashes. Logs the offending URL = the page origin.
        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            val mainFrame = request?.isForMainFrame == true
            debug("onReceivedHttpError mainFrame=$mainFrame status=${errorResponse?.statusCode} " +
                    "reason=${errorResponse?.reasonPhrase} url=${request?.url}" +
                    if (mainFrame) "  <-- NON-OK (likely the flash page)" else "")
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            val mainFrame = request?.isForMainFrame == true
            debug("onReceivedError mainFrame=$mainFrame code=${error?.errorCode} " +
                    "desc=${error?.description} url=${request?.url}" +
                    if (mainFrame) "  <-- ERROR (likely the flash page)" else "")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            debug("onPageFinished url=$url")
            // Pin viewport + force input font-size so iOS-style focus zoom doesn't happen on Android either.
            view?.evaluateJavascript(VIEWPORT_PIN_JS, null)
            // Snapshot what actually rendered, to fingerprint the flash page.
            view?.evaluateJavascript("document.title") { debug("  document.title=$it url=$url") }
            view?.evaluateJavascript(
                "document.body ? document.body.innerText.substring(0, 300) : '<no body>'"
            ) { debug("  bodyText[0:300]=$it") }
            // User canceled at the QCB gateway — report Canceled so the host app returns to the
            // order page instead of the payment-failed page. Checked before the accept branch: a
            // cancel that still reached /qpay/accept must not be refetched into a Failed result.
            if (sawCancelCallback) {
                Log.d(TAG, "post-cancel onPageFinished → reporting Canceled")
                finishWith(QPayLauncher.Result.Canceled)
                return
            }
            if (sawAcceptCallback && !didStartRefetch) {
                didStartRefetch = true
                Log.d(TAG, "post-callback onPageFinished → refetching order")
                refetchOrderAndDispatch()
                return
            }
            // Only reveal if no further navigation starts within the debounce window. A redirect
            // hop calls onPageStarted → showCover() before this fires, keeping the cover up.
            scheduleReveal()
        }
    }

    // MARK: - Cover (hide intermediate redirect pages)

    /** Re-cover the WebView and cancel any pending reveal. */
    private fun showCover() {
        revealRunnable?.let { mainHandler.removeCallbacks(it) }
        revealRunnable = null
        coverView.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
    }

    /** Reveal the WebView, but only if no new page load starts within [REVEAL_DEBOUNCE_MS]. */
    private fun scheduleReveal() {
        revealRunnable?.let { mainHandler.removeCallbacks(it) }
        val r = Runnable {
            progressBar.visibility = View.GONE
            coverView.visibility = View.GONE
            debug("revealed settled page url=${webView.url}")
        }
        revealRunnable = r
        mainHandler.postDelayed(r, REVEAL_DEBOUNCE_MS)
    }

    /** Timestamped trace so the exact sequence + origin of the brief flash page is captured. */
    private fun debug(msg: String) {
        Log.d(TAG, "[DEBUG ${System.currentTimeMillis()}] $msg")
    }

    private fun refetchOrderAndDispatch() {
        showCover()
        lifecycleScope.launch {
            val order = GetOrderApiInteractor(httpClient).getOrder(args.orderUrl, args.accessToken)
            val state = order?.embedded?.payment?.firstOrNull()?.state.orEmpty()
            val success = state in TERMINAL_SUCCESS_STATES
            Log.d(TAG, "refetch: state=$state success=$success")
            finishWith(if (success) QPayLauncher.Result.Success else QPayLauncher.Result.Failed("state=$state"))
        }
    }

    private fun finishWith(result: QPayLauncher.Result) {
        if (didDispatchResult) return
        didDispatchResult = true
        val data = Intent().apply { putExtra(QPayLauncherContract.EXTRA_RESULT, result) }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun derivePayPageOrigin(payPageUrl: String): String? {
        return runCatching {
            val uri = Uri.parse(payPageUrl)
            val scheme = uri.scheme ?: return@runCatching null
            val host = uri.host ?: return@runCatching null
            "$scheme://$host/"
        }.getOrNull()
    }

    override fun onDestroy() {
        revealRunnable?.let { mainHandler.removeCallbacks(it) }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "QPayActivity"
        /** A page must stay put this long (no new load) before we reveal it; a redirect hop fires
         * onPageStarted well within this window, so intermediate pages stay covered. */
        private const val REVEAL_DEBOUNCE_MS = 450L
        private val TERMINAL_SUCCESS_STATES = setOf(
            "CAPTURED", "AUTHORISED", "PURCHASED", "VERIFIED", "POST_AUTH_REVIEW"
        )

        // Override page viewport so the QCB hosted page doesn't auto-zoom when inputs receive focus.
        private const val VIEWPORT_PIN_JS = """
        (function() {
          try {
            var existing = document.querySelectorAll('meta[name="viewport"]');
            for (var i = 0; i < existing.length; i++) { existing[i].parentNode.removeChild(existing[i]); }
            var meta = document.createElement('meta');
            meta.name = 'viewport';
            meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no';
            (document.head || document.documentElement).appendChild(meta);

            var style = document.createElement('style');
            style.innerHTML = 'input, select, textarea, button { font-size: 16px !important; }';
            (document.head || document.documentElement).appendChild(style);
          } catch (e) {}
        })();
        """
    }
}
