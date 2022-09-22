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
        val acsPaReq = intent.getStringExtra(ThreeDSecureWebViewClient.ACS_PA_REQ_KEY)
        val acsMd = intent.getStringExtra(ThreeDSecureWebViewClient.ACS_MD_KEY)
        val gatewayUrl = intent.getStringExtra(ThreeDSecureWebViewClient.GATEWAY_URL_KEY)

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

    fun finishWithResult(state: String? = null) {
        state?.let {
            val intent = Intent().apply {
                putExtra(KEY_3DS_STATE, it)
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

        fun getIntent(context: Context, acsUrl: String, acsPaReq: String, acsMd: String, gatewayUrl: String) =
                Intent(context, ThreeDSecureWebViewActivity::class.java).apply {
                    putExtra(ThreeDSecureWebViewClient.ACS_URL_KEY, acsUrl)
                    putExtra(ThreeDSecureWebViewClient.ACS_PA_REQ_KEY, acsPaReq)
                    putExtra(ThreeDSecureWebViewClient.ACS_MD_KEY, acsMd)
                    putExtra(ThreeDSecureWebViewClient.GATEWAY_URL_KEY, gatewayUrl)
                }
    }
}
