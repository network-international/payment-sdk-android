package payment.sdk.android.qpay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
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

    private var sawAcceptCallback = false
    private var didStartRefetch = false
    private var didDispatchResult = false

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
            ).apply { gravity = android.view.Gravity.CENTER }
        }
        container.addView(progressBar)
        setContentView(container)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWith(QPayLauncher.Result.Canceled)
            }
        })

        startCheckout()
    }

    private fun startCheckout() {
        progressBar.visibility = View.VISIBLE
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
            Log.d(TAG, "shouldOverride method=${request.method} url=$url")
            // Backend's QPay accept callback — process the redirect (so the server records the
            // payment) but mark a flag so we refetch the order on the next page load.
            if (url.contains("/qpay/accept")) {
                Log.d(TAG, "callback URL seen — letting it through; will refetch order on next page load")
                sawAcceptCallback = true
            }
            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "onPageFinished url=$url")
            progressBar.visibility = View.GONE
            // Pin viewport + force input font-size so iOS-style focus zoom doesn't happen on Android either.
            view?.evaluateJavascript(VIEWPORT_PIN_JS, null)
            if (sawAcceptCallback && !didStartRefetch) {
                didStartRefetch = true
                Log.d(TAG, "post-callback onPageFinished → refetching order")
                refetchOrderAndDispatch()
            }
        }
    }

    private fun refetchOrderAndDispatch() {
        progressBar.visibility = View.VISIBLE
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

    companion object {
        private const val TAG = "QPayActivity"
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
