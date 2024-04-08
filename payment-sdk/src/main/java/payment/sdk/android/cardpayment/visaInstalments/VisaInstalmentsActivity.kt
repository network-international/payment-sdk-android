package payment.sdk.android.cardpayment.visaInstalments

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import payment.sdk.android.SDKConfig
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstalmentActivityArgs
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstalmentsVMState
import payment.sdk.android.cardpayment.visaInstalments.view.VisaInstalmentsView
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.sdk.R

class VisaInstalmentsActivity : ComponentActivity() {
    private val inputArgs: VisaInstalmentActivityArgs? by lazy {
        VisaInstalmentActivityArgs.fromIntent(intent = intent)
    }

    private val viewModel: VisaInstalmentsViewModel by viewModels { VisaInstalmentsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = runCatching {
            requireNotNull(inputArgs) {
                "VisaInstalmentsActivity input arguments were not found"
            }
        }.getOrElse {
            finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED))
            return
        }

        setContent {
            val state by viewModel.state.collectAsState()

            when (state) {
                is VisaInstalmentsVMState.Loading -> CircularProgressDialog((state as VisaInstalmentsVMState.Loading).message)
                VisaInstalmentsVMState.Init -> {
                    viewModel.init(args = args)
                }

                is VisaInstalmentsVMState.PlanSelection -> {
                    VisaInstalmentsView(
                        state = (state as VisaInstalmentsVMState.PlanSelection),
                        onNavigationUp = {},
                        onSelectPlan = {
                            viewModel.onSelectPlan(
                                selectedPlan = it,
                                state = (state as VisaInstalmentsVMState.PlanSelection)
                            )
                        },
                        onPayClicked = {
                            viewModel.makeCardPayment(
                                plan = it,
                                state = (state as VisaInstalmentsVMState.PlanSelection),
                                payPageUrl = args.payPageUrl
                            )
                        }
                    )
                }

                is VisaInstalmentsVMState.InitiateThreeDS -> {
                    val response = (state as VisaInstalmentsVMState.InitiateThreeDS).threeDSecureDto
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

                is VisaInstalmentsVMState.InitiateThreeDSTwo -> {
                    val response =
                        (state as VisaInstalmentsVMState.InitiateThreeDSTwo).threeDSecureTwoDto
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

                VisaInstalmentsVMState.Captured -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED))
                VisaInstalmentsVMState.PaymentAuthorised -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED))
                VisaInstalmentsVMState.PostAuthReview -> finishWithData(CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW))
                VisaInstalmentsVMState.Purchased -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_PURCHASED))
                is VisaInstalmentsVMState.Failed -> finishWithData(
                    CardPaymentData(
                        CardPaymentData.STATUS_PAYMENT_FAILED
                    )
                )
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
            setNegativeButton(R.string.cancel_alert) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            show()
        }
    }

    private fun finishWithData(cardPaymentData: CardPaymentData) {
        val intent = Intent().apply {
            putExtra(CardPaymentData.INTENT_DATA_KEY, cardPaymentData)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        const val THREE_D_SECURE_REQUEST_KEY = 504
        const val THREE_D_SECURE_TWO_REQUEST_KEY = 503
    }
}