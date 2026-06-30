package payment.sdk.android.cardpayment.threedsecuretwo.webview

import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebView

class ThreeDSecureTwoWebChromeClient(
        private val activity: ThreeDSecureTwoWebViewActivity
) : WebChromeClient() {

    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        // Use the Activity context, NOT applicationContext: a WebView created with the
        // application context is not bound to the Activity's window token, so its IME
        // connection falls back to the fullscreen "extract" editor in landscape. Typed
        // characters then never sync back to the field (e.g. the 3DS OTP popup looks
        // empty) unless committed via the editor action. The Activity context keeps the
        // popup's input bound to the window so the keyboard behaves like the main view.
        val newWebView = ThreeDSecureTwoWebView(activity)
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
