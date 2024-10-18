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
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstallmentsVMState
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstalmentActivityArgs
import payment.sdk.android.cardpayment.visaInstalments.view.VisaInstalmentsView
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.sdk.R

class VisaInstallmentsActivity : ComponentActivity() {
    private val inputArgs: VisaInstalmentActivityArgs? by lazy {
        VisaInstalmentActivityArgs.fromIntent(intent = intent)
    }

    private val viewModel: VisaInstallmentsViewModel by viewModels { VisaInstallmentsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnBackPressed()
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
                is VisaInstallmentsVMState.Loading -> CircularProgressDialog((state as VisaInstallmentsVMState.Loading).message)
                VisaInstallmentsVMState.Init -> {
                    viewModel.init(args = args)
                }

                is VisaInstallmentsVMState.PlanSelection -> {
                    VisaInstalmentsView(
                        state = (state as VisaInstallmentsVMState.PlanSelection),
                        onNavigationUp = {
                            if (SDKConfig.showCancelAlert) {
                                showDialog()
                            } else {
                                setResult(RESULT_CANCELED, Intent())
                                finish()
                            }
                        },
                        onSelectPlan = {
                            viewModel.onSelectPlan(
                                selectedPlan = it,
                                state = (state as VisaInstallmentsVMState.PlanSelection)
                            )
                        },
                        onPayClicked = {
                            viewModel.makeCardPayment(
                                plan = it,
                                state = (state as VisaInstallmentsVMState.PlanSelection),
                                payPageUrl = args.payPageUrl,
                                cvv = args.cvv
                            )
                        }
                    )
                }

                is VisaInstallmentsVMState.InitiateThreeDS -> {
                    val response = (state as VisaInstallmentsVMState.InitiateThreeDS).threeDSecureDto
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

                is VisaInstallmentsVMState.InitiateThreeDSTwo -> {
                    val response =
                        (state as VisaInstallmentsVMState.InitiateThreeDSTwo).threeDSecureTwoDto
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

                VisaInstallmentsVMState.Captured -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED))
                VisaInstallmentsVMState.PaymentAuthorised -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED))
                VisaInstallmentsVMState.PostAuthReview -> finishWithData(CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW))
                VisaInstallmentsVMState.Purchased -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_PURCHASED))
                is VisaInstallmentsVMState.Failed -> finishWithData(
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
                    "FAILED" -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED, reason = "Failed 3DS"))
                    "POST_AUTH_REVIEW" -> finishWithData(CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW))
                    else -> finishWithData(CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED, reason = "Failed 3DS"))
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