package payment.sdk.android.knet

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
import payment.sdk.android.core.KnetInitErrorBody
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.getKnetUrl
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.KnetApiInteractor
import payment.sdk.android.core.interactor.KnetApiResponse
import payment.sdk.android.webview.PaymentWebSession
import payment.sdk.android.webview.applyPaymentWindowInsets
import payment.sdk.android.webview.redactUrlForLog

/**
 * Drives a KNET (Kuwait debit) payment in a WebView.
 *
 * The order is re-read before every attempt so a retry after a cancel posts to the payment's
 * current `payment:knet` link. KNET's page is loaded, and the moment the WebView leaves KNET's site
 * it is covered and the order decides the outcome — the paypage hop the gateway redirects to is a
 * dead end inside the SDK.
 *
 * Differs from Benefit in one deliberate way: a KNET payment is `PENDING` from the moment it is
 * initiated, before the payer has done anything, and the gateway accepts a new initiation on a
 * `PENDING` payment. So backing out while `PENDING` returns the payer to the payment page for
 * another try instead of holding them on KNET's page.
 */
class KnetActivity : AppCompatActivity() {

    private lateinit var args: KnetLauncher.Config
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    /** Opaque view above the WebView; while visible the payer sees white + spinner only. */
    private lateinit var coverView: FrameLayout

    /** Set once a KNET callback on our own gateway is seen: the result reached the backend. */
    private var sawReturnCallback = false
    private var didStartPolling = false
    private var didDispatchResult = false

    /** Set while the order is checked before the payer may leave KNET's page. */
    private var isVerifyingBeforeLeaving = false

    private var isWebSessionIsolated = false

    /** Set when the payer is seen on KNET's cancel page (`/kpg/paymentcancel.htm`). */
    private var payerCancelled = false

    /** Set once the WebView has reached KNET's own site. */
    private var didReachKnetHost = false

    /** Host of the hosted page the gateway handed us, e.g. `kpaytest.com.kw`. */
    private var knetHost: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var revealRunnable: Runnable? = null

