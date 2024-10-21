package payment.sdk.android.cardpayment.savedCard

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import payment.sdk.android.SDKConfig
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.partialAuth.model.PartialAuthActivityArgs
import payment.sdk.android.cardpayment.savedCard.view.SavedCardPaymentView
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity.Companion.INTENT_CHALLENGE_RESPONSE
import payment.sdk.android.cardpayment.visaInstalments.model.InstallmentPlan
import payment.sdk.android.cardpayment.visaInstalments.view.VisaInstalmentsView
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.sdk.R

class SavedCardPaymentActivity : ComponentActivity() {

    private val inputArgs: SavedCardActivityArgs? by lazy {
        SavedCardActivityArgs.fromIntent(intent = intent)
    }

    private val partialAuthActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            finishWithData(CardPaymentData.getFromIntent(result.data!!))
        } else {
            finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
        }
    }

    private val viewModel: SavedPaymentViewModel by viewModels { SavedPaymentViewModel.Factory }

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
                    payPageUrl = args.paymentUrl,
                    orderAmount = OrderAmount(args.amount, args.currency)
                )

                is SavedCardPaymentState.Failed ->
                    finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))

                SavedCardPaymentState.Init -> viewModel.authorize(
                    authUrl = args.authUrl,
                    paymentUrl = args.paymentUrl,
                    recaptureCsc = args.savedCard.recaptureCsc,
                    cvv = args.cvv,
                    cardToken = args.savedCard.cardToken,
                    selfLink = args.selfUrl,
                    matchedCandidates = args.matchedCandidates,
                    savedCard = args.savedCard,
                    savedCardUrl = args.savedCardUrl,
                    orderAmount = OrderAmount(args.amount, args.currency)
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
                                payPageUrl = args.paymentUrl,
                                visaPlans = (state as SavedCardPaymentState.CaptureCvv).visaPlans,
                                orderAmount = OrderAmount(args.amount, args.currency)
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
                    val response =
                        (state as SavedCardPaymentState.InitiateThreeDSTwo).threeDSecureTwoDto
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
                SavedCardPaymentState.PaymentAuthorised ->
                    finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED))

                SavedCardPaymentState.PostAuthReview ->
                    finishWithData(CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW))

                SavedCardPaymentState.Purchased -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_PURCHASED))
                is SavedCardPaymentState.ShowVisaPlans -> {
                    val response = (state as SavedCardPaymentState.ShowVisaPlans)
                    VisaInstalmentsView(
                        instalmentPlans = InstallmentPlan.fromVisaPlans(response.visaPlans, response.orderAmount),
                        cardNumber = response.cardNumber
                    ) { plan ->
                        viewModel.initiateVisPayment(
                            plan,
                            response.savedCardPaymentRequest,
                            response.orderUrl,
                            response.paymentCookie
                        )
                    }
                }

                is SavedCardPaymentState.InitiatePartialAuth -> {
                    startPartialAuthActivity((state as SavedCardPaymentState.InitiatePartialAuth).partialAuthIntent)
                }
            }
        }
    }

    private fun startPartialAuthActivity(partialAuthIntent: PartialAuthIntent) {
        try {
            partialAuthActivityLauncher.launch(
                PartialAuthActivityArgs.getArgs(partialAuthIntent).toIntent(this)
            )
        } catch (e: IllegalArgumentException) {
            finishWithData(CardPaymentData(CardPaymentData.STATUS_GENERIC_ERROR, e.message))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_CANCELED) {
            setResult(RESULT_CANCELED, Intent())
            finish()
            return
        }
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
                    "AWAITING_PARTIAL_AUTH_APPROVAL" -> {
                        (data.getParcelableExtra(INTENT_CHALLENGE_RESPONSE) as? PartialAuthIntent)?.let { intent ->
                            startPartialAuthActivity(intent)
                        }
                    }

                    else -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
                }
            } else {
                finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
            }
        }
        if (requestCode == VISA_INSTALMENT_SELECTION_KEY) {
            if (resultCode == RESULT_OK && data != null) {
                finishWithData(CardPaymentData.getFromIntent(data))
            } else {
                finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
            }
        }
    }

    private fun finishWithData(cardPaymentData: CardPaymentData) {
        val intent = Intent().apply {
            putExtra(CardPaymentData.INTENT_DATA_KEY, cardPaymentData)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (SDKConfig.showCancelAlert) {
                    showDialog()
                } else {
                    setResult(RESULT_CANCELED, Intent())
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
            setNegativeButton(R.string.cancel_alert) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            show()
        }
    }

    companion object {
        const val THREE_D_SECURE_REQUEST_KEY = 504
        const val THREE_D_SECURE_TWO_REQUEST_KEY = 503
        const val VISA_INSTALMENT_SELECTION_KEY = 505
    }
}