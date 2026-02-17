package payment.sdk.android.payments

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import kotlinx.coroutines.launch
import org.json.JSONObject
import payment.sdk.android.SDKConfig
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.partialAuth.model.PartialAuthActivityArgs
import payment.sdk.android.partialAuth.view.PartialAuthView
import payment.sdk.android.savedCard.SavedCardPaymentActivity.Companion.THREE_D_SECURE_REQUEST_KEY
import payment.sdk.android.savedCard.SavedCardPaymentActivity.Companion.THREE_D_SECURE_TWO_REQUEST_KEY
import payment.sdk.android.cardpayment.threedsecure.ThreeDSecureWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity
import payment.sdk.android.cardpayment.threedsecuretwo.webview.ThreeDSecureTwoWebViewActivity.Companion.INTENT_CHALLENGE_RESPONSE
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.view.VisaInstalmentsView
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.CardType
import payment.sdk.android.payments.model.PaymentResultArgs
import payment.sdk.android.payments.view.PaymentResultScreen
import payment.sdk.android.payments.view.UnifiedPaymentPageScreen
import payment.sdk.android.sdk.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UnifiedPaymentPageActivity : AppCompatActivity() {

    private val viewModel: UnifiedPaymentPageViewModel by viewModels { UnifiedPaymentPageViewModel.Factory(args) }

    private lateinit var args: UnifiedPaymentPageRequest

    private val paymentDataLauncher =
        registerForActivityResult(GetPaymentDataResult()) { taskResult ->
            when (taskResult.status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    try {
                        val paymentMethodData = taskResult.result
                            ?.toJson()
                            ?.let { JSONObject(it).getJSONObject("paymentMethodData") }

                        val token = paymentMethodData
                            ?.getJSONObject("tokenizationData")
                            ?.getString("token")
                            .orEmpty()

                        if (token.isNotEmpty()) {
                            viewModel.acceptGooglePay(token)
                        } else {
                            viewModel.setProcessingFinished()
                            finishWithData(UnifiedPaymentPageResult.Failed("Google Pay token is empty"))
                        }
                    } catch (e: Exception) {
                        viewModel.setProcessingFinished()
                        finishWithData(UnifiedPaymentPageResult.Failed("Failed to parse Google Pay result"))
                    }
                }

                CommonStatusCodes.CANCELED -> {
                    viewModel.setProcessingFinished()
                    finishWithData(UnifiedPaymentPageResult.Cancelled)
                }

                AutoResolveHelper.RESULT_ERROR -> {
                    viewModel.setProcessingFinished()
                    finishWithData(UnifiedPaymentPageResult.Failed("Google Pay error"))
                }


                CommonStatusCodes.INTERNAL_ERROR -> {
                    viewModel.setProcessingFinished()
                    finishWithData(UnifiedPaymentPageResult.Failed("Google Pay error"))
                }
            }
        }

    private val aaniPayLauncher = AaniPayLauncher(this) { result ->
        when (result) {
            AaniPayLauncher.Result.Success -> finishWithData(UnifiedPaymentPageResult.Success)
            is AaniPayLauncher.Result.Failed -> finishWithData(UnifiedPaymentPageResult.Failed("Aani Pay failed"))
            AaniPayLauncher.Result.Canceled -> {}
        }
    }

    private val clickToPayLauncher = ClickToPayLauncher(this) { result ->
        when (result) {
            ClickToPayLauncher.Result.Success -> finishWithData(UnifiedPaymentPageResult.Success)
            ClickToPayLauncher.Result.Authorised -> finishWithData(UnifiedPaymentPageResult.Authorised)
            ClickToPayLauncher.Result.Captured -> finishWithData(UnifiedPaymentPageResult.Success)
            ClickToPayLauncher.Result.PostAuthReview -> finishWithData(UnifiedPaymentPageResult.PostAuthReview)
            is ClickToPayLauncher.Result.Failed -> finishWithData(UnifiedPaymentPageResult.Failed(result.error))
            ClickToPayLauncher.Result.Canceled -> {}
            is ClickToPayLauncher.Result.Requires3DS -> {
                startActivityForResult(
                    ThreeDSecureWebViewActivity.getIntent(
                        context = this,
                        acsUrl = result.acsUrl,
                        acsPaReq = result.acsPaReq,
                        acsMd = result.acsMd,
                        gatewayUrl = null
                    ),
                    THREE_D_SECURE_REQUEST_KEY
                )
            }
            is ClickToPayLauncher.Result.Requires3DSTwo -> {
                val currentState = viewModel.uiState.value
                val orderUrl = result.orderUrl
                    ?: (currentState as? UnifiedPaymentPageVMUiState.Authorized)?.orderUrl
                    ?: ""
                startActivityForResult(
                    ThreeDSecureTwoWebViewActivity.getIntent(
                        context = this,
                        threeDSMethodData = result.threeDSMethodData,
                        threeDSMethodNotificationURL = result.threeDSMethodNotificationURL,
                        threeDSMethodURL = result.threeDSMethodUrl,
                        threeDSServerTransID = result.threeDSServerTransId,
                        paymentCookie = result.paymentCookie,
                        threeDSAuthenticationsUrl = result.threeDSTwoAuthenticationURL,
                        directoryServerID = result.directoryServerId,
                        threeDSMessageVersion = result.threeDSMessageVersion,
                        threeDSTwoChallengeResponseURL = result.threeDSTwoChallengeResponseURL,
                        outletRef = result.outletRef,
                        orderRef = result.orderRef,
                        orderUrl = orderUrl,
                        paymentRef = result.paymentRef
                    ),
                    THREE_D_SECURE_TWO_REQUEST_KEY
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnBackPressed()
        args = runCatching {
            requireNotNull(UnifiedPaymentPageRequest.fromIntent(intent)) {
                "Payments input arguments were not found"
            }
        }.getOrElse {
            finishWithData(UnifiedPaymentPageResult.Failed("intent args not found"))
            return
        }
        initEffects()
        setContent {
            val state by viewModel.uiState.collectAsState()
            val isProcessing by viewModel.isProcessing.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                when (state) {
                    is UnifiedPaymentPageVMUiState.Authorized -> {
                        val authState = (state as UnifiedPaymentPageVMUiState.Authorized)
                        UnifiedPaymentPageScreen(
                            supportedCards = authState.supportedCards.toMutableSet().apply {
                                add(CardType.Visa)
                            },
                            googlePayUiConfig = authState.googlePayUiConfig,
                            isSamsungPayAvailable = authState.isSamsungPayAvailable,
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
                                viewModel.startGooglePayProcess()
                                authState.googlePayUiConfig?.task?.addOnCompleteListener(
                                    paymentDataLauncher::launch
                                )
                            },
                            onSamsungPay = {
                                finishWithData(UnifiedPaymentPageResult.SamsungPayRequested)
                            },
                            aaniConfig = authState.aaniConfig,
                            clickToPayConfig = authState.clickToPayConfig,
                            isProcessing = isProcessing,
                            onClickAaniPay = { config ->
                                aaniPayLauncher.launch(config)
                            },
                            onClickToPay = { config ->
                                clickToPayLauncher.launch(config)
                            },
                            onClose = {
                                finishWithData(UnifiedPaymentPageResult.Cancelled)
                            }
                        )
                    }

                    UnifiedPaymentPageVMUiState.Init -> {
                        viewModel.authorize()
                    }

                    is UnifiedPaymentPageVMUiState.Loading -> {
                        CircularProgressDialog((state as UnifiedPaymentPageVMUiState.Loading).message)
                    }

                    is UnifiedPaymentPageVMUiState.ShowVisaPlans -> {
                        val visState = (state as UnifiedPaymentPageVMUiState.ShowVisaPlans)
                        VisaInstalmentsView(
                            instalmentPlans = InstallmentPlan.fromVisaPlans(
                                visState.visaPlans,
                                visState.orderAmount
                            ),
                            cardNumber = visState.makeCardPaymentRequest.pan
                        ) { plan ->
                            viewModel.makeVisPayment(
                                makeCardPaymentRequest = visState.makeCardPaymentRequest,
                                selectedPlan = plan,
                                orderUrl = visState.orderUrl
                            )
                        }
                    }

                    is UnifiedPaymentPageVMUiState.InitiatePartialAuth -> {
                        val partialAuthState = (state as UnifiedPaymentPageVMUiState.InitiatePartialAuth)
                        PartialAuthView(
                            args = PartialAuthActivityArgs.getArgs(
                                partialAuthState.partialAuthIntent
                            )
                        ) { result ->
                            finishWithData(result.getCardPaymentsState())
                        }
                    }

                    is UnifiedPaymentPageVMUiState.ShowPaymentResult -> {
                        val resultState = (state as UnifiedPaymentPageVMUiState.ShowPaymentResult)
                        PaymentResultScreen(
                            args = resultState.args,
                            onDone = {
                                actuallyFinishWithData(resultState.pendingResult)
                            }
                        )
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {}
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressDialog(message = LoadingMessage.PAYMENT)
                    }
                }
            }
        }
    }

    private fun initEffects() {
        lifecycleScope.launch {
            viewModel.effect.collect {
                when (it) {
                    UnifiedPaymentPageVMEffects.Captured -> finishWithData(UnifiedPaymentPageResult.Success)
                    is UnifiedPaymentPageVMEffects.Failed -> finishWithData(
                        UnifiedPaymentPageResult.Failed(
                            it.error
                        )
                    )

                    is UnifiedPaymentPageVMEffects.InitiateThreeDS -> {
                        val response = it.threeDSecureDto
                        startActivityForResult(
                            ThreeDSecureWebViewActivity.getIntent(
                                context = this@UnifiedPaymentPageActivity,
                                acsUrl = response.acsUrl,
                                acsPaReq = response.acsPaReq,
                                acsMd = response.acsMd,
                                gatewayUrl = response.threeDSOneUrl
                            ),
                            THREE_D_SECURE_REQUEST_KEY
                        )
                    }

                    is UnifiedPaymentPageVMEffects.InitiateThreeDSTwo -> {
                        val response = it.threeDSecureTwoDto
                        startActivityForResult(
                            ThreeDSecureTwoWebViewActivity.getIntent(
                                context = this@UnifiedPaymentPageActivity,
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

                    UnifiedPaymentPageVMEffects.PaymentAuthorised -> finishWithData(UnifiedPaymentPageResult.PartiallyAuthorised)
                    UnifiedPaymentPageVMEffects.PostAuthReview -> finishWithData(UnifiedPaymentPageResult.PostAuthReview)
                    UnifiedPaymentPageVMEffects.Purchased -> finishWithData(UnifiedPaymentPageResult.Success)
                }
            }
        }
    }

    private fun setOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Disable back button on result screen
                val currentState = viewModel.uiState.value
                if (currentState is UnifiedPaymentPageVMUiState.ShowPaymentResult) {
                    return
                }
                if (SDKConfig.showCancelAlert) {
                    showDialog()
                } else {
                    val intent = Intent().apply {
                        putExtra(
                            UnifiedPaymentPageLauncherContract.EXTRA_RESULT,
                            UnifiedPaymentPageResult.Cancelled
                        )
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
            return finishWithData(UnifiedPaymentPageResult.Cancelled)
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
                            "AUTHORISED" -> finishWithData(UnifiedPaymentPageResult.Authorised)
                            "PURCHASED", "CAPTURED" -> finishWithData(UnifiedPaymentPageResult.Success)
                            "FAILED" -> finishWithData(UnifiedPaymentPageResult.Failed("3DS Failed"))
                            "POST_AUTH_REVIEW" -> finishWithData(UnifiedPaymentPageResult.PostAuthReview)
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
                                    finishWithData(UnifiedPaymentPageResult.Failed(it.message.orEmpty()))
                                    return
                                }.let {
                                    viewModel.startPartialAuth(it)
                                }
                            }

                            else -> finishWithData(UnifiedPaymentPageResult.Failed("3DS Failed"))
                        }
                    }.onFailure {
                        finishWithData(
                            UnifiedPaymentPageResult.Failed(
                                it.message ?: "Failed 3DS"
                            )
                        )
                    }
                }
            }
        } else {
            return finishWithData(UnifiedPaymentPageResult.Failed("Failed 3DS"))
        }
    }

    private fun finishWithData(result: UnifiedPaymentPageResult) {
        // If already on result screen, don't intercept again
        if (viewModel.uiState.value is UnifiedPaymentPageVMUiState.ShowPaymentResult) {
            actuallyFinishWithData(result)
            return
        }
        // Show result screen for success/failure statuses
        when (result) {
            is UnifiedPaymentPageResult.Success,
            is UnifiedPaymentPageResult.Authorised -> {
                showPaymentResult(isSuccess = true, result = result)
                return
            }
            is UnifiedPaymentPageResult.Failed -> {
                showPaymentResult(isSuccess = false, result = result)
                return
            }
            else -> {
                // Cancelled, PostAuthReview, SamsungPayRequested, PartialAuth* — skip result screen
                actuallyFinishWithData(result)
            }
        }
    }

    private fun showPaymentResult(isSuccess: Boolean, result: UnifiedPaymentPageResult) {
        val currentState = viewModel.uiState.value
        val formattedAmount = (currentState as? UnifiedPaymentPageVMUiState.Authorized)?.orderAmount
        val supportedCards = (currentState as? UnifiedPaymentPageVMUiState.Authorized)?.supportedCards ?: emptySet()

        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateTime = dateFormatter.format(Date())

        val args = PaymentResultArgs(
            isSuccess = isSuccess,
            formattedAmount = formattedAmount,
            transactionId = "",
            dateTime = dateTime,
            supportedCards = supportedCards
        )

        viewModel.showPaymentResult(
            UnifiedPaymentPageVMUiState.ShowPaymentResult(
                args = args,
                pendingResult = result
            )
        )
    }

    private fun actuallyFinishWithData(result: UnifiedPaymentPageResult) {
        val intent = Intent().apply {
            putExtra(UnifiedPaymentPageLauncherContract.EXTRA_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
