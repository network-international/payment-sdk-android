package payment.sdk.android.bnpl

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
import payment.sdk.android.core.BnplProvider
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.BnplApiInteractor
import payment.sdk.android.core.interactor.BnplApiResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.webview.PaymentWebSession

/**
 * Drives a buy-now-pay-later checkout — Tamara or Tabby — in a WebView.
 *
 * `POST .../{provider}` with the three return URLs answers with the provider's hosted checkout URL
 * and its own reference. The payer approves the plan there, the provider redirects to whichever
 * return URL matches the outcome, and the SDK intercepts that redirect before it loads: the URL
 * points at the paypage, which is meaningless for an SDK-hosted payment because its auth code was
 * already consumed. The reference goes to `/accept` so the backend can finalise the payment, and the
 * order is then polled for the authoritative state.
 */
class BnplActivity : AppCompatActivity() {

    /** Which return URL the provider redirected to. */
    private enum class ReturnLeg { SUCCESS, CANCEL, FAILURE }

    private lateinit var args: BnplLauncher.Config
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    /** Opaque view above the WebView; while visible the payer sees white + spinner only. */
    private lateinit var coverView: FrameLayout

    private var didDispatchResult = false
    private var didStartPolling = false

    /** True when the payment WebView runs on the SDK's own isolated web profile (see Q2). */
    private var isWebSessionIsolated = false

    /**
     * Set once a return leg has been seen, so backing out afterwards resolves the payment from the
     * order rather than discarding a checkout the payer may already have completed.
     */
    private var sawReturn = false

    /**
     * The provider's reference for this checkout, captured at initiation. Tamara returns it there
     * (`tamaraOrderId`), which makes the accept call independent of what the return URL carries.
     */
    private var providerReference: String? = null

    private val provider: BnplProvider get() = args.provider

    private val mainHandler = Handler(Looper.getMainLooper())
    private var revealRunnable: Runnable? = null

