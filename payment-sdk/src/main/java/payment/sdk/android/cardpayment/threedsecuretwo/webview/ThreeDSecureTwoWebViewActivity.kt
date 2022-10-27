package payment.sdk.android.cardpayment.threedsecuretwo.webview

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
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoActivity


import java.net.URLEncoder
import java.util.Stack

open class ThreeDSecureTwoWebViewActivity : AppCompatActivity() {

    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val content: ViewGroup by lazy {
        findViewById<ViewGroup>(R.id.content)
    }
    private val progressView: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.progress)
    }

    private val threeDSecureWebViews = Stack<ThreeDSecureTwoWebView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_3d_secure)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val threeDSMethodURL = intent.getStringExtra(THREE_DS_METHOD_URL)
        val threeDSServerTransID = intent.getStringExtra(THREE_DS_SERVER_TRANS_ID)
        val threeDSMethodData = intent.getStringExtra(THREE_DS_METHOD_DATA)
        val threeDSMethodNotificationURL = intent.getStringExtra(THREE_DS_METHOD_NOTIFICATION_URL)

        val webView = ThreeDSecureTwoWebView(this)
        webView.init(this)
        webView.loadData(
            ThreeDSTwoHTMLFrames.html(
                threeDSMethodURL = threeDSMethodURL,
                threeDSServerTransID = threeDSServerTransID,
                threeDSMethodNotificationURL = threeDSMethodNotificationURL,
                threeDSMethodData = threeDSMethodData
            ),
            "text/html",
            "UTF-8"
        )

        pushNewWebView(webView)
    }

    fun pushNewWebView(webView: ThreeDSecureTwoWebView) {
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

    fun setTitle(title: String) {
        toolbar.title = title
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

        internal const val THREE_DS_METHOD_URL = "threeDSMethodURL"
        internal const val THREE_DS_SERVER_TRANS_ID = "threeDSServerTransID"
        internal const val THREE_DS_METHOD_DATA = "threeDSMethodData"
        internal const val THREE_DS_METHOD_NOTIFICATION_URL = "threeDSMethodNotificationURL"

        fun getIntent(
            context: Context, threeDSMethodURL: String, threeDSServerTransID: String,
            threeDSMethodData: String, threeDSMethodNotificationURL: String
        ) =
            Intent(context, ThreeDSecureTwoWebViewActivity::class.java).apply {
                putExtra(THREE_DS_METHOD_URL, threeDSMethodURL)
                putExtra(THREE_DS_SERVER_TRANS_ID, threeDSServerTransID)
                putExtra(THREE_DS_METHOD_DATA, threeDSMethodData)
                putExtra(THREE_DS_METHOD_NOTIFICATION_URL, threeDSMethodNotificationURL)
            }
    }
}