    private val httpClient by lazy { CoroutinesGatewayHttpClient() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = KnetLauncher.Config.fromIntent(intent)
        if (config == null) {
            finishWith(KnetLauncher.Result.InvalidRequest)
            return
        }
        args = config

        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.WHITE)
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
            webViewClient = knetWebViewClient
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
        applyPaymentWindowInsets(container)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Frozen while a page is loading or the result is being read: leaving then could
                // abandon a payment KNET is already taking.
                if (didDispatchResult || didStartPolling || isVerifyingBeforeLeaving ||
                    coverView.visibility == View.VISIBLE
                ) {
                    Log.d(TAG, "back ignored while the payment is loading or processing")
                    return
                }
                when {
                    sawReturnCallback -> startPollingIfNeeded()
                    payerCancelled -> finishWith(KnetLauncher.Result.CanceledOnProvider)
                    didReachKnetHost -> verifyBeforeLeaving()
                    else -> finishWith(KnetLauncher.Result.Canceled)
                }
            }
        })

        startCheckout()
    }

    /**
     * Re-reads the order before initiating, so a second attempt on the same order uses the
     * payment's current `payment:knet` link and never re-initiates a payment that went through.
     */
    private fun startCheckout() {
        showCover()
        lifecycleScope.launch {
            val order = GetOrderApiInteractor(httpClient).getOrder(args.orderUrl, args.accessToken)
            val knetUrl = if (order == null) {
                Log.w(TAG, "could not re-read the order before starting; using the original link")
                args.knetUrl
            } else {
                val state = order.embedded?.payment?.firstOrNull()?.state.orEmpty().uppercase()
                when {
                    state in TERMINAL_SUCCESS_STATES -> {
                        Log.d(TAG, "order already paid (state=$state); not starting another attempt")
                        finishWith(KnetLauncher.Result.Success)
                        return@launch
                    }
                    state == STATE_POST_AUTH_REVIEW -> {
                        finishWith(KnetLauncher.Result.PostAuthReview)
                        return@launch
                    }
                }
                order.getKnetUrl() ?: run {
                    Log.e(TAG, "order no longer offers KNET (state=$state)")
                    finishWith(KnetLauncher.Result.Unavailable("The order no longer carries a payment:knet link"))
                    return@launch
                }
            }
            initiate(knetUrl)
        }
    }

    private suspend fun initiate(knetUrl: String) {
        when (val response = KnetApiInteractor(httpClient).initKnet(knetUrl, args.accessToken)) {
            is KnetApiResponse.Error -> {
                // A non-2xx arrives as an exception carrying the raw body; read the gateway's own
                // reason ("Failed to encrypt KNET trandata (invalidKnetPayment)") back out of it.
                val reason = KnetInitErrorBody.reasonFrom(response.error.message)
                    ?: response.error.message
                    ?: "KNET init failed"
                Log.e(TAG, "initKnet failed: $reason")
                finishWith(KnetLauncher.Result.Unavailable(reason))
            }

            is KnetApiResponse.Success -> {
                val body = response.response
                val paymentUrl = body.paymentUrl
                if (!body.isInitiated || paymentUrl.isNullOrBlank()) {
                    val reason = body.errorMessage ?: "KNET initiation was not accepted"
                    Log.e(TAG, "KNET initiation rejected status=${body.status} reason=$reason")
                    finishWith(KnetLauncher.Result.Unavailable(reason))
                    return
                }
                knetHost = hostOf(paymentUrl)
                val origins = listOfNotNull(
                    PaymentWebSession.originOf(paymentUrl),
                    PaymentWebSession.originOf(args.orderUrl)
                )
                PaymentWebSession.reset(isWebSessionIsolated, origins)
                webView.loadUrl(paymentUrl)
            }
        }
    }

    private val knetWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString()
            noteNavigation(url, "navigating to")

            // Never suppress anything on the gateway itself: its callback is what records the
            // result, and cancelling it would strand the payment in PENDING.
            if (isGatewayHost(url)) return false

            if (sawReturnCallback || hasLeftKnet(url)) {
                suppressRedirectAndResolve(url)
                return true
            }
            if (isKnetHost(url)) didReachKnetHost = true
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // shouldOverrideUrlLoading is not called for loadUrl, server redirects or form POSTs —
            // which is every navigation this flow makes — so the same decisions are taken here.
            noteNavigation(url, "page started")
            if (isKnetHost(url)) {
                didReachKnetHost = true
            } else if (isGatewayHost(url)) {
                if (didReachKnetHost) sawReturnCallback = true
            } else if (hasLeftKnet(url)) {
                suppressRedirectAndResolve(url)
                return
            }
            showCover()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (sawReturnCallback || hasLeftKnet(url)) {
                resolveAfterLeavingKnet()
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
            Log.e(TAG, "main frame error code=${error?.errorCode} url=${redactUrlForLog(request.url?.toString())}")
            // Stopping the paypage hop surfaces here as a load error of our own making.
            if (didStartPolling || didDispatchResult) return
            when {
                sawReturnCallback || hasLeftKnet(request.url?.toString()) -> resolveAfterLeavingKnet()
                // On KNET's site an attempt may be in flight: read the order once — paid ends the
                // payment, anything still payable goes back to the payment page for another try.
                didReachKnetHost -> verifyBeforeLeaving()
                else -> finishWith(KnetLauncher.Result.Unavailable("Failed to load KNET page"))
            }
        }
    }

    private fun hostOf(url: String?): String? =
        runCatching { android.net.Uri.parse(url).host }.getOrNull()?.lowercase()

    /** The api-gateway host the order lives on. Navigations to it are never suppressed. */
    private val gatewayHost: String? by lazy { hostOf(args.orderUrl) }

    private fun isGatewayHost(url: String?): Boolean {
        val gateway = gatewayHost ?: return false
        return hostOf(url) == gateway
    }

    private fun isKnetHost(url: String?): Boolean {
        val knet = knetHost ?: return false
        return hostOf(url) == knet
    }

    /** The payer is done with KNET the moment the WebView leaves KNET's site. */
    private fun hasLeftKnet(url: String?): Boolean = didReachKnetHost && !isKnetHost(url)

    /** KNET's own cancel page, `kpaytest.com.kw/kpg/paymentcancel.htm` on the test host. */
    private fun isKnetCancelPage(url: String?): Boolean {
        if (!isKnetHost(url)) return false
        val path = url?.substringBefore('?')?.lowercase() ?: return false
        return path.contains("cancel")
    }

    private fun noteNavigation(url: String?, source: String) {
        Log.d(TAG, "$source ${redactUrlForLog(url)}")
        if (isKnetCancelPage(url)) {
            Log.d(TAG, "payer cancelled on the hosted page")
            payerCancelled = true
        }
    }

    private fun suppressRedirectAndResolve(url: String?) {
        Log.d(TAG, "suppressing post-payment redirect to ${redactUrlForLog(url)}")
        sawReturnCallback = true
        webView.stopLoading()
        showCover()
        resolveAfterLeavingKnet()
    }

    /**
     * Once the payer has left KNET's site: a cancel seen on KNET's page goes straight back to the
     * payment page; anything else is a real result and is read from the order.
     */
    private fun resolveAfterLeavingKnet() {
        if (didDispatchResult || didStartPolling) return
        if (payerCancelled) {
            Log.d(TAG, "payer cancelled on KNET's page — returning to the payment page")
            finishWith(KnetLauncher.Result.CanceledOnProvider)
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

    private fun pollOrderState() {
        lifecycleScope.launch {
            repeat(MAX_POLL_ATTEMPTS) { attempt ->
                if (attempt > 0) delay(POLL_INTERVAL_MS)
                val order = GetOrderApiInteractor(httpClient).getOrder(args.orderUrl, args.accessToken)
                val state = order?.embedded?.payment?.firstOrNull()?.state.orEmpty().uppercase()
                Log.d(TAG, "poll attempt=${attempt + 1} state=$state")
                when {
                    state in TERMINAL_SUCCESS_STATES -> {
                        finishWith(KnetLauncher.Result.Success)
                        return@launch
                    }
                    state == STATE_POST_AUTH_REVIEW -> {
                        finishWith(KnetLauncher.Result.PostAuthReview)
                        return@launch
                    }
                    state in TERMINAL_FAILURE_STATES -> {
                        finishWith(
                            if (payerCancelled) KnetLauncher.Result.CanceledOnProvider
                            else KnetLauncher.Result.Failed("state=$state")
                        )
                        return@launch
                    }
                }
            }
            finishWith(KnetLauncher.Result.Failed("Timed out waiting for the payment result"))
        }
    }

    /**
     * Reads the order once before letting the payer leave KNET's page. Paid ends the flow as a
     * success; every other state — including PENDING, which every KNET payment is until the payer
     * finishes on KNET's page — returns them to the payment page, where the same order can be tried
     * again. If the order cannot be read, the payer stays on KNET's page.
     */
    private fun verifyBeforeLeaving() {
        isVerifyingBeforeLeaving = true
        showCover()
        lifecycleScope.launch {
            val order = GetOrderApiInteractor(httpClient).getOrder(args.orderUrl, args.accessToken)
            isVerifyingBeforeLeaving = false
            if (order == null) {
                Log.e(TAG, "could not read the order; staying on KNET's page")
                scheduleReveal()
                return@launch
            }
            val state = order.embedded?.payment?.firstOrNull()?.state.orEmpty().uppercase()
            Log.d(TAG, "checked the order before leaving KNET's page, state=$state")
            when {
                state in TERMINAL_SUCCESS_STATES -> finishWith(KnetLauncher.Result.Success)
                state == STATE_POST_AUTH_REVIEW -> finishWith(KnetLauncher.Result.PostAuthReview)
                state in TERMINAL_FAILURE_STATES -> finishWith(KnetLauncher.Result.CanceledOnProvider)
                else -> finishWith(KnetLauncher.Result.Canceled)
            }
        }
    }

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

    private fun finishWith(result: KnetLauncher.Result) {
        if (didDispatchResult) return
        didDispatchResult = true
        val data = Intent().apply { putExtra(KnetLauncherContract.EXTRA_RESULT, result) }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onDestroy() {
        revealRunnable?.let { mainHandler.removeCallbacks(it) }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "KnetActivity"

        private const val REVEAL_DEBOUNCE_MS = 450L
        private const val MAX_POLL_ATTEMPTS = 15
        private const val POLL_INTERVAL_MS = 2_000L

        private const val STATE_POST_AUTH_REVIEW = "POST_AUTH_REVIEW"
        private val TERMINAL_SUCCESS_STATES = setOf("CAPTURED", "AUTHORISED", "PURCHASED", "VERIFIED")
        private val TERMINAL_FAILURE_STATES = setOf("FAILED", "DECLINED", "CANCELLED", "REVERSED")
    }
}
