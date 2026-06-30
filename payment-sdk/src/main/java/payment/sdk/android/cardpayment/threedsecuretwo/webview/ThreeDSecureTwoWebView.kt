package payment.sdk.android.cardpayment.threedsecuretwo.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

@SuppressLint("SetJavaScriptEnabled")
class ThreeDSecureTwoWebView : WebView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    /**
     * In landscape the IME defaults to fullscreen "extract" mode, whose editor often fails to
     * sync typed characters back into a WebView's input (e.g. the 3DS OTP field stays empty).
     * Disable the fullscreen/extract UI so keystrokes go straight to the WebView field.
     */
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(outAttrs)
        outAttrs.imeOptions = outAttrs.imeOptions or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN
        return inputConnection
    }

    fun init(activity: ThreeDSecureTwoWebViewActivity) {
        settings.apply {
            setSupportMultipleWindows(true)
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            userAgentString = "Android Pay Page"
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            domStorageEnabled = true
            databaseEnabled = true
        }

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
        webChromeClient = ThreeDSecureTwoWebChromeClient(activity)
        webViewClient = ThreeDSecureTwoWebViewClient(activity)
    }
}
