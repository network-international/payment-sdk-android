package payment.sdk.android.cardpayment.threedsecure

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.*

class ThreeDSecureWebViewClient(
        private val activity: ThreeDSecureWebViewActivity
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        val endOf3ds = url?.contains("3ds_status=") == true && url.contains("state=")
        if (endOf3ds) {
            view?.stopLoading()
            Uri.parse(url).getQueryParameter("state")?.let { value ->
                on3dSecureCallback(value)
            } ?: activity.finishWithResult() // no state in the url
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.title?.let { activity.setTitle(it, url) }
    }

    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        view?.stopLoading()
        activity.finishWithResult()
    }

    override fun onReceivedSslError(view: WebView?, sslErrorHandler: SslErrorHandler?, error: SslError?) {
        sslErrorHandler?.cancel()
        view?.stopLoading()
        activity.finishWithResult()
    }

    private fun on3dSecureCallback(state: String) {
        activity.finishWithResult(state = state)
    }

    companion object {
        internal const val ACS_URL_KEY = "acsUrl"
        internal const val ACS_PA_REQ_KEY = "acsPaReq"
        internal const val ACS_MD_KEY = "acsMd"
        internal const val GATEWAY_URL_KEY = "gatewayUrl"
    }
}
