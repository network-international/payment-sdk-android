package payment.sdk.android.cardpayment.savedCard

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.savedCard.view.SavedCardPaymentView
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.cardpayment.widget.CircularProgressDialog

class SavedCardPaymentActivity : ComponentActivity() {

    private val inputArgs: SavedCardActivityArgs? by lazy {
        SavedCardActivityArgs.fromIntent(intent = intent)
    }

    private val viewModel: SavedPaymentViewModel by viewModels { SavedPaymentViewModel.Factory }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnBackPressed()
        val args = runCatching {
            requireNotNull(inputArgs) {
                "SavedCardPaymentActivity input arguments were not found"
            }
        }.getOrElse {
            finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
            return
        }

        setContent {
            val state by viewModel.state.collectAsState()

            when (state) {
                is SavedCardPaymentState.Authorized -> viewModel.doSavedCardPayment(
                    accessToken = (state as SavedCardPaymentState.Authorized).accessToken,
                    savedCardUrl = args.savedCardUrl,
                    savedCard = args.savedCard,
                    cvv = null,
                    orderUrl = (state as SavedCardPaymentState.Authorized).orderUrl,
                    paymentCookie = (state as SavedCardPaymentState.Authorized).paymentCookie
                )

                is SavedCardPaymentState.Failed -> finishWithData(
                    CardPaymentData(
                        CardPaymentData.STATUS_PAYMENT_FAILED
                    )
                )

                SavedCardPaymentState.Init -> viewModel.authorize(
                    args.authUrl,
                    args.paymentUrl,
                    args.savedCard.recaptureCsc
                )

                is SavedCardPaymentState.CaptureCvv -> {
                    SavedCardPaymentView(args.savedCard, args.amount, args.currency) { cvv ->
                        viewModel.doSavedCardPayment(
                            accessToken = (state as SavedCardPaymentState.CaptureCvv).accessToken,
                            savedCardUrl = args.savedCardUrl,
                            savedCard = args.savedCard,
                            cvv = cvv,
                            orderUrl = (state as SavedCardPaymentState.CaptureCvv).orderUrl,
                            paymentCookie = (state as SavedCardPaymentState.CaptureCvv).paymentCookie
                        )
                    }
                }

                is SavedCardPaymentState.InitiateThreeDS -> {
                    val response = state as SavedCardPaymentState.InitiateThreeDS
                    startActivityForResult(
                        ThreeDSecureWebViewActivity.getIntent(
                            context = this,
                            acsUrl = response.acsUrl,
                            acsPaReq = response.acsPaReq,
                            acsMd = response.acsMd,
                            gatewayUrl = response.threeDSOneUrl
                        ),
                        THREE_D_SECURE_REQUEST_KEY
                    )
                }

                is SavedCardPaymentState.InitiateThreeDSTwo -> {
                    val response = state as SavedCardPaymentState.InitiateThreeDSTwo
                    startActivityForResult(
                        ThreeDSecureTwoWebViewActivity.getIntent(
                            context = this,
                            threeDSMethodData = response.threeDSMethodData,
                            threeDSMethodNotificationURL = response.threeDSMethodNotificationURL,
                            threeDSMethodURL = response.threeDSMethodURL,
                            threeDSServerTransID = response.threeDSServerTransID,
                            paymentCookie = response.paymentCookie,
                            threeDSAuthenticationsUrl = response.threeDSTwoAuthenticationURL,
                            directoryServerID = response.directoryServerID,
                            threeDSMessageVersion = response.threeDSMessageVersion,
                            threeDSTwoChallengeResponseURL = response.threeDSTwoChallengeResponseURL,
                            outletRef = response.outletRef,
                            orderRef = response.orderRef,
                            orderUrl = response.orderUrl,
                            paymentRef = response.paymentReference
                        ),
                        THREE_D_SECURE_TWO_REQUEST_KEY
                    )
                }

                is SavedCardPaymentState.Loading -> CircularProgressDialog((state as SavedCardPaymentState.Loading).message)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == THREE_D_SECURE_REQUEST_KEY ||
            requestCode == THREE_D_SECURE_TWO_REQUEST_KEY
        ) {
            if (resultCode == RESULT_OK && data != null) {
                val state = data.getStringExtra(
                    ThreeDSecureWebViewActivity.KEY_3DS_STATE
                )!!
                when (state) {
                    "AUTHORISED" -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED))
                    "PURCHASED" -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_PURCHASED))
                    "CAPTURED" -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED))
                    "FAILED" -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
                    else -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
                }
            } else {
                finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
            }
        }
    }

    private fun finishWithData(cardPaymentData: CardPaymentData) {
        val intent = Intent().apply {
            putExtra(CardPaymentData.INTENT_DATA_KEY, cardPaymentData)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun setOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
        })
    }

    companion object {
        const val THREE_D_SECURE_REQUEST_KEY = 504
        const val THREE_D_SECURE_TWO_REQUEST_KEY = 503
    }
}