package payment.sdk.android.cardpayment.threedsecuretwo.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.*
import payment.sdk.android.sdk.R

class ThreeDSecureTwoWebViewClient(
        private val activity: ThreeDSecureTwoWebViewActivity
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        val endOf3dsTwoFingerPrinting = url?.contains("/3ds2/method/notification") == true
        if (endOf3dsTwoFingerPrinting) {
            view?.stopLoading()
            activity.handleThreeDS2StageCompletion()
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.title?.let { activity.setWebViewToolbarTitle(AUTHENTICATING_3DS_TRANSACTION) }
    }

    @Deprecated("Deprecated in Java")
    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        view?.stopLoading()
        activity.finishWithResult()
    }

    override fun onReceivedSslError(view: WebView?, sslErrorHandler: SslErrorHandler?, error: SslError?) {
        sslErrorHandler?.cancel()
        view?.stopLoading()
        activity.finishWithResult()
    }

    companion object {
        private val AUTHENTICATING_3DS_TRANSACTION: Int = R.string.authenticating_three_ds_two
    }
}
