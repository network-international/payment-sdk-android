package payment.sdk.android.clicktopay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import payment.sdk.android.SDKConfig
import kotlinx.coroutines.launch
import payment.sdk.android.clicktopay.model.ClickToPayEffect
import payment.sdk.android.clicktopay.model.ClickToPayState
import payment.sdk.android.sdk.R
import java.io.ByteArrayInputStream

/**
 * Activity that handles the Click to Pay checkout flow.
 * Uses a WebView to load the Visa JavaScript SDK and communicate via JavaScript bridge.
 *
 * Navigates the WebView to a real HTTPS URL and intercepts the request via
 * shouldInterceptRequest to serve local HTML from assets. This ensures the WebView
 * has a proper HTTPS security origin for postMessage/iframe communication while
 * allowing external scripts (Visa SDK) to load normally over the network.
 */
class ClickToPayActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = SDKConfig.getLanguage()
        val locale = java.util.Locale(lang)
        val config = newBase.resources.configuration.apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var popupContainer: FrameLayout

    // Flag to prevent multiple SDK initializations
    private var sdkInitialized = false
    // Track readiness for SDK initialization (both page load and VCTP config needed)
    private var pageFinishedLoading = false
    private var vctpConfigReady = false

    // The URL we navigate to and intercept to serve local HTML
    private lateinit var interceptUrl: String
    // Flag to ensure we only intercept the first load (not subsequent navigations)
    private var htmlIntercepted = false

    // Args must be validated before accessing viewModel
    private lateinit var inputArgs: ClickToPayLauncher.Config

    private val viewModel: ClickToPayViewModel by viewModels {
        ClickToPayViewModel.Factory(inputArgs)
    }

    // Flag to prevent multiple native lookup triggers
    private var lookupTriggered = false

    // Flag to track WebView renderer crash
    private var renderProcessGone = false

    // Popup WebView for Mastercard DCF enrollment
    private var popupWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Validate args FIRST before anything else
        val args = ClickToPayLauncher.Config.fromIntent(intent = intent)
        if (args == null) {
            Log.e(TAG, "ClickToPayActivity input arguments were not found")
            finishWithResult(ClickToPayLauncher.Result.Failed("ClickToPayActivity input arguments were not found"))
            return
        }
        inputArgs = args

        setContentView(R.layout.activity_click_to_pay)

        setupViews()
        setupWebView()
        setupBackPressHandler()
        observeViewModel()

        if (!inputArgs.testOtpMode) {
            // Fetch VCTP config in parallel while loading WebView
            viewModel.fetchVctpConfig { _ ->
                runOnUiThread {
                    vctpConfigReady = true
                    tryInitializeSdk()
                }
            }
        }

        // Load HTML directly with proper base URL origin
        loadHtml()
    }

    private fun setupViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        popupContainer = findViewById(R.id.popupContainer)

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Enable third-party cookies - required for Visa SRC SDK iframes
        // to communicate with card brand backends
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Add JavaScript interface
        webView.addJavascriptInterface(
            ClickToPayJsBridge(viewModel),
            "ClickToPayBridge"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                // Intercept only the initial navigation URL to serve local HTML
                if (!htmlIntercepted && url == interceptUrl) {
                    htmlIntercepted = true
                    Log.d(TAG, "Intercepting request for: $url, serving local HTML")
                    return try {
                        val htmlContent = assets.open("click_to_pay.html")
                            .bufferedReader()
                            .use { it.readText() }
                        WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            ByteArrayInputStream(htmlContent.toByteArray(Charsets.UTF_8))
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load local HTML", e)
                        null
                    }
                }
                return null
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                val url = error?.url.orEmpty()
                // The Visa SRC SDK loads iframes from multiple card brand domains
                // (visa.com, mastercard.com, etc). On emulators the sandbox SSL
                // certificates are often untrusted. Allow all SSL errors when the
                // SDK URL points to the sandbox environment.
                val isSandbox = inputArgs.clickToPayConfig.isSandbox
                if (isSandbox) {
                    Log.d(TAG, "Sandbox mode: proceeding with SSL error for: $url")
                    handler?.proceed()
                } else if (url.contains("secure.checkout.visa.com")) {
                    Log.d(TAG, "Proceeding with SSL error for trusted domain: $url")
                    handler?.proceed()
                } else {
                    Log.e(TAG, "SSL error for untrusted domain: $url, error: ${error?.primaryError}")
                    super.onReceivedSslError(view, handler, error)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Page started loading: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")

                if (!sdkInitialized) {
                    if (inputArgs.testOtpMode) {
                        sdkInitialized = true
                        showTestOtpPage()
                    } else {
                        pageFinishedLoading = true
                        tryInitializeSdk()
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val isMainFrame = request?.isForMainFrame == true
                Log.e(TAG, "WebView error: ${error?.description} for URL: ${request?.url}, isMainFrame=$isMainFrame")
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                Log.e(TAG, "WebView renderer process gone! didCrash=${detail?.didCrash()}, rendererPriorityAtExit=${detail?.rendererPriorityAtExit()}")
                renderProcessGone = true
                finishWithResult(ClickToPayLauncher.Result.Failed("WebView renderer process crashed"))
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d(TAG, "JS Console: ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                return true
            }

            // Handle popup windows for Mastercard DCF enrollment
            @SuppressLint("SetJavaScriptEnabled")
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                Log.d(TAG, "onCreateWindow called, creating popup WebView")
                val popup = WebView(this@ClickToPayActivity).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.setSupportMultipleWindows(true)
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                }

                // Enable third-party cookies for the popup
                CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true)

                popup.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        Log.d(TAG, "Popup navigating to: ${request?.url}")
                        return false
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        if (inputArgs.clickToPayConfig.isSandbox) {
                            handler?.proceed()
                        } else {
                            super.onReceivedSslError(view, handler, error)
                        }
                    }
                }

                popup.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        Log.d(TAG, "Popup window closed by script")
                        dismissPopup()
                    }
                }

                // Add popup to the visible container so the user can interact with it
                popupContainer.removeAllViews()
                popupContainer.addView(popup, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
                popupContainer.visibility = View.VISIBLE

                popupWebView = popup
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = popup
                resultMsg?.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                Log.d(TAG, "Main window onCloseWindow")
                dismissPopup()
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If popup is visible, dismiss it first (e.g., Mastercard DCF)
                if (popupContainer.visibility == View.VISIBLE) {
                    dismissPopup()
                    return
                }
                when (viewModel.state.value) {
                    is ClickToPayState.Processing -> {
                        // Don't allow back during processing
                    }
                    else -> {
                        finishWithResult(ClickToPayLauncher.Result.Canceled)
                    }
                }
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                Log.d(TAG, "State changed: ${state::class.simpleName}")
                when (state) {
                    is ClickToPayState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is ClickToPayState.Processing -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    is ClickToPayState.LookingUpConsumer -> {
                        progressBar.visibility = View.GONE
                        // SDK just initialized - trigger native email lookup if available
                        triggerNativeLookup()
                    }
                    else -> {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                Log.d(TAG, "Effect received: ${effect::class.simpleName}")
                handleEffect(effect)
            }
        }
    }

    private fun handleEffect(effect: ClickToPayEffect) {
        when (effect) {
            is ClickToPayEffect.PaymentSuccess -> {
                finishWithResult(ClickToPayLauncher.Result.Success)
            }
            is ClickToPayEffect.PaymentAuthorised -> {
                finishWithResult(ClickToPayLauncher.Result.Authorised)
            }
            is ClickToPayEffect.PaymentCaptured -> {
                finishWithResult(ClickToPayLauncher.Result.Captured)
            }
            is ClickToPayEffect.PaymentPending -> {
                // Handled by ViewModel polling
            }
            is ClickToPayEffect.Canceled -> {
                finishWithResult(ClickToPayLauncher.Result.Canceled)
            }
            is ClickToPayEffect.ShowError -> {
                finishWithResult(ClickToPayLauncher.Result.Failed(effect.message))
            }
            is ClickToPayEffect.Requires3DS -> {
                finishWithResult(
                    ClickToPayLauncher.Result.Requires3DS(
                        acsUrl = effect.acsUrl,
                        acsPaReq = effect.acsPaReq,
                        acsMd = effect.acsMd
                    )
                )
            }
            is ClickToPayEffect.Requires3DSTwo -> {
                finishWithResult(
                    ClickToPayLauncher.Result.Requires3DSTwo(
                        threeDSMethodUrl = effect.threeDSMethodUrl,
                        threeDSServerTransId = effect.threeDSServerTransId,
                        directoryServerId = effect.directoryServerId,
                        threeDSMessageVersion = effect.threeDSMessageVersion,
                        acsUrl = effect.acsUrl,
                        threeDSTwoAuthenticationURL = effect.threeDSTwoAuthenticationURL,
                        threeDSTwoChallengeResponseURL = effect.threeDSTwoChallengeResponseURL,
                        outletRef = effect.outletRef,
                        orderRef = effect.orderRef,
                        paymentRef = effect.paymentRef,
                        threeDSMethodData = effect.threeDSMethodData,
                        threeDSMethodNotificationURL = effect.threeDSMethodNotificationURL,
                        paymentCookie = effect.paymentCookie,
                        orderUrl = effect.orderUrl
                    )
                )
            }
        }
    }

    private fun finishWithResult(result: ClickToPayLauncher.Result) {
        Log.d(TAG, "finishWithResult: ${result::class.simpleName}" +
            if (result is ClickToPayLauncher.Result.Failed) " error=${result.error}" else "")
        val intent = Intent().apply {
            putExtra(ClickToPayLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }


    /**
     * Dismiss the popup WebView used for Mastercard DCF enrollment
     */
    private fun dismissPopup() {
        popupWebView?.let { popup ->
            popupContainer.removeView(popup)
            popup.destroy()
        }
        popupWebView = null
        popupContainer.visibility = View.GONE
    }

    override fun onDestroy() {
        dismissPopup()
        webView.destroy()
        super.onDestroy()
    }

    /**
     * Load Click to Pay HTML by navigating to a real HTTPS URL.
     * The shouldInterceptRequest callback will intercept this URL and serve
     * local HTML from assets. This gives the WebView a proper HTTPS origin
     * while allowing external scripts (Visa SDK) to load over the network.
     */
    private fun loadHtml() {
        if (inputArgs.testOtpMode) {
            // In test mode, load HTML directly from assets — no network needed
            Log.d(TAG, "Test OTP mode: loading HTML from assets")
            interceptUrl = ""
            webView.loadUrl("file:///android_asset/click_to_pay.html")
            return
        }

        val baseOrigin = inputArgs.payPageUrl?.let { payPageUrl ->
            try {
                val uri = Uri.parse(payPageUrl)
                "${uri.scheme}://${uri.host}"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse pay page URL", e)
                null
            }
        } ?: "https://secure.checkout.visa.com"

        // Use a path that won't conflict with real resources on the server
        interceptUrl = "$baseOrigin/click-to-pay-local.html"
        Log.d(TAG, "Loading Click to Pay HTML via intercept URL: $interceptUrl")
        webView.loadUrl(interceptUrl)
    }

    /**
     * Try to initialize the SDK — only proceeds when both page load and VCTP config are ready.
     */
    private fun tryInitializeSdk() {
        if (sdkInitialized || !pageFinishedLoading || !vctpConfigReady) return
        sdkInitialized = true
        initializeSdk()
    }

    /**
     * Initialize the SDK after HTML is loaded.
     * Injects the lookup GIF, sets nativeWillLookup if email provided,
     * then calls initializeSdk with config JSON.
     *
     * Note: lookupConsumer is NOT called here because initializeSdk returns
     * before the Visa SDK script loads (async). The lookup is triggered
     * from observeViewModel when the state transitions to LookingUpConsumer
     * (which happens when the JS SDK calls onSdkInitialized).
     */
    private fun initializeSdk() {
        // Inject the lookup GIF and Visa logo from assets
        injectLookupGif()
        injectVisaLogo()

        // Inject button colors from color resources
        injectButtonColors()

        // Check if we have user email (native will handle lookup)
        val userEmail = inputArgs.userEmail
        if (!userEmail.isNullOrEmpty()) {
            // Tell JS that native will trigger lookup
            webView.evaluateJavascript("setNativeWillLookup()") { }
        } else {
            // No email provided — enable probe mode to check for stored recognition
            webView.evaluateJavascript("setProbeMode()") { }
        }

        // Initialize the SDK
        val configJson = viewModel.getInitConfigJson()
        val escapedJson = configJson
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("'", "\\'")
            .trim()
        webView.evaluateJavascript("initializeSdk('$escapedJson')") { result ->
            Log.d(TAG, "initializeSdk result: $result")
        }
    }

    /**
     * Trigger native email lookup after SDK is fully initialized.
     * Called from state observer when LookingUpConsumer state is received.
     */
    private fun triggerNativeLookup() {
        if (lookupTriggered || renderProcessGone) return
        val userEmail = inputArgs.userEmail
        if (!userEmail.isNullOrEmpty()) {
            lookupTriggered = true
            Log.d(TAG, "Triggering native lookup for email")
            val escapedEmail = userEmail.replace("'", "\\'")
            webView.evaluateJavascript("lookupConsumer('$escapedEmail')") { }
        }
    }

    /**
     * Load the lookup GIF from assets, convert to base64, and inject into HTML
     */
    private fun injectLookupGif() {
        try {
            val gifBytes = assets.open("ctp_cards_loader.gif").use { it.readBytes() }
            val base64 = Base64.encodeToString(gifBytes, Base64.NO_WRAP)
            val dataUri = "data:image/gif;base64,$base64"
            // Escape for JS string
            webView.evaluateJavascript("setLookupGif('$dataUri')") { }
            Log.d(TAG, "Lookup GIF injected (${gifBytes.size} bytes)")
        } catch (e: Exception) {
            Log.d(TAG, "No lookup GIF found in assets: ${e.message}")
        }
    }

    /**
     * Load the Visa logo from assets, convert to base64, and inject into HTML
     */
    private fun injectVisaLogo() {
        try {
            val logoBytes = assets.open("visa_logo.png").use { it.readBytes() }
            val base64 = Base64.encodeToString(logoBytes, Base64.NO_WRAP)
            val dataUri = "data:image/png;base64,$base64"
            webView.evaluateJavascript("setVisaLogo('$dataUri')") { }
            Log.d(TAG, "Visa logo injected (${logoBytes.size} bytes)")
        } catch (e: Exception) {
            Log.d(TAG, "No Visa logo found in assets: ${e.message}")
        }
    }

    /**
     * Show OTP page directly for testing, bypassing SDK initialization.
     */
    private fun showTestOtpPage() {
        Log.d(TAG, "Test OTP mode: showing OTP page directly")
        progressBar.visibility = View.GONE
        webView.evaluateJavascript("showTestOtpPage()") { result ->
            Log.d(TAG, "showTestOtpPage result: $result")
        }
    }

    private fun injectButtonColors() {
        val btnBg = colorToHex(R.color.payment_sdk_pay_button_background_color)
        val btnText = colorToHex(R.color.payment_sdk_pay_button_text_color)
        val disabledBg = colorToHex(R.color.payment_sdk_button_disabled_background_color)
        val disabledText = colorToHex(R.color.payment_sdk_button_disabled_text_color)
        val css = """
            .btn-primary { background: $btnBg !important; color: $btnText !important; }
            .btn-pay { background: $btnBg !important; color: $btnText !important; }
            .add-card-submit-btn { background: $btnBg !important; color: $btnText !important; }
            .otp-verify-btn { background: $btnBg !important; color: $btnText !important; }
            .btn-primary:disabled { background: $disabledBg !important; color: $disabledText !important; }
            .btn-pay:disabled { background: $disabledBg !important; color: $disabledText !important; }
            .add-card-submit-btn:disabled { background: $disabledBg !important; color: $disabledText !important; }
            .otp-verify-btn:disabled { background: $disabledBg !important; color: $disabledText !important; }
        """.trimIndent().replace("\n", " ")
        webView.evaluateJavascript(
            "(function(){var s=document.createElement('style');s.textContent='$css';document.head.appendChild(s);})()", null
        )
    }

    private fun colorToHex(colorRes: Int): String {
        val color = SDKConfig.getColorOverride(colorRes)
            ?: ContextCompat.getColor(this, colorRes)
        return String.format("#%06X", 0xFFFFFF and color)
    }

    companion object {
        private const val TAG = "ClickToPayActivity"
    }
}
