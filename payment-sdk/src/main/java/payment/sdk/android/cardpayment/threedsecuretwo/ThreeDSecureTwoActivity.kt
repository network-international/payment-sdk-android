package payment.sdk.android.cardpayment.threedsecuretwo

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.usdk.android.UsdkThreeDS2ServiceImpl
import org.emvco.threeds.core.*
import org.emvco.threeds.core.ui.UiCustomization
import org.json.JSONException
import org.json.JSONObject
import payment.sdk.android.cardpayment.CardPaymentApiInteractor
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.sdk.R
import java.lang.Exception

enum class NGeniusEnvironments {
    UAT,
    DEV,
    PROD,
}


class ThreeDSecureTwoActivity : AppCompatActivity() {

    private var progressDialog: AlertDialog? = null

    private fun getEnv(stringVal: String): NGeniusEnvironments {
        if(stringVal.contains("-uat", true) ||
            stringVal.contains("sandbox", true)) {
            return NGeniusEnvironments.UAT
        }
        if(stringVal.contains("-dev", true)) {
            return NGeniusEnvironments.DEV
        }
        return NGeniusEnvironments.PROD
    }

    private fun getConfigParam(threeDSAuthenticationsUrl: String) : ConfigParameters {

        val directoryServer = DirectoryServer(MTFDirectoryServers.MC_MTF_DIRECTORY_SERVER_ID,
            MTFDirectoryServers.MC_MTF_DIRECTORY_SERVER_KEY_ID,
            MTFDirectoryServers.MC_MTF_DIRECTORY_SERVER_PUBLIC_KEY,
            MTFDirectoryServers.MC_MTF_DIRECTORY_SERVER_CERT,
            MTFDirectoryServers.MC_DIRECTORY_SERVER_PROVIDER_NAME)

        val configParameters = ConfigParameters()

        if(getEnv(threeDSAuthenticationsUrl) == NGeniusEnvironments.PROD) {
            return configParameters
        }
        configParameters.addDirectoryServer(directoryServer)
        return configParameters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_three_dsecure_two)
        showProgress(true)

        val service: ThreeDS2Service = UsdkThreeDS2ServiceImpl();

        val paymentApiInteractor = CardPaymentApiInteractor(CoroutinesGatewayHttpClient())
        val directoryServerID = intent.getStringExtra(DIRECTORY_SERVER_ID_KEY)
        val messageVersion = intent.getStringExtra(THREE_DS_MESSAGE_VERSION_KEY)
        val threeDSAuthenticationsUrl = intent.getStringExtra(THREE_DS_AUTH_URL_KEY)
        val threeDSTwoChallengeResponseURL = intent.getStringExtra(THREE_DS_CHALLENGE_URL_KEY)
        val paymentCookie = intent.getStringExtra(PAYMENT_COOKIE_KEY)

        // Prepare ConfigParameters
        val configParams = getConfigParam(threeDSAuthenticationsUrl);

        val listener: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
                if (!intent.getBooleanExtra(
                        UsdkThreeDS2ServiceImpl.INITIALIZATION_ACTION_EXTRA_SUCCESS,
                        false
                    )
                ) {
                    showProgress(false)
                    throw RuntimeException(
                        "Failed to initialize SDK" +
                                ", code: " + intent.getStringExtra(UsdkThreeDS2ServiceImpl.INITIALIZATION_ACTION_EXTRA_ERROR_CODE) +
                                ", type: " + intent.getStringExtra(UsdkThreeDS2ServiceImpl.INITIALIZATION_ACTION_EXTRA_ERROR_TYPE)
                    )
                }
                // TODO: dynamic for production
                var directoryServerId = "SANDBOX_DS"
                if (getEnv(threeDSAuthenticationsUrl) == NGeniusEnvironments.PROD) {
                    directoryServerId = directoryServerID
                }
                val transaction: Transaction =
                    service.createTransaction(directoryServerId, messageVersion)
                val authenticationRequestParameters = transaction.authenticationRequestParameters
                val sdkEphemPubKey = Gson().fromJson(authenticationRequestParameters.sdkEphemeralPublicKey,
                    SDKEphemPubKey::class.java)


                paymentApiInteractor.postThreeDSTwoAuthentications(
                    sdkAppID = authenticationRequestParameters.sdkAppID,
                    sdkEncData = authenticationRequestParameters.deviceData,
                    sdkEphemPubKey = sdkEphemPubKey,
                    sdkMaxTimeout = 10,
                    sdkReferenceNumber = authenticationRequestParameters.sdkReferenceNumber,
                    sdkTransID = authenticationRequestParameters.sdkTransactionID,
                    deviceRenderOptions = DeviceRenderOptions("03", arrayOf("01", "02", "03", "05")),
                    threeDSAuthenticationsUrl = threeDSAuthenticationsUrl,
                    paymentCookie = paymentCookie,
                    success = { _: String, authenticationsResp: JSONObject ->
                        handleOnFrictionlessResponse(
                            authenticationsResp,
                            transaction,
                            paymentApiInteractor,
                            threeDSTwoChallengeResponseURL,
                            paymentCookie
                        )
                    },
                    error = {
                            exception ->
                        error(exception)
                        println(exception)
                        showProgress(false)
                    }
                )
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            listener,
            IntentFilter(UsdkThreeDS2ServiceImpl.INTENT_INITIALIZATION_ACTION)
        )

        // Initialize the SDK
        service.initialize(this, configParams, "en_US", UiCustomization());
    }

    fun showProgress(show: Boolean) {
        progressDialog = if (show) {
            progressDialog?.dismiss()
            AlertDialog.Builder(this)
                .setTitle(null)
                .setCancelable(false)
                .create().apply {
                    show()
                    setContentView(R.layout.view_progress_dialog)
                    findViewById<TextView>(R.id.text).setText(R.string.message_authorizing)
                }
        } else {
            progressDialog?.dismiss()
            null
        }
    }

    internal fun handleOnFrictionlessResponse(
        authenticationsResp: JSONObject,
        transaction: Transaction,
        paymentApiInteractor: CardPaymentApiInteractor,
        threeDSTwoChallengeResponseURL: String,
        paymentCookie: String
    ) {
        try {
            val state = authenticationsResp.getString("state")
            if(state == "FAILED") {
                // Abort journey
                finishWithResult(state)
                return
            }
            val threeDSConfigJson = authenticationsResp.getJSONObject("3ds2")
            val threeDSTwoConfig = Gson().fromJson(threeDSConfigJson.toString(), ThreeDSTwoConfig::class.java)
            val transStatus = threeDSTwoConfig.transStatus
            if (transStatus == "C") {
                openChallengeScreen(
                    transaction, threeDSTwoConfig,
                    paymentApiInteractor,
                    threeDSTwoChallengeResponseURL,
                    paymentCookie
                )
            } else if (transStatus == "Y") {
                finishWithResult(state)
            }
        } catch (e: JSONException) {
            // Abort journey
            finishWithResult()
        }
    }

    internal fun openChallengeScreen(
        transaction: Transaction,
        threeDSTwoConfig: ThreeDSTwoConfig,
        paymentApiInteractor: CardPaymentApiInteractor,
        threeDSTwoChallengeResponseURL: String,
        paymentCookie: String
    ) {
        val challengeParameters = ChallengeParameters()
        challengeParameters.acsRefNumber = threeDSTwoConfig.acsReferenceNumber
        challengeParameters.set3DSServerTransactionID(threeDSTwoConfig.threeDSServerTransID)
        challengeParameters.acsSignedContent = threeDSTwoConfig.acsSignedContent
        challengeParameters.acsTransactionID = threeDSTwoConfig.acsTransID
        try {
            transaction.doChallenge(this, challengeParameters, object : ChallengeStatusReceiver {
                override fun cancelled() {
                    finishWithResult("USER_CANCELLED")
                }

                override fun protocolError(p0: ProtocolErrorEvent?) {
                    finishWithResult("SDK_PROTOCOL_ERROR")
                }

                override fun runtimeError(p0: RuntimeErrorEvent?) {
                    finishWithResult("SDK_RUNTIME_ERROR")
                }

                override fun completed(p0: CompletionEvent?) {
                    println("Challenge has completed with status Y")
                    paymentApiInteractor.postThreeDSTwoChallengeResponse(
                        threeDSTwoChallengeResponseURL = threeDSTwoChallengeResponseURL,
                        paymentCookie = paymentCookie,
                        success = { state: String, authenticationsResp: JSONObject ->
                            println(authenticationsResp.toString())
                            finishWithResult(state)
                        },
                        error = {
                                exception ->
                            error(exception)
                            println(exception)
                            showProgress(false)
                        }

                    )
                }

                override fun timedout() {
                    finishWithResult("SDK_TIMED_OUT")
                }
            }, 30)
        } catch (e: Exception) {
            // catch block here
            finishWithResult("SDK_UNKNOWN_EXCEPTION")
        }
    }

    private fun finishWithResult(state: String? = "FAILED") {
        showProgress(false)
        state?.let {
            val intent = Intent().apply {
                putExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE, it)
            }
            setResult(Activity.RESULT_OK, intent)
        }
        finish()
    }

    companion object {
        internal const val DIRECTORY_SERVER_ID_KEY = "directoryServerID"
        internal const val THREE_DS_MESSAGE_VERSION_KEY = "threeDSMessageVersion"
        internal const val PAYMENT_COOKIE_KEY = "paymentCookie"
        internal const val THREE_DS_AUTH_URL_KEY = "threeDSTwoAuthenticationURL"
        internal const val THREE_DS_CHALLENGE_URL_KEY = "threeDSTwoChallengeResponseURL"
        fun getIntent(context: Context, directoryServerID: String, threeDSMessageVersion: String,
                      paymentCookie: String, threeDSTwoAuthenticationURL:String,
                      threeDSTwoChallengeResponseURL: String) =
            Intent(context, ThreeDSecureTwoActivity::class.java).apply {
                putExtra(DIRECTORY_SERVER_ID_KEY, directoryServerID)
                putExtra(THREE_DS_MESSAGE_VERSION_KEY, threeDSMessageVersion)
                putExtra(PAYMENT_COOKIE_KEY, paymentCookie)
                putExtra(THREE_DS_AUTH_URL_KEY, threeDSTwoAuthenticationURL)
                putExtra(THREE_DS_CHALLENGE_URL_KEY, threeDSTwoChallengeResponseURL)
            }
    }
}