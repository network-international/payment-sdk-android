package payment.sdk.android.cardpayment.threedsecure

import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebView

class ThreeDSecureWebChromeClient(
        private val activity: ThreeDSecureWebViewActivity
) : WebChromeClient() {

    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        val newWebView = ThreeDSecureWebView(activity.applicationContext)
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
