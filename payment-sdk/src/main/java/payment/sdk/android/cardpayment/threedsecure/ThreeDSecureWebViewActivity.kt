package payment.sdk.android.cardpayment.threedsecure

import payment.sdk.android.sdk.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity


import java.net.URLEncoder
import java.util.Stack

open class ThreeDSecureWebViewActivity : AppCompatActivity() {

    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val content: ViewGroup by lazy {
        findViewById<ViewGroup>(R.id.content)
    }
    private val progressView: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.progress)
    }

    private val threeDSecureWebViews = Stack<ThreeDSecureWebView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_3d_secure)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val acsUrl = intent.getStringExtra(ThreeDSecureWebViewClient.ACS_URL_KEY)
        if (acsUrl == null) {
            finishWithError("ThreeDS one acs url not found")
            return
        }

        val acsPaReq = intent.getStringExtra(ThreeDSecureWebViewClient.ACS_PA_REQ_KEY)
        if (acsPaReq == null) {
            finishWithError("ThreeDS one acsPaReq not found")
            return
        }
        val acsMd = intent.getStringExtra(ThreeDSecureWebViewClient.ACS_MD_KEY)
        if (acsMd == null) {
            finishWithError("ThreeDS one acsMd not found")
            return
        }
        val gatewayUrl = intent.getStringExtra(ThreeDSecureWebViewClient.GATEWAY_URL_KEY)
        if (gatewayUrl == null) {
            finishWithError("ThreeDS one url not found")
            return
        }

        val params = StringBuilder().apply {
            append("PaReq=")
            append(URLEncoder.encode(acsPaReq, "UTF-8"))
            append("&TermUrl=")
            append(URLEncoder.encode(gatewayUrl, "UTF-8"))
            append("&MD=")
            append(URLEncoder.encode(acsMd, "UTF-8"))
        }

        val webView = ThreeDSecureWebView(this)
        webView.init(this)
        webView.postUrl(acsUrl, params.toString().toByteArray())

        pushNewWebView(webView)
    }

    private fun finishWithError(message: String) {
        val intent = Intent().apply {
            putExtra(CardPaymentData.INTENT_DATA_KEY, CardPaymentData(CardPaymentData.STATUS_GENERIC_ERROR, message))
            putExtra(KEY_3DS_STATE, STATUS_PAYMENT_FAILED)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun pushNewWebView(webView: ThreeDSecureWebView) {
        threeDSecureWebViews.push(webView)
        content.removeAllViews()
        content.addView(webView)
    }

    fun popCurrentWebView() {
        threeDSecureWebViews.pop()
        pushNewWebView(threeDSecureWebViews.pop())
    }

    fun setLoadProgress(value: Int) {
        if (value < 100) {
            progressView.progress = value
            progressView.visibility = View.VISIBLE
        } else {
            progressView.visibility = View.GONE
        }
    }

    private fun handleCardPaymentResponse(state: String): CardPaymentData {
        return when (state) {
            ThreeDSecureTwoWebViewActivity.STATUS_PAYMENT_AUTHORISED -> CardPaymentData(
                CardPaymentData.STATUS_PAYMENT_AUTHORIZED)
            ThreeDSecureTwoWebViewActivity.STATUS_PAYMENT_PURCHASED -> CardPaymentData(
                CardPaymentData.STATUS_PAYMENT_PURCHASED)
            ThreeDSecureTwoWebViewActivity.STATUS_PAYMENT_CAPTURED -> CardPaymentData(
                CardPaymentData.STATUS_PAYMENT_CAPTURED)
            ThreeDSecureTwoWebViewActivity.STATUS_PAYMENT_FAILED -> CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED)
            ThreeDSecureTwoWebViewActivity.STATUS_POST_AUTH_REVIEW -> CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW)
            else -> CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED)
        }
    }

    fun finishWithResult(state: String? = null) {
        if(state != null) {
            val intent = Intent().apply {
                putExtra(ThreeDSecureTwoWebViewActivity.KEY_3DS_STATE, state)
                putExtra(ThreeDSecureTwoWebViewActivity.INTENT_DATA_KEY, handleCardPaymentResponse(state))
            }
            setResult(Activity.RESULT_OK, intent)
        } else {
            val intent = Intent().apply {
                putExtra(ThreeDSecureTwoWebViewActivity.INTENT_DATA_KEY, handleCardPaymentResponse("failed"))
            }
            setResult(Activity.RESULT_OK, intent)
        }
        finish()
    }

    override fun onBackPressed() {
        when {
            threeDSecureWebViews.peek().canGoBack() -> threeDSecureWebViews.peek().goBack()
            threeDSecureWebViews.size > 1 -> popCurrentWebView()
            else -> super.onBackPressed()
        }
    }

    fun setTitle(title: String, subTitle: String? = null) {
        toolbar.title = title
        subTitle?.let {
            toolbar.subtitle = subTitle
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val KEY_3DS_STATE = "3ds-state"
        const val STATUS_PAYMENT_FAILED = "FAILED"

        fun getIntent(context: Context, acsUrl: String?, acsPaReq: String?, acsMd: String?, gatewayUrl: String?) =
                Intent(context, ThreeDSecureWebViewActivity::class.java).apply {
                    putExtra(ThreeDSecureWebViewClient.ACS_URL_KEY, acsUrl)
                    putExtra(ThreeDSecureWebViewClient.ACS_PA_REQ_KEY, acsPaReq)
                    putExtra(ThreeDSecureWebViewClient.ACS_MD_KEY, acsMd)
                    putExtra(ThreeDSecureWebViewClient.GATEWAY_URL_KEY, gatewayUrl)
                }
    }
}
