package payment.sdk.android.cardpayment.savedCard

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
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
import payment.sdk.android.SDKConfig
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.savedCard.view.SavedCardPaymentView
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.sdk.R

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
                    cvv = (state as SavedCardPaymentState.Authorized).cvv,
                    orderUrl = (state as SavedCardPaymentState.Authorized).orderUrl,
                    paymentCookie = (state as SavedCardPaymentState.Authorized).paymentCookie,
                    payPageUrl = args.paymentUrl
                )

                is SavedCardPaymentState.Failed -> finishWithData(
                    CardPaymentData(
                        CardPaymentData.STATUS_PAYMENT_FAILED
                    )
                )

                SavedCardPaymentState.Init -> viewModel.authorize(
                    args.authUrl,
                    args.paymentUrl,
                    args.savedCard.recaptureCsc,
                    args.cvv
                )

                is SavedCardPaymentState.CaptureCvv -> {
                    SavedCardPaymentView(
                        savedCard = args.savedCard,
                        amount = args.amount,
                        currency = args.currency,
                        onStartPayment = { cvv ->
                            viewModel.doSavedCardPayment(
                                accessToken = (state as SavedCardPaymentState.CaptureCvv).accessToken,
                                savedCardUrl = args.savedCardUrl,
                                savedCard = args.savedCard,
                                cvv = cvv,
                                orderUrl = (state as SavedCardPaymentState.CaptureCvv).orderUrl,
                                paymentCookie = (state as SavedCardPaymentState.CaptureCvv).paymentCookie,
                                payPageUrl = args.paymentUrl
                            )
                        }, onNavigationUp = {
                            if (SDKConfig.showCancelAlert) {
                                showDialog()
                            } else {
                                setResult(RESULT_CANCELED, Intent())
                                finish()
                            }
                        })
                }

                is SavedCardPaymentState.InitiateThreeDS -> {
                    val response = (state as SavedCardPaymentState.InitiateThreeDS).threeDSecureDto
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
                    val response = (state as SavedCardPaymentState.InitiateThreeDSTwo).threeDSecureTwoDto
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
                SavedCardPaymentState.Captured -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED))
                SavedCardPaymentState.PaymentAuthorised -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED))
                SavedCardPaymentState.PostAuthReview -> finishWithData(CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW))
                SavedCardPaymentState.Purchased -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_PURCHASED))
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
                    "POST_AUTH_REVIEW" -> finishWithData(CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW))
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
                if (SDKConfig.showCancelAlert) {
                    showDialog()
                } else {
                    setResult(Activity.RESULT_CANCELED, Intent())
                    finish()
                }
            }
        })
    }

    private fun showDialog() {
        with(AlertDialog.Builder(this)) {
            setMessage(R.string.cancel_payment_alert_message)
            setTitle(R.string.cancel_payment_alert_title)
            setCancelable(false)
            setPositiveButton(R.string.confirm_cancel_alert) { _: DialogInterface?, _: Int ->
                setResult(RESULT_CANCELED, intent)
                finish()
            }
            setNegativeButton(R.string.cancel_alert ) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            show()
        }
    }

    companion object {
        const val THREE_D_SECURE_REQUEST_KEY = 504
        const val THREE_D_SECURE_TWO_REQUEST_KEY = 503
    }
}