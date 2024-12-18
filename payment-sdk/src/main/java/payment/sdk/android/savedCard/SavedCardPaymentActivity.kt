package payment.sdk.android.savedCard

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import payment.sdk.android.SDKConfig
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity.Companion.INTENT_CHALLENGE_RESPONSE
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.partialAuth.model.PartialAuthActivityArgs
import payment.sdk.android.partialAuth.view.PartialAuthView
import payment.sdk.android.payments.PaymentsResult
import payment.sdk.android.savedCard.model.SavedCardPaymentState
import payment.sdk.android.savedCard.model.SavedCardPaymentsVMEffects
import payment.sdk.android.savedCard.view.SavedCardPaymentView
import payment.sdk.android.sdk.R
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.view.VisaInstalmentsView

class SavedCardPaymentActivity : ComponentActivity() {

    private val viewModel: SavedPaymentViewModel by viewModels { SavedPaymentViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnBackPressed()
        val inputArgs = runCatching {
            requireNotNull(SavedCardPaymentRequest.fromIntent(intent)) {
                "SavedCardPaymentActivity input arguments were not found"
            }
        }.getOrElse {
            finishWithData(
                CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED),
                PaymentsResult.Failed(it.message.orEmpty())
            )
            return
        }

        initEffects()

        setContent {
            val state by viewModel.state.collectAsState()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.make_payment),
                                color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
                            )
                        },
                        backgroundColor = colorResource(id = R.color.payment_sdk_toolbar_color),
                        navigationIcon = {
                            IconButton(onClick = {
                                if (SDKConfig.showCancelAlert) {
                                    showDialog()
                                } else {
                                    setResult(RESULT_CANCELED, Intent())
                                    finish()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    tint = colorResource(id = R.color.payment_sdk_toolbar_icon_color),
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                },
            ) { contentPadding ->
                when (state) {
                    is SavedCardPaymentState.CaptureCvv -> {
                        val capturedState = (state as SavedCardPaymentState.CaptureCvv)
                        SavedCardPaymentView(
                            modifier = Modifier.padding(contentPadding),
                            savedCard = capturedState.savedCardPaymentRequest.savedCard,
                            orderAmount = capturedState.orderAmount
                        ) { cvv ->
                            viewModel.doSavedCardPayment(
                                orderUrl = (state as SavedCardPaymentState.CaptureCvv).orderUrl,
                                paymentCookie = (state as SavedCardPaymentState.CaptureCvv).paymentCookie,
                                visaPlans = (state as SavedCardPaymentState.CaptureCvv).visaPlans,
                                orderAmount = capturedState.orderAmount,
                                savedCardPaymentRequest = capturedState.savedCardPaymentRequest.copy(
                                    cvv = cvv
                                )
                            )
                        }
                    }

                    SavedCardPaymentState.Init -> viewModel.authorize(
                        payPageUrl = inputArgs.paymentUrl,
                        authorizationUrl = inputArgs.authorizationUrl,
                        cvv = inputArgs.cvv
                    )

                    is SavedCardPaymentState.InitiatePartialAuth -> {
                        val partialAuthState = (state as SavedCardPaymentState.InitiatePartialAuth)
                        PartialAuthView(
                            args = PartialAuthActivityArgs.getArgs(
                                partialAuthState.partialAuthIntent
                            )
                        ) { result ->
                            finishWithData(result, result.getCardPaymentsState())
                        }
                    }

                    is SavedCardPaymentState.Loading -> CircularProgressDialog((state as SavedCardPaymentState.Loading).message)
                    is SavedCardPaymentState.ShowVisaPlans -> {
                        val response = (state as SavedCardPaymentState.ShowVisaPlans)
                        VisaInstalmentsView(
                            instalmentPlans = InstallmentPlan.fromVisaPlans(
                                response.visaPlans,
                                response.orderAmount
                            ),
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
                }
            }
        }
    }

    private fun initEffects() {
        lifecycleScope.launch {
            viewModel.effect.collectLatest {
                when (it) {
                    SavedCardPaymentsVMEffects.Captured -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED),
                        PaymentsResult.Success
                    )

                    is SavedCardPaymentsVMEffects.Failed -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED),
                        PaymentsResult.Failed(it.error)
                    )

                    is SavedCardPaymentsVMEffects.InitiateThreeDS -> {
                        val response = it.threeDSecureDto
                        startActivityForResult(
                            ThreeDSecureWebViewActivity.getIntent(
                                context = this@SavedCardPaymentActivity,
                                acsUrl = response.acsUrl,
                                acsPaReq = response.acsPaReq,
                                acsMd = response.acsMd,
                                gatewayUrl = response.threeDSOneUrl
                            ),
                            THREE_D_SECURE_REQUEST_KEY
                        )
                    }

                    is SavedCardPaymentsVMEffects.InitiateThreeDSTwo -> {
                        val response = it.threeDSecureTwoDto
                        startActivityForResult(
                            ThreeDSecureTwoWebViewActivity.getIntent(
                                context = this@SavedCardPaymentActivity,
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

                    SavedCardPaymentsVMEffects.PaymentAuthorised -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED),
                        PaymentsResult.PartiallyAuthorised
                    )

                    SavedCardPaymentsVMEffects.PostAuthReview -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW),
                        PaymentsResult.PostAuthReview
                    )

                    SavedCardPaymentsVMEffects.Purchased -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_PURCHASED),
                        PaymentsResult.Success
                    )
                }
            }
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
                val state = data.getStringExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE)!!
                when (state) {
                    "AUTHORISED" -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED),
                        PaymentsResult.Success
                    )

                    "PURCHASED" -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_PURCHASED),
                        PaymentsResult.Success
                    )

                    "CAPTURED" -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED),
                        PaymentsResult.Success
                    )

                    "FAILED" -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED),
                        PaymentsResult.Failed("3DS failed")
                    )

                    "POST_AUTH_REVIEW" -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW),
                        PaymentsResult.PostAuthReview
                    )

                    "AWAITING_PARTIAL_AUTH_APPROVAL" -> {
                        runCatching {
                            requireNotNull(
                                data.getParcelableExtra(INTENT_CHALLENGE_RESPONSE) as? PartialAuthIntent
                            ) {
                                "Partial auth intent is missing"
                            }
                        }.getOrElse {
                            finishWithData(
                                CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED),
                                PaymentsResult.Failed(it.message.orEmpty())
                            )
                            return
                        }.let {
                            viewModel.initiatePartialAuth(it)
                        }
                    }

                    else -> finishWithData(
                        CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED),
                        PaymentsResult.Failed("3DS failed")
                    )
                }
            } else {
                finishWithData(
                    CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED),
                    PaymentsResult.Failed("3DS failed")
                )
            }
        }
    }

    private fun finishWithData(
        cardPaymentData: CardPaymentData,
        result: PaymentsResult
    ) {
        val intent = Intent().apply {
            putExtra(CardPaymentData.INTENT_DATA_KEY, cardPaymentData)
            putExtra(SavedCardPaymentLauncherContract.EXTRA_RESULT, result)
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
    }
}