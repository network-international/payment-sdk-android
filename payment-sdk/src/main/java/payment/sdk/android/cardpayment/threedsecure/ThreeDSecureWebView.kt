package payment.sdk.android.cardpayment.threedsecure

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

@SuppressLint("SetJavaScriptEnabled")
class ThreeDSecureWebView : WebView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun init(activity: ThreeDSecureWebViewActivity) {
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
        webChromeClient = ThreeDSecureWebChromeClient(activity)
        webViewClient = ThreeDSecureWebViewClient(activity)
    }
}