    private val httpClient by lazy { CoroutinesGatewayHttpClient() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = BnplLauncher.Config.fromIntent(intent)
        if (config == null) {
            finishWith(BnplLauncher.Result.InvalidRequest)
            return
        }
        args = config

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
            webViewClient = bnplWebViewClient
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
                // Backing out after the return leg would discard a checkout that already went through.
                if (sawReturn) {
                    startPollingIfNeeded()
                } else {
                    // Nothing recorded against the payment yet, so the order is still payable.
                    finishWith(BnplLauncher.Result.Canceled)
                }
            }
        })

        startCheckout()
    }

    private fun startCheckout() {
        showCover()
        lifecycleScope.launch {
            val response = BnplApiInteractor(httpClient).initBnpl(
                url = args.checkoutUrl,
                accessToken = args.accessToken,
                successUrl = args.successUrl,
                cancelUrl = args.cancelUrl,
                failureUrl = args.failureUrl
            )
            when (response) {
                is BnplApiResponse.Error -> {
                    // These providers refuse checkouts for reasons the SDK cannot see coming — a
                    // basket below their minimum, an unsupported currency, a shopper they will not
                    // lend to — and the gateway itself can fail to reach them at all. None of it
                    // touches the order, so the option ends rather than the payment.
                    Log.e(TAG, "${provider.apmName}: initiation failed: ${response.error.message}")
                    finishWith(
                        BnplLauncher.Result.Unavailable(
                            provider,
                            response.error.message ?: "${provider.apmName} could not start a checkout"
                        )
                    )
                }

                is BnplApiResponse.Success -> {
                    val body = response.response
                    // The gateway abandoned the checkout on the payer's behalf — nothing was
                    // charged, so they keep their other options.
                    if (body.cancelled == true) {
                        Log.d(TAG, "${provider.apmName}: gateway reported the checkout as cancelled")
                        finishWith(BnplLauncher.Result.Canceled)
                        return@launch
                    }
                    val checkoutUrl = body.hostedCheckoutUrl
                    if (checkoutUrl.isNullOrBlank()) {
                        Log.e(TAG, "${provider.apmName}: no checkout URL in the response")
                        finishWith(
                            BnplLauncher.Result.Unavailable(
                                provider,
                                body.errorMessage ?: "${provider.apmName} did not return a checkout URL"
                            )
                        )
                        return@launch
                    }
                    providerReference = body.providerReference
                    // Start from a clean session, scoped to the SDK — never the host app (Q2).
                    val origins = listOfNotNull(
                        PaymentWebSession.originOf(checkoutUrl),  // provider hosted checkout
                        PaymentWebSession.originOf(args.orderUrl) // api-gateway
                    )
                    PaymentWebSession.reset(isWebSessionIsolated, origins)
                    webView.loadUrl(checkoutUrl)
                }
            }
        }
    }

    private val bnplWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString()
            Log.d(TAG, "${provider.apmName}: navigating to $url")
            return handleIfReturn(url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // shouldOverrideUrlLoading is never invoked for `loadUrl`, for server redirects or for
            // form POSTs — which is most of what this flow does — so the return leg is caught here
            // too, before the dead paypage hop can render.
            if (handleIfReturn(url)) return
            showCover()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (handleIfReturn(url)) return
            scheduleReveal()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: android.webkit.WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame != true) return
            Log.e(TAG, "${provider.apmName}: main frame error code=${error?.errorCode} url=${request.url}")
            // Stopping the return navigation above surfaces here as a load error of our own making,
            // and the payment is already being resolved, so it must not be treated as a failure.
            if (didStartPolling || didDispatchResult) return
            if (sawReturn) {
                startPollingIfNeeded()
            } else {
                // The hosted page never loaded, so no checkout began — the option ends, not the
                // payment.
                finishWith(
                    BnplLauncher.Result.Unavailable(
                        provider,
                        "Failed to load the ${provider.apmName} page"
                    )
                )
            }
        }
    }

    /**
     * Which return URL, if any, a navigation is heading to. Matching is on the marker parameter the
     * SDK put there rather than on the whole URL, because the providers append their own parameters
     * to the address they were given.
     */
    private fun returnLeg(url: String?): ReturnLeg? {
        val marker = queryParam(url, BnplProvider.RESULT_PARAM) ?: return null
        return when (marker) {
            "success" -> ReturnLeg.SUCCESS
            "cancel" -> ReturnLeg.CANCEL
            "failure" -> ReturnLeg.FAILURE
            else -> null
        }
    }

    private fun queryParam(url: String?, name: String): String? =
        runCatching { Uri.parse(url).getQueryParameter(name) }.getOrNull()?.takeIf { it.isNotBlank() }

    /**
     * The provider's reference read off the return URL. Used only when the initiation response did
     * not already carry one, and every name either provider has been seen to use is accepted.
     */
    private fun referenceFrom(url: String?): String? {
        val names = listOf(provider.acceptIdField, "orderId", "order_id", "paymentId", "payment_id", "checkoutId")
        return names.firstNotNullOfOrNull { queryParam(url, it) }
    }

    /**
     * Catches a return leg wherever it surfaces. The URL points at the paypage, whose session was
     * consumed when the payment started in the SDK rather than there, so it would render a dead
     * "payment link is not exist" page — the navigation is stopped rather than allowed.
     */
    private fun handleIfReturn(url: String?): Boolean {
        val leg = returnLeg(url) ?: return false
        sawReturn = true
        webView.stopLoading()
        showCover()
        finaliseReturn(leg, referenceFrom(url))
        return true
    }

    /**
     * Hands the provider's outcome to the backend, then reads the result off the order. The accept
     * call is what moves the payment out of PENDING, so a failure to reach it is not treated as a
     * declined payment: the order is polled either way and has the final say.
     */
    private fun finaliseReturn(leg: ReturnLeg, referenceFromUrl: String?) {
        if (didDispatchResult || didStartPolling) return

        if (leg == ReturnLeg.CANCEL) {
            Log.d(TAG, "${provider.apmName}: payer cancelled on the provider's page")
            finishWith(BnplLauncher.Result.CanceledOnProvider)
            return
        }

        // The reference captured at initiation is the provider's own and is known to be the one the
        // accept endpoint wants; the return URL is only a fallback for a provider that does not hand
        // it back up front.
        val reference = providerReference ?: referenceFromUrl
        if (reference.isNullOrBlank()) {
            // The providers notify the gateway server to server as well, so a return without a
            // reference is still worth resolving from the order rather than failing outright.
            Log.d(TAG, "${provider.apmName}: no provider reference available — polling without accepting")
            startPollingIfNeeded()
            return
        }

        lifecycleScope.launch {
            val accepted = BnplApiInteractor(httpClient).acceptBnpl(
                url = args.acceptUrl,
                accessToken = args.accessToken,
                idField = provider.acceptIdField,
                idValue = reference
            )
            if (!accepted) Log.e(TAG, "${provider.apmName}: accept call failed")
            startPollingIfNeeded()
        }
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
                Log.d(TAG, "${provider.apmName}: poll attempt=${attempt + 1} state=$state")
                when {
                    state in TERMINAL_SUCCESS_STATES -> {
                        finishWith(BnplLauncher.Result.Success)
                        return@launch
                    }

                    state == STATE_POST_AUTH_REVIEW -> {
                        finishWith(BnplLauncher.Result.PostAuthReview)
                        return@launch
                    }

                    state in TERMINAL_FAILURE_STATES -> {
                        finishWith(BnplLauncher.Result.Failed("state=$state"))
                        return@launch
                    }
                    // Otherwise still in flight — give the backend more time.
                }
            }
            finishWith(BnplLauncher.Result.Failed("Timed out waiting for the payment result"))
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

    private fun finishWith(result: BnplLauncher.Result) {
        if (didDispatchResult) return
        didDispatchResult = true
        val data = Intent().apply { putExtra(BnplLauncherContract.EXTRA_RESULT, result) }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onDestroy() {
        revealRunnable?.let { mainHandler.removeCallbacks(it) }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BnplActivity"

        /** A page must stay put this long (no new load) before it is revealed. */
        private const val REVEAL_DEBOUNCE_MS = 450L
        private const val MAX_POLL_ATTEMPTS = 15
        private const val POLL_INTERVAL_MS = 2_000L

        private const val STATE_POST_AUTH_REVIEW = "POST_AUTH_REVIEW"
        private val TERMINAL_SUCCESS_STATES = setOf("CAPTURED", "AUTHORISED", "PURCHASED", "VERIFIED")
        private val TERMINAL_FAILURE_STATES = setOf("FAILED", "DECLINED", "CANCELLED", "REVERSED")
    }
}
