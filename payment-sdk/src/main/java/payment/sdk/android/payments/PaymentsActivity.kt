package payment.sdk.android.payments

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import kotlinx.coroutines.launch
import payment.sdk.android.SDKConfig
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.partialAuth.model.PartialAuthActivityArgs
import payment.sdk.android.cardpayment.savedCard.SavedCardPaymentActivity.Companion.THREE_D_SECURE_REQUEST_KEY
import payment.sdk.android.cardpayment.savedCard.SavedCardPaymentActivity.Companion.THREE_D_SECURE_TWO_REQUEST_KEY
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity.Companion.INTENT_CHALLENGE_RESPONSE
import payment.sdk.android.cardpayment.visaInstalments.model.InstallmentPlan
import payment.sdk.android.cardpayment.visaInstalments.view.InstalmentPlanView
import payment.sdk.android.cardpayment.visaInstalments.view.VisaInstalmentsView
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.core.CardType
import payment.sdk.android.payments.view.PaymentsScreen
import payment.sdk.android.sdk.R

class PaymentsActivity : AppCompatActivity() {

    private val viewModel: PaymentsViewModel by viewModels { PaymentsViewModel.Factory(args) }

    private lateinit var args: PaymentsRequest

    private val paymentDataLauncher =
        registerForActivityResult(GetPaymentDataResult()) { taskResult ->
            when (taskResult.status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    taskResult.result?.let {
                        viewModel.acceptGooglePay(it.toJson())
                    }
                }

                CommonStatusCodes.CANCELED -> finishWithData(PaymentsLauncher.Result.Cancelled)

                AutoResolveHelper.RESULT_ERROR ->
                    finishWithData(PaymentsLauncher.Result.Failed("Google Pay error"))

                CommonStatusCodes.INTERNAL_ERROR ->
                    finishWithData(PaymentsLauncher.Result.Failed("Google Pay error"))
            }
        }

    private val partialAuthActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            finishWithData(CardPaymentData.getCardPaymentState(result.data))
        } else {
            finishWithData(PaymentsLauncher.Result.Failed("Partial auth failed"))
        }
    }

    private val aaniPayLauncher = AaniPayLauncher(this) { result ->
        when (result) {
            AaniPayLauncher.Result.Success -> finishWithData(PaymentsLauncher.Result.Success)
            is AaniPayLauncher.Result.Failed -> finishWithData(PaymentsLauncher.Result.Failed("Aani Pay failed"))
            AaniPayLauncher.Result.Canceled -> {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnBackPressed()
        args = runCatching {
            requireNotNull(PaymentsRequest.fromIntent(intent)) {
                "Payments input arguments were not found"
            }
        }.getOrElse {
            finishWithData(PaymentsLauncher.Result.Failed("intent args not found"))
            return
        }
        initEffects()
        setContent {
            Scaffold(
                backgroundColor = Color(0xFFD6D6D6),
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
                                finishWithData(PaymentsLauncher.Result.Cancelled)
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
                val state by viewModel.uiState.collectAsState()
                when (state) {
                    is PaymentsVMUiState.Authorized -> {
                        val authState = (state as PaymentsVMUiState.Authorized)
                        PaymentsScreen(
                            modifier = Modifier.padding(contentPadding),
                            supportedCards = authState.supportedCards.toMutableSet().apply {
                                add(CardType.Visa)
                            },
                            googlePayUiConfig = authState.googlePayUiConfig,
                            onMakePayment = { cardNumber, expiry, cvv, cardholderName ->
                                viewModel.makeCardPayment(
                                    selfUrl = authState.selfUrl,
                                    cardPaymentUrl = authState.cardPaymentUrl,
                                    accessToken = authState.accessToken,
                                    paymentCookie = authState.paymentCookie,
                                    cardNumber = cardNumber,
                                    expiry = expiry,
                                    cvv = cvv,
                                    cardholderName = cardholderName,
                                    orderUrl = authState.orderUrl,
                                    amount = authState.amount,
                                    currencyCode = authState.currencyCode,
                                    payerIp = authState.payerIp
                                )
                            },
                            formattedAmount = authState.orderAmount,
                            showWallets = authState.showWallets,
                            onGooglePay = {
                                authState.googlePayUiConfig?.task?.addOnCompleteListener(
                                    paymentDataLauncher::launch
                                )
                            },
                            aaniConfig = authState.aaniConfig,
                            onClickAaniPay = { config ->
                                aaniPayLauncher.launch(config)
                            }
                        )
                    }

                    PaymentsVMUiState.Init -> {
                        viewModel.authorize()
                    }

                    is PaymentsVMUiState.Loading -> {
                        CircularProgressDialog((state as PaymentsVMUiState.Loading).message)
                    }

                    is PaymentsVMUiState.ShowVisaPlans -> {
                        val visState = (state as PaymentsVMUiState.ShowVisaPlans)
                        VisaInstalmentsView(
                            instalmentPlans = InstallmentPlan.fromVisaPlans(visState.visaPlans, visState.orderAmount),
                            cardNumber = visState.makeCardPaymentRequest.pan
                        ) { plan ->
                            viewModel.makeVisPayment(makeCardPaymentRequest = visState.makeCardPaymentRequest, selectedPlan = plan, orderUrl = visState.orderUrl)
                        }
                    }
                }
            }
        }
    }

    private fun initEffects() {
        lifecycleScope.launch {
            viewModel.effect.collect {
                when (it) {
                    PaymentsVMEffects.Captured -> finishWithData(PaymentsLauncher.Result.Success)
                    is PaymentsVMEffects.Failed -> finishWithData(
                        PaymentsLauncher.Result.Failed(
                            it.error
                        )
                    )

                    is PaymentsVMEffects.InitiatePartialAuth -> {
                        try {
                            partialAuthActivityLauncher.launch(
                                PartialAuthActivityArgs.getArgs(
                                    it.partialAuthIntent
                                ).toIntent(this@PaymentsActivity)
                            )
                        } catch (e: IllegalArgumentException) {
                            finishWithData(PaymentsLauncher.Result.Failed(""))
                        }
                    }

                    is PaymentsVMEffects.InitiateThreeDS -> {
                        val response = it.threeDSecureDto
                        startActivityForResult(
                            ThreeDSecureWebViewActivity.getIntent(
                                context = this@PaymentsActivity,
                                acsUrl = response.acsUrl,
                                acsPaReq = response.acsPaReq,
                                acsMd = response.acsMd,
                                gatewayUrl = response.threeDSOneUrl
                            ),
                            THREE_D_SECURE_REQUEST_KEY
                        )
                    }

                    is PaymentsVMEffects.InitiateThreeDSTwo -> {
                        val response = it.threeDSecureTwoDto
                        startActivityForResult(
                            ThreeDSecureTwoWebViewActivity.getIntent(
                                context = this@PaymentsActivity,
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

                    PaymentsVMEffects.PaymentAuthorised -> finishWithData(PaymentsLauncher.Result.PartiallyAuthorised)
                    PaymentsVMEffects.PostAuthReview -> finishWithData(PaymentsLauncher.Result.PostAuthReview)
                    PaymentsVMEffects.Purchased -> finishWithData(PaymentsLauncher.Result.Success)
                }
            }
        }
    }

    private fun setOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (SDKConfig.showCancelAlert) {
                    showDialog()
                } else {
                    val intent = Intent().apply {
                        putExtra(PaymentsLauncherContract.EXTRA_RESULT, PaymentsLauncher.Result.Cancelled)
                    }
                    setResult(Activity.RESULT_CANCELED, intent)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_CANCELED) {
            return finishWithData(PaymentsLauncher.Result.Cancelled)
        }

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                THREE_D_SECURE_REQUEST_KEY, THREE_D_SECURE_TWO_REQUEST_KEY -> {
                    runCatching {
                        val state =
                            requireNotNull(data?.getStringExtra(ThreeDSecureWebViewActivity.KEY_3DS_STATE)) {
                                "State is missing from 3DS Secure result"
                            }
                        when (state) {
                            "AUTHORISED" -> finishWithData(PaymentsLauncher.Result.Authorised)
                            "PURCHASED", "CAPTURED" -> finishWithData(PaymentsLauncher.Result.Success)
                            "FAILED" -> finishWithData(PaymentsLauncher.Result.Failed("3DS Failed"))
                            "POST_AUTH_REVIEW" -> finishWithData(PaymentsLauncher.Result.PostAuthReview)
                            "AWAITING_PARTIAL_AUTH_APPROVAL" -> {
                                runCatching {
                                    requireNotNull(
                                        data?.getParcelableExtra(
                                            INTENT_CHALLENGE_RESPONSE
                                        ) as? PartialAuthIntent
                                    ) {
                                        "Partial auth intent is missing"
                                    }
                                }.getOrElse {
                                    finishWithData(PaymentsLauncher.Result.Failed(it.message.orEmpty()))
                                    return
                                }.let {
                                    startPartialAuthActivity(it)
                                }
                            }

                            else -> finishWithData(PaymentsLauncher.Result.Failed("3DS Failed"))
                        }
                    }.onFailure {
                        finishWithData(
                            PaymentsLauncher.Result.Failed(
                                it.message ?: "Failed 3DS"
                            )
                        )
                    }
                }
            }
        } else {
            return finishWithData(PaymentsLauncher.Result.Failed("Failed 3DS"))
        }
    }

    private fun startPartialAuthActivity(partialAuthIntent: PartialAuthIntent) {
        try {
            partialAuthActivityLauncher.launch(
                PartialAuthActivityArgs.getArgs(partialAuthIntent).toIntent(this)
            )
        } catch (e: IllegalArgumentException) {
            finishWithData(PaymentsLauncher.Result.Failed(e.message.orEmpty()))
        }
    }

    private fun finishWithData(result: PaymentsLauncher.Result) {
        val intent = Intent().apply {
            putExtra(PaymentsLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}