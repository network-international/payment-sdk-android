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
import payment.sdk.android.webview.PaymentWebSession

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

    /** True when the payment WebView runs on the SDK's own isolated web profile (see Q2). */
    private var isWebSessionIsolated = false

    /**
     * Set when the payer is seen hitting Benefit's cancel page. Distinguishes "the payer backed
     * out" from "the payment was declined" — the order reports `FAILED` for both.
     */
    private var payerCancelled = false

    /**
     * Set once the WebView has actually reached Benefit's own site, so leaving it can be read as
     * the payer returning rather than as the flow still starting up.
     */
    private var didReachBenefitHost = false

    /** Host of the hosted page the gateway handed us, e.g. `test.benefit-gateway.bh`. */
    private var benefitHost: String? = null

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
        isWebSessionIsolated = PaymentWebSession.isolate(webView)
        PaymentWebSession.configureCookies(webView, isWebSessionIsolated)
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
                } else if (payerCancelled) {
                    finishWith(BenefitLauncher.Result.CanceledOnProvider)
                } else {
                    // Nothing recorded against the payment yet, so the order is still payable.
                    finishWith(BenefitLauncher.Result.Canceled)
                }
            }
        })

        startCheckout()
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
                    benefitHost = hostOf(paymentUrl)
                    // Start from a clean session, scoped to the SDK — never the host app (Q2).
                    val origins = listOfNotNull(
                        PaymentWebSession.originOf(paymentUrl),   // Benefit hosted page
                        PaymentWebSession.originOf(args.orderUrl) // api-gateway
                    )
                    PaymentWebSession.reset(isWebSessionIsolated, origins)
                    webView.loadUrl(paymentUrl)
                }
            }
        }
    }

    private val benefitWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString()
            noteNavigation(url, "navigating to")

            if (isReturnCallback(url)) {
                // Allowed through so the backend can process the Benefit result.
                return false
            }

            // Never suppress anything on the gateway itself. The accept callback is what tells the
            // backend how the payment went, and its exact path is the gateway's to choose —
            // cancelling it because it did not match the expected shape would strand it in PENDING.
            if (isGatewayHost(url)) {
                return false
            }

            if (sawReturnCallback || hasLeftBenefit(url)) {
                suppressRedirectAndResolve(url)
                return true
            }

            if (isBenefitHost(url)) {
                didReachBenefitHost = true
            }
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // Benefit returns via a form POST, and Android does not call shouldOverrideUrlLoading
            // for POST navigations — onPageStarted does fire, so the callback is detected here too.
            noteNavigation(url, "page started")
            if (isBenefitHost(url)) {
                // Also recorded here, not only in shouldOverrideUrlLoading: that callback is never
                // invoked for `loadUrl`, for server redirects or for form POSTs, which is every
                // navigation this flow actually makes. Without this the payer is never seen reaching
                // Benefit, so leaving it goes unnoticed and the dead paypage hop renders.
                didReachBenefitHost = true
            } else if (isReturnCallback(url) || isGatewayHost(url)) {
                sawReturnCallback = true
            } else if (hasLeftBenefit(url)) {
                // The paypage hop started loading without passing through shouldOverrideUrlLoading;
                // stop it before it can render.
                suppressRedirectAndResolve(url)
                return
            }
            showCover()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (sawReturnCallback || hasLeftBenefit(url)) {
                resolveAfterLeavingBenefit()
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
            // Stopping the paypage hop above surfaces here as a load error of our own making, and
            // the payment is already being resolved, so it must not be treated as a failure.
            if (didStartPolling) return
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

    private fun hostOf(url: String?): String? =
        runCatching { android.net.Uri.parse(url).host }.getOrNull()?.lowercase()

    /**
     * The api-gateway host the order itself lives on. Every server-side callback — the accept
     * endpoint included — is on this host, so navigations to it are never suppressed.
     */
    private val gatewayHost: String? by lazy { hostOf(args.orderUrl) }

    private fun isGatewayHost(url: String?): Boolean {
        val gateway = gatewayHost ?: return false
        return hostOf(url) == gateway
    }

    private fun isBenefitHost(url: String?): Boolean {
        val benefit = benefitHost ?: return false
        return hostOf(url) == benefit
    }

    /**
     * The payer is done with Benefit the moment the WebView leaves Benefit's own site, whatever it
     * lands on next. That destination cannot be predicted from the order: the paypage is on an
     * entirely different domain from the gateway (`paypage-dev.platform.network.ae` versus
     * `api-gateway-dev.ngenius-payments.com`), so a rule written in terms of our own domain misses
     * it and lets the paypage's dead "payment link is not exist" page render. Leaving Benefit is the
     * signal; where it goes afterwards is not our business, because the order decides the outcome.
     */
    private fun hasLeftBenefit(url: String?): Boolean =
        didReachBenefitHost && !isBenefitHost(url)

    /**
     * Benefit's own cancel page, e.g. `test.benefit-gateway.bh/payment/paymentcancel.htm`. Tapping
     * Cancel on the hosted page lands here before Benefit hands control back to us.
     *
     * This — not the gateway's `Error/accept` — is the only cancel signal the WebView ever sees.
     * Benefit reports the outcome to our backend server to server, so the accept callback never
     * appears as a navigation at all; by the time the WebView moves again it is already on the
     * paypage, which looks identical for a cancel and for a decline.
     */
    private fun isBenefitCancelPage(url: String?): Boolean {
        if (!isBenefitHost(url)) return false
        val path = url?.substringBefore('?')?.lowercase() ?: return false
        return path.contains("cancel")
    }

    /**
     * `.../benefit/Error/accept`. Kept as a secondary signal for the case where the callback does
     * travel through the WebView, since the backend records it as a failed payment unconditionally
     * and so it can never mean the payer actually paid.
     */
    private fun isErrorCallback(url: String?): Boolean {
        val path = url?.substringBefore('?')?.lowercase() ?: return false
        return path.contains("/benefit/error/") && path.endsWith("/accept")
    }

    /** Records anything worth knowing about a URL the WebView passes through. */
    private fun noteNavigation(url: String?, source: String) {
        Log.d(TAG, "$source $url")
        if (isBenefitCancelPage(url) || isErrorCallback(url)) {
            Log.d(TAG, "payer cancelled on the hosted page")
            payerCancelled = true
        }
        if (isReturnCallback(url)) {
            sawReturnCallback = true
        }
    }

    /**
     * Suppresses the browser-facing paypage hop the gateway redirects to once it has already
     * recorded the result. That page's session was consumed when the payment started from the SDK
     * rather than the paypage, so it renders a dead end the payer must never see.
     */
    private fun suppressRedirectAndResolve(url: String?) {
        Log.d(TAG, "suppressing post-payment redirect to $url")
        sawReturnCallback = true
        webView.stopLoading()
        showCover()
        resolveAfterLeavingBenefit()
    }

    /**
     * Decides what to do once the payer has left Benefit's site. The gateway has already recorded
     * the result by this point — it is what redirected us onwards — so an error callback needs no
     * confirmation from the order: it can only mean the payer cancelled or the attempt errored, and
     * either way they belong back on the payment page with their other options intact. Anything
     * else is a real result and is read from the order.
     */
    private fun resolveAfterLeavingBenefit() {
        if (didDispatchResult || didStartPolling) return
        if (payerCancelled) {
            Log.d(TAG, "payer cancelled on Benefit's page — returning to the payment page")
            finishWith(BenefitLauncher.Result.CanceledOnProvider)
            return
        }
        startPollingIfNeeded()
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
                        // A failure that followed the error callback is the payer backing out, not a
                        // decline, so it hands them back to the payment page rather than ending it.
                        if (payerCancelled) {
                            finishWith(BenefitLauncher.Result.CanceledOnProvider)
                        } else {
                            finishWith(BenefitLauncher.Result.Failed("state=$state"))
                        }
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
