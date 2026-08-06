package payment.sdk.android.benefit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.BenefitApiInteractor
import payment.sdk.android.core.interactor.BenefitApiResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor

/**
 * Drives the Benefit (Bahrain debit) checkout in a WebView.
 *
 * `POST .../benefit` returns a hosted page on the Benefit gateway. Once the payer authenticates,
 * Benefit form-POSTs the result to `.../benefit/Response/accept` (or `/Error/accept`) on our
 * gateway, which processes it and answers `303` to the paypage. That paypage hop is meaningless for
 * an SDK-hosted payment — its auth code was already consumed — so the WebView stays covered and the
 * order is polled for the authoritative payment state instead of reading anything off the page.
 */
class BenefitActivity : AppCompatActivity() {

    private lateinit var args: BenefitLauncher.Config
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    /** Opaque view above the WebView; while visible the payer sees white + spinner only. */
    private lateinit var coverView: FrameLayout

    /**
     * Set once the gateway's own accept callback is reached, which means the result has been handed
     * to the backend and the order is worth polling.
     */
    private var sawReturnCallback = false
    private var didStartPolling = false
    private var didDispatchResult = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var revealRunnable: Runnable? = null

    private val httpClient by lazy { CoroutinesGatewayHttpClient() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = BenefitLauncher.Config.fromIntent(intent)
        if (config == null) {
            finishWith(BenefitLauncher.Result.InvalidRequest)
            return
        }
        args = config

        if (!args.currencyCode.equals(SUPPORTED_CURRENCY, ignoreCase = true)) {
            finishWith(BenefitLauncher.Result.InvalidRequest)
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
            webViewClient = benefitWebViewClient
        }
        container.addView(webView)

        progressBar = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

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
                // Backing out after the callback would discard a payment that already went through.
                if (sawReturnCallback) {
                    startPollingIfNeeded()
                } else {
                    finishWith(BenefitLauncher.Result.Canceled)
                }
            }
        })

        resetWebSession()
        startCheckout()
    }

    /**
     * Start every Benefit payment from a clean web session. Android's WebView storage is
     * process-global and persistent, so without this a previous payment's cookies and localStorage
     * leak into the next order's hosted page.
     */
    private fun resetWebSession() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
    }

    private fun startCheckout() {
        showCover()
        lifecycleScope.launch {
            when (val response = BenefitApiInteractor(httpClient).initBenefit(args.benefitUrl, args.accessToken)) {
                is BenefitApiResponse.Error -> {
                    Log.e(TAG, "initBenefit failed: ${response.error.message}")
                    finishWith(BenefitLauncher.Result.Failed(response.error.message ?: "Benefit init failed"))
                }

                is BenefitApiResponse.Success -> {
                    val body = response.response
                    val paymentUrl = body.paymentUrl
                    if (!body.isInitiated || paymentUrl.isNullOrBlank()) {
                        Log.e(TAG, "Benefit initiation rejected status=${body.status}")
                        finishWith(
                            BenefitLauncher.Result.Failed(
                                body.errorMessage ?: "Benefit initiation was not accepted"
                            )
                        )
                        return@launch
                    }
                    webView.loadUrl(paymentUrl)
                }
            }
        }
    }

    private val benefitWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString()
            if (isReturnCallback(url)) {
                // Allowed through so the backend can process the Benefit result.
                sawReturnCallback = true
            }
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // Benefit returns via a form POST, and Android does not call shouldOverrideUrlLoading
            // for POST navigations — onPageStarted does fire, so the callback is detected here too.
            if (isReturnCallback(url)) {
                sawReturnCallback = true
            }
            showCover()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (sawReturnCallback) {
                startPollingIfNeeded()
                return
            }
            scheduleReveal()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: android.webkit.WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame != true) return
            Log.e(TAG, "main frame error code=${error?.errorCode} url=${request.url}")
            // After the accept callback the payment is already decided server-side, so a failure
            // loading the redirect target must not be reported as a failed payment.
            if (sawReturnCallback) {
                startPollingIfNeeded()
            } else {
                finishWith(BenefitLauncher.Result.Failed("Failed to load Benefit page"))
            }
        }
    }

    /**
     * True for the gateway's own return endpoints, `.../benefit/Response/accept` and
     * `.../benefit/Error/accept`. Both mean the result reached the backend; which one it was does
     * not decide the outcome, the polled order state does.
     */
    private fun isReturnCallback(url: String?): Boolean {
        val path = url?.substringBefore('?')?.lowercase() ?: return false
        return path.contains("/benefit/") && path.endsWith("/accept")
    }

    private fun startPollingIfNeeded() {
        if (didStartPolling || didDispatchResult) return
        didStartPolling = true
        showCover()
        pollOrderState()
    }

    /**
     * The gateway may still be finalising the payment when the redirect lands, so the order is
     * polled until it reports a terminal state.
     */
    private fun pollOrderState() {
        lifecycleScope.launch {
            repeat(MAX_POLL_ATTEMPTS) { attempt ->
                if (attempt > 0) delay(POLL_INTERVAL_MS)
                val order = GetOrderApiInteractor(httpClient).getOrder(args.orderUrl, args.accessToken)
                val state = order?.embedded?.payment?.firstOrNull()?.state.orEmpty().uppercase()
                Log.d(TAG, "poll attempt=${attempt + 1} state=$state")
                when {
                    state in TERMINAL_SUCCESS_STATES -> {
                        finishWith(BenefitLauncher.Result.Success)
                        return@launch
                    }

                    state == STATE_POST_AUTH_REVIEW -> {
                        finishWith(BenefitLauncher.Result.PostAuthReview)
                        return@launch
                    }

                    state in TERMINAL_FAILURE_STATES -> {
                        finishWith(BenefitLauncher.Result.Failed("state=$state"))
                        return@launch
                    }
                    // Otherwise still in flight — give the backend more time.
                }
            }
            finishWith(BenefitLauncher.Result.Failed("Timed out waiting for the payment result"))
        }
    }

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
        val runnable = Runnable {
            progressBar.visibility = View.GONE
            coverView.visibility = View.GONE
        }
        revealRunnable = runnable
        mainHandler.postDelayed(runnable, REVEAL_DEBOUNCE_MS)
    }

    private fun finishWith(result: BenefitLauncher.Result) {
        if (didDispatchResult) return
        didDispatchResult = true
        val data = Intent().apply { putExtra(BenefitLauncherContract.EXTRA_RESULT, result) }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onDestroy() {
        revealRunnable?.let { mainHandler.removeCallbacks(it) }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BenefitActivity"
        private const val SUPPORTED_CURRENCY = "BHD"

        /** A page must stay put this long (no new load) before it is revealed. */
        private const val REVEAL_DEBOUNCE_MS = 450L
        private const val MAX_POLL_ATTEMPTS = 15
        private const val POLL_INTERVAL_MS = 2_000L

        private const val STATE_POST_AUTH_REVIEW = "POST_AUTH_REVIEW"
        private val TERMINAL_SUCCESS_STATES = setOf("CAPTURED", "AUTHORISED", "PURCHASED", "VERIFIED")
        private val TERMINAL_FAILURE_STATES = setOf("FAILED", "DECLINED", "CANCELLED", "REVERSED")
    }
}
