package payment.sdk.android.cardpayment.threedsecuretwo.webview

import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebView

class ThreeDSecureTwoWebChromeClient(
        private val activity: ThreeDSecureTwoWebViewActivity
) : WebChromeClient() {

    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        val newWebView = ThreeDSecureTwoWebView(activity.applicationContext)
        newWebView.init(activity)
        activity.pushNewWebView(newWebView)
        (resultMsg.obj as WebView.WebViewTransport).webView = newWebView
        resultMsg.sendToTarget()

        return true
    }

    override fun onCloseWindow(window: WebView) {
        activity.popCurrentWebView()
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        activity.setLoadProgress(newProgress)
    }
}
