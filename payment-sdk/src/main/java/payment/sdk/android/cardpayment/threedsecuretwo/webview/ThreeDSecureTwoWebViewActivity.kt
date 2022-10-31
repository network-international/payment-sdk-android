package payment.sdk.android.cardpayment.threedsecuretwo.webview


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.gson.Gson
import payment.sdk.android.cardpayment.CardPaymentApiInteractor
import payment.sdk.android.cardpayment.CardPaymentPresenter
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.dependency.StringResources
import payment.sdk.android.core.dependency.StringResourcesImpl
import payment.sdk.android.sdk.R
import java.net.URLEncoder
import java.util.*


open class ThreeDSecureTwoWebViewActivity : AppCompatActivity() {
    private fun getIpUrl(stringVal: String, outletRef: String, orderRef: String): String {
        val slug =
            "/api/outlets/$outletRef/orders/$orderRef/payments/{paymentRef}/3ds2/requester-ip"
        if (stringVal.contains("-uat", true) ||
            stringVal.contains("sandbox", true)
        ) {
            return "https://paypage.sandbox.ngenius-payments.com$slug"
        }
        if (stringVal.contains("-dev", true)) {
            return "https://paypage-dev.ngenius-payments.com$slug"
        }
        return return "https://paypage.ngenius-payments.com$slug"
    }

