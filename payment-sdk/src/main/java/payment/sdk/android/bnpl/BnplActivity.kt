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
import payment.sdk.android.core.BnplReturn
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.api.SDKHttpException
import payment.sdk.android.core.interactor.BnplApiInteractor
import payment.sdk.android.core.interactor.BnplApiResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.webview.PaymentWebSession
import payment.sdk.android.webview.applyPaymentWindowInsets
import payment.sdk.android.webview.redactUrlForLog

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
    private enum class ReturnLeg {
        SUCCESS, CANCEL, FAILURE,
        /**
         * The payer landed back on our return address, but without our marker to say how it went.
         * Providers that rebuild the query from their own form fields — Tabby's hosted page submits
         * a GET form carrying only `code`, `payment_method` and `payment_id` — drop it. The payer
         * has still returned, so the payment is finalised and the order decides the outcome.
         */
        UNKNOWN
    }

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

    /**
     * What to report if the order is still not final on the next read. Set only when the gateway
     * rejected the accept call: with no provider payment to finalise, the order will not move, so
     * waiting out the full polling window would only keep the payer staring at a spinner.
     */
    private var resultIfNotFinal: BnplLauncher.Result? = null

    /**
     * Set when the payer chose Cancel on the provider's page but the way back lost our marker. The
     * gateway's Tamara test checkout submits the payer's choice as `outcome` to its own `notify`
     * step and then returns to the success address with only `code`, so the return itself looks the
     * same for approve, decline and cancel. Accepting after a cancel fails the order, so a return
     * that follows a cancel goes back to the payment page instead, as the web paypage does.
     */
    private var payerCancelledOnProvider = false

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
            // Shows through the padding the insets add below, and the providers' pages are white.
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
        applyPaymentWindowInsets(container)

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
                    Log.e(TAG, "${provider.apmName}: initiation failed: ${response.error.message}")
                    if (SDKHttpException.isSessionExpired(response.error)) {
                        // The access token is dead, not the provider. Every other method on the
                        // page authenticates with the same token, so sending the payer back to
                        // choose one of them walks them into the same failure. The payment ends
                        // instead, which tells the merchant to start a fresh order.
                        finishWith(
                            BnplLauncher.Result.Failed(
                                "The payment session has expired. Start a new order to pay."
                            )
                        )
                        return@launch
                    }
                    // Otherwise the providers refuse checkouts for reasons the SDK cannot see
                    // coming — a basket below their minimum, an unsupported currency, a shopper
                    // they will not lend to — and the gateway itself can fail to reach them at
                    // all. None of it touches the order, so the option ends rather than the
                    // payment.
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
            Log.d(TAG, "${provider.apmName}: navigating to ${redactUrlForLog(url)}")
            noteProviderOutcome(url)
            return handleIfReturn(url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            noteProviderOutcome(url)
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
            Log.e(TAG, "${provider.apmName}: main frame error code=${error?.errorCode} url=${redactUrlForLog(request.url?.toString())}")
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

    /** The addresses we asked the provider to send the payer back to. */
    private val returnUrls: List<String> by lazy {
        listOf(args.successUrl, args.cancelUrl, args.failureUrl)
    }

    /**
     * Which return URL, if any, a navigation is heading to.
     *
     * Both halves matter: the navigation has to be going to one of the addresses we nominated
     * ([BnplReturn]), and it has to carry our marker. Matching on the marker alone ends the
     * checkout the instant it opens, because the gateway pastes our return URLs unencoded into the
     * provider's checkout URL and the marker surfaces there too. Matching on the address alone
     * cannot tell success from cancel.
     */
    private fun returnLeg(url: String?): ReturnLeg? {
        if (!BnplReturn.isReturnTarget(url, returnUrls)) return null
        return when (BnplReturn.markerOf(url, returnUrls)) {
            "success" -> ReturnLeg.SUCCESS
            "cancel" -> ReturnLeg.CANCEL
            "failure" -> ReturnLeg.FAILURE
            // Arriving at an address we nominated is itself the signal that the payer came back.
            // Only the marker can distinguish a cancel, so without it the payment is finalised and
            // the order has the final say — which it does in every other case anyway.
            else -> ReturnLeg.UNKNOWN
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
     * Records a cancel the payer submitted on the provider's page. Only a `notify` step carrying
     * `outcome=cancel` counts; anything else leaves the order to decide.
     */
    private fun noteProviderOutcome(url: String?) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        if (uri.path?.endsWith("/notify") != true) return
        val outcome = runCatching { uri.getQueryParameter("outcome") }.getOrNull() ?: return
        payerCancelledOnProvider = outcome.equals("cancel", ignoreCase = true)
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

        if (leg == ReturnLeg.CANCEL || (leg == ReturnLeg.UNKNOWN && payerCancelledOnProvider)) {
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
            val status = BnplApiInteractor(httpClient).acceptBnplStatus(
                url = args.acceptUrl,
                accessToken = args.accessToken,
                idField = provider.acceptIdField,
                idValue = reference
            )
            if (status != BnplApiInteractor.ACCEPTED) {
                Log.e(TAG, "${provider.apmName}: accept call failed httpStatus=$status")
            }
            // A 4xx is the gateway saying there is no provider payment to finalise for this order
            // (e.g. Tabby's "Back to merchant", which returns without our marker). The order still
            // gets one read in case it already settled; if it has not, the payer is not left
            // waiting. A network error, 5xx, auth failure or throttle says nothing about the
            // payment, so the full window still applies.
            if (status in 400..499 && status !in setOf(401, 403, 408, 429)) {
                resultIfNotFinal = when (leg) {
                    ReturnLeg.UNKNOWN -> BnplLauncher.Result.CanceledOnProvider
                    ReturnLeg.FAILURE -> BnplLauncher.Result.Failed(
                        "Payment was not completed on ${provider.apmName}"
                    )
                    else -> null
                }
            }
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
            var lastState: String? = null

            repeat(MAX_POLL_ATTEMPTS) { attempt ->
                if (attempt > 0) delay(POLL_INTERVAL_MS)
                val state = readPaymentState()
                if (state != null) lastState = state
                Log.d(TAG, "${provider.apmName}: poll attempt=${attempt + 1} state=$state")
                resultFor(state)?.let {
                    finishWith(it)
                    return@launch
                }
                resultIfNotFinal?.let {
                    Log.d(TAG, "${provider.apmName}: accept was rejected and the order is still $state — not waiting further")
                    finishWith(it)
                    return@launch
                }
                // Otherwise still in flight — give the backend more time.
            }

            // Out of attempts, which is not the same as out of options: the payer may have approved
            // the plan while the gateway was slow to finalise it, and a request that never landed
            // leaves the state unread rather than pending. So the order is asked again, more
            // patiently, before anything is reported — telling a merchant a payment failed when it
            // actually went through is the worst outcome this flow has.
            repeat(FINAL_CHECK_ATTEMPTS) { attempt ->
                delay(FINAL_CHECK_INTERVAL_MS)
                val state = readPaymentState()
                if (state != null) lastState = state
                Log.d(TAG, "${provider.apmName}: final check attempt=${attempt + 1} state=$state")
                resultFor(state)?.let {
                    finishWith(it)
                    return@launch
                }
            }

            // Still unresolved. That is an unknown outcome, not a declined one, so it is reported
            // as something to reconcile from the order rather than as a payment the payer failed
            // to make.
            val waited = (MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS +
                    FINAL_CHECK_ATTEMPTS * FINAL_CHECK_INTERVAL_MS) / 1_000
            val seen = lastState?.takeIf { it.isNotEmpty() }?.let { " (last seen $it)" }.orEmpty()
            Log.e(TAG, "${provider.apmName}: no final state after ${waited}s$seen")
            finishWith(
                BnplLauncher.Result.Failed(
                    "The payment did not reach a final state within ${waited}s$seen; " +
                            "confirm it from the order"
                )
            )
        }
    }

    /** The order's payment state, or null when the order could not be read or carries no payment. */
    private suspend fun readPaymentState(): String? =
        GetOrderApiInteractor(httpClient)
            .getOrder(args.orderUrl, args.accessToken)
            ?.embedded?.payment?.firstOrNull()?.state?.uppercase()

    /** The outcome a state settles on, or null while it is still in flight or could not be read. */
    private fun resultFor(state: String?): BnplLauncher.Result? {
        if (state.isNullOrEmpty()) return null
        return when {
            state in TERMINAL_SUCCESS_STATES -> BnplLauncher.Result.Success
            state == STATE_POST_AUTH_REVIEW -> BnplLauncher.Result.PostAuthReview
            state in TERMINAL_FAILURE_STATES -> BnplLauncher.Result.Failed("state=$state")
            else -> null
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

        /** Slower, last-chance reads of the order once the poll above has run out. */
        private const val FINAL_CHECK_ATTEMPTS = 3
        private const val FINAL_CHECK_INTERVAL_MS = 3_000L

        private const val STATE_POST_AUTH_REVIEW = "POST_AUTH_REVIEW"
        private val TERMINAL_SUCCESS_STATES = setOf("CAPTURED", "AUTHORISED", "PURCHASED", "VERIFIED")
        private val TERMINAL_FAILURE_STATES = setOf("FAILED", "DECLINED", "CANCELLED", "REVERSED")
    }
}