    private var progressDialog: AlertDialog? = null

    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val content: ViewGroup by lazy {
        findViewById<ViewGroup>(R.id.content)
    }
    private val progressView: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.progress)
    }

    private val stringResources: StringResources by lazy {
        StringResourcesImpl(this)
    }

    private val paymentApiInteractor = CardPaymentApiInteractor(CoroutinesGatewayHttpClient())

    private val threeDSecureWebViews = Stack<ThreeDSecureTwoWebView>()
    private var paymentCookie: String? = null
    private var threeDSAuthenticationsUrl: String? = null
    private var threeDSMethodNotificationURL: String? = null
    private var outletRef: String? = null
    private var orderRef: String? = null
    private var fingerPrintCompleted: Boolean = false
    private var threeDSTwoChallengeResponseURL: String? = null
    private var orderUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_3d_secure)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val threeDSMethodURL = intent.getStringExtra(THREE_DS_METHOD_URL)
        val threeDSServerTransID = intent.getStringExtra(THREE_DS_SERVER_TRANS_ID)
        val threeDSMethodData = intent.getStringExtra(THREE_DS_METHOD_DATA)
        threeDSMethodNotificationURL = intent.getStringExtra(THREE_DS_METHOD_NOTIFICATION_URL)
        paymentCookie = intent.getStringExtra(PAYMENT_COOKIE_KEY)
        threeDSAuthenticationsUrl = intent.getStringExtra(THREE_DS_AUTH_URL_KEY)
        outletRef = intent.getStringExtra(THREE_DS_AUTH_URL_KEY)
        orderRef = intent.getStringExtra(THREE_DS_AUTH_URL_KEY)
        threeDSTwoChallengeResponseURL = intent.getStringExtra(THREE_DS_CHALLENGE_URL_KEY)
        orderUrl = intent.getStringExtra(ORDER_URL)

        showProgress(true, stringResources.getString(AUTHENTICATING_3DS_TRANSACTION))

        val webView = ThreeDSecureTwoWebView(this)
        webView.init(this)
        val params = StringBuilder().apply {
            append("threeDSServerTransID=")
            append(URLEncoder.encode(threeDSServerTransID, "UTF-8"))
            append("&threeDSMethodNotificationURL=")
            append(URLEncoder.encode(threeDSMethodNotificationURL, "UTF-8"))
            append("&threeDSMethodData=")
            append(URLEncoder.encode(threeDSMethodData, "UTF-8"))
        }
        webView.postUrl(threeDSMethodURL, params.toString().toByteArray())
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

    private fun openChallenge(base64EncodedCReq: String, acsURL: String) {
        val webView = ThreeDSecureTwoWebView(this)
        webView.init(this)
        val params = StringBuilder().apply {
            append("creq=")
            append(URLEncoder.encode(base64EncodedCReq, "UTF-8"))
        }
        webView.postUrl(acsURL, params.toString().toByteArray())
        pushNewWebView(webView)
        showProgress(false, null)
    }

    private fun postAuthentications(browserData: BrowserData, threeDSCompInd: String) {
        paymentApiInteractor.postThreeDSTwoBrowserAuthentications(
            browserData = browserData,
            threeDSCompInd = threeDSCompInd,
            threeDSAuthenticationsUrl = threeDSAuthenticationsUrl!!,
            paymentCookie = paymentCookie!!,
            notificationUrl = threeDSMethodNotificationURL!!,
            success = { authResponse ->
                val state = authResponse.getString("state")
                val threeDSData = authResponse.getJSONObject("3ds2")
                if (state != "FAILED") {
                    val transStatus = threeDSData.getString("transStatus")
                    if (transStatus == "C") {
                    val base64EncodedCReq = threeDSData.getString("base64EncodedCReq")
                    val acsURL = threeDSData.getString("acsURL")
                        // Open Challenge
                        openChallenge(base64EncodedCReq, acsURL)
                    } else {
                        finishWithResult(state)
                    }
                } else {
                    finishWithResult(state)
                }
            },
            error = { exception ->
                Log.e("ThreeDSTwoWebActivity", "ThreeDS Authentications failed")
            }
        )
    }

    fun handleThreeDS2StageCompletion() {
        if (!fingerPrintCompleted) {
            fingerPrintCompleted = true
            onCompleteFingerPrint("Y")
        } else {
            // Challenge is completed.
            val currentWebView = threeDSecureWebViews.last()
            currentWebView.visibility = View.GONE
            showProgress(true, stringResources.getString(AUTHENTICATING_3DS_TRANSACTION))
            paymentApiInteractor.postThreeDSTwoChallengeResponse(
                threeDSTwoChallengeResponseURL = threeDSTwoChallengeResponseURL!!,
                paymentCookie = paymentCookie!!,
                success = { state, response ->
                    paymentApiInteractor.getOrder(
                        orderUrl = orderUrl!!,
                        paymentCookie = paymentCookie!!,
                        success = { _, _, _, _, _, order ->
                            val orderState: String? = order
                                .getJSONObject("_embedded")
                                ?.getJSONArray("payment")
                                ?.getJSONObject(0)
                                ?.getString("state")
                            finishWithResult(orderState)
                        },
                        error = {
                            finishWithResult()
                        }
                    )
                },
                error = {
                    finishWithResult()
                }
            )
        }
    }

    fun onCompleteFingerPrint(threeDSCompInd: String) {
        val currentWebView = threeDSecureWebViews.last()
        currentWebView.visibility = View.GONE
        val browserDataJS = "browserLanguage: window.navigator.language," +
                "browserJavaEnabled: window.navigator.javaEnabled ? window.navigator.javaEnabled() : false," +
                "browserColorDepth: window.screen.colorDepth.toString()," +
                "browserScreenHeight: window.screen.height.toString()," +
                "browserScreenWidth: window.screen.width.toString()," +
                "browserTZ: new Date().getTimezoneOffset().toString()," +
                "browserUserAgent: window.navigator.userAgent"
        currentWebView.evaluateJavascript("(function(){ return ({ $browserDataJS }); })()") { browserDataJson ->
            val browserData = Gson().fromJson(browserDataJson, BrowserData::class.java)
            browserData.browserAcceptHeader = "application/json, text/plain, */*"
            browserData.browserJavascriptEnabled = true
            browserData.challengeWindowSize = "05"
            if (paymentCookie == null) {
                Log.e(
                    "ThreeDSTwoWebActivity",
                    "ThreeDS Authentications failed due to missing payment cookie"
                )
                finishWithResult()
            } else if (threeDSAuthenticationsUrl == null) {
                Log.e(
                    "ThreeDSTwoWebActivity",
                    "ThreeDS Authentications failed due to missing authenticationsurl"
                )
                finishWithResult()
            } else if (threeDSMethodNotificationURL == null) {
                Log.e(
                    "ThreeDSTwoWebActivity",
                    "ThreeDS Authentications failed due to missing method notification url"
                )
                finishWithResult()
            } else {
                paymentApiInteractor.getPayerIP(
                    requestIpUrl = getIpUrl(threeDSAuthenticationsUrl!!, outletRef!!, orderRef!!),
                    paymentCookie = paymentCookie!!,
                    success = { ipResponse ->
                        browserData.browserIP = ipResponse.getString("requesterIp")
                        postAuthentications(browserData, threeDSCompInd);
                    },
                    error = { exception ->
                        browserData.browserIP = "192.168.1.1"
                        postAuthentications(browserData, threeDSCompInd);
                        Log.e(
                            "ThreeDSTwoWebActivity",
                            "Unable to obtain IP Address. Going with default IP"
                        )
                    }
                )
            }
        }
    }

    fun finishWithResult(state: String? = null) {
        state?.let {
            val intent = Intent().apply {
                putExtra(KEY_3DS_STATE, it)
            }
            setResult(Activity.RESULT_OK, intent)
        }
        showProgress(false, null)
        finish()
    }

    override fun onBackPressed() {
        when {
            threeDSecureWebViews.peek().canGoBack() -> threeDSecureWebViews.peek().goBack()
            threeDSecureWebViews.size > 1 -> popCurrentWebView()
            else -> super.onBackPressed()
        }
    }

    fun setWebViewToolbarTitle(title: Int) {
        toolbar.setTitle(title)
    }

    fun showProgress(show: Boolean, text: String?) {
        progressDialog = if (show) {
            progressDialog?.dismiss()
            AlertDialog.Builder(this, R.style.OpaqueDialogTheme)
                .setTitle(null)
                .setCancelable(false)
                .create().apply {
                    show()
                    setContentView(R.layout.view_progress_dialog)
                    findViewById<TextView>(R.id.text).text = text
                }
        } else {
            progressDialog?.dismiss()
            null
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

        internal const val THREE_DS_METHOD_URL = "threeDSMethodURL"
        internal const val THREE_DS_SERVER_TRANS_ID = "threeDSServerTransID"
        internal const val THREE_DS_METHOD_DATA = "threeDSMethodData"
        internal const val THREE_DS_METHOD_NOTIFICATION_URL = "threeDSMethodNotificationURL"
        internal const val PAYMENT_COOKIE_KEY = "paymentCookie"
        internal const val THREE_DS_AUTH_URL_KEY = "threeDSTwoAuthenticationURL"
        internal const val THREE_DS_CHALLENGE_URL_KEY = "threeDSTwoChallengeResponseURL"
        internal const val DIRECTORY_SERVER_ID_KEY = "directoryServerID"
        internal const val THREE_DS_MESSAGE_VERSION_KEY = "threeDSMessageVersion"
        internal const val OUTLET_REF = "outletRef"
        internal const val ORDER_REF = "orderRef"
        internal const val ORDER_URL = "orderUrl"

        private val AUTHENTICATING_3DS_TRANSACTION: Int = R.string.authenticating_three_ds_two

        fun getIntent(
            context: Context,
            threeDSMethodURL: String,
            threeDSServerTransID: String,
            threeDSMethodData: String,
            threeDSMethodNotificationURL: String,
            paymentCookie: String,
            threeDSAuthenticationsUrl: String,
            directoryServerID: String,
            threeDSMessageVersion: String,
            threeDSTwoChallengeResponseURL: String,
            outletRef: String,
            orderRef: String,
            orderUrl: String
        ) =
            Intent(context, ThreeDSecureTwoWebViewActivity::class.java).apply {
                putExtra(THREE_DS_METHOD_URL, threeDSMethodURL)
                putExtra(THREE_DS_SERVER_TRANS_ID, threeDSServerTransID)
                putExtra(THREE_DS_METHOD_DATA, threeDSMethodData)
                putExtra(PAYMENT_COOKIE_KEY, paymentCookie)
                putExtra(THREE_DS_METHOD_NOTIFICATION_URL, threeDSMethodNotificationURL)
                putExtra(THREE_DS_AUTH_URL_KEY, threeDSAuthenticationsUrl)
                putExtra(DIRECTORY_SERVER_ID_KEY, directoryServerID)
                putExtra(THREE_DS_MESSAGE_VERSION_KEY, threeDSMessageVersion)
                putExtra(THREE_DS_CHALLENGE_URL_KEY, threeDSTwoChallengeResponseURL)
                putExtra(OUTLET_REF, outletRef)
                putExtra(ORDER_REF, orderRef)
                putExtra(ORDER_URL, orderUrl)
            }
    }
}
