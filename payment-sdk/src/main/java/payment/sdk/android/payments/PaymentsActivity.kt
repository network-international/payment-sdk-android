package payment.sdk.android.payments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
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
import payment.sdk.android.payments.theme.PgColors
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.launch
import org.json.JSONObject
import payment.sdk.android.SDKConfig
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.qpay.QPayLauncher
import payment.sdk.android.benefit.BenefitLauncher
import payment.sdk.android.bnpl.BnplLauncher
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
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.SliceRequest
import payment.sdk.android.samsungpay.SamsungPayClient
import payment.sdk.android.samsungpay.SamsungPayResponse
import payment.sdk.android.payments.model.PaymentResultArgs
import payment.sdk.android.payments.view.PaymentResultScreen
import payment.sdk.android.payments.view.UnifiedPaymentPageScreen
import payment.sdk.android.sdk.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UnifiedPaymentPageActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NI-SDK-GPay-Debug"
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = SDKConfig.getLanguage()
        val locale = Locale(lang)
        val config = newBase.resources.configuration.apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private val viewModel: UnifiedPaymentPageViewModel by viewModels { UnifiedPaymentPageViewModel.Factory(args) }

    /** Snapshot of the Authorized state — captured the first time it arrives so it survives the
     *  Loading transition that happens during payment dispatch. Required for the result page,
     *  which is rendered AFTER state has moved past Authorized. */
    private var lastAuthorizedState: UnifiedPaymentPageVMUiState.Authorized? = null

    private lateinit var args: UnifiedPaymentPageRequest

    private val samsungPayClient: SamsungPayClient? by lazy {
        args.samsungPayConfig?.let { config ->
            SamsungPayClient(this, config.serviceId, CoroutinesGatewayHttpClient())
        }
    }

    private val paymentDataLauncher =
        registerForActivityResult(GetPaymentDataResult()) { taskResult ->
            Log.d(TAG, "paymentDataLauncher: statusCode=${taskResult.status.statusCode}, statusMessage=${taskResult.status.statusMessage}")
            when (taskResult.status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    Log.d(TAG, "paymentDataLauncher: SUCCESS branch")
                    try {
                        val paymentMethodData = taskResult.result
                            ?.toJson()
                            ?.let { JSONObject(it).getJSONObject("paymentMethodData") }

                        val token = paymentMethodData
                            ?.getJSONObject("tokenizationData")
                            ?.getString("token")
                            .orEmpty()

                        Log.d(TAG, "paymentDataLauncher: token length=${token.length}, isEmpty=${token.isEmpty()}")
                        if (token.isNotEmpty()) {
                            Log.d(TAG, "paymentDataLauncher: calling viewModel.acceptGooglePay")
                            viewModel.acceptGooglePay(token)
                        } else {
                            Log.w(TAG, "paymentDataLauncher: token empty → setProcessingFinished + Failed")
                            viewModel.setProcessingFinished()
                            finishWithData(UnifiedPaymentPageResult.Failed("Google Pay token is empty"))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "paymentDataLauncher: parse exception → setProcessingFinished + Failed", e)
                        viewModel.setProcessingFinished()
                        finishWithData(UnifiedPaymentPageResult.Failed("Failed to parse Google Pay result"))
                    }
                }

                CommonStatusCodes.CANCELED -> {
                    Log.d(TAG, "paymentDataLauncher: CANCELED → setProcessingFinished")
                    viewModel.setProcessingFinished()
                }

                AutoResolveHelper.RESULT_ERROR -> {
                    Log.e(TAG, "paymentDataLauncher: RESULT_ERROR → setProcessingFinished + Failed")
                    viewModel.setProcessingFinished()
                    finishWithData(UnifiedPaymentPageResult.Failed("Google Pay error"))
                }

                CommonStatusCodes.INTERNAL_ERROR -> {
                    Log.e(TAG, "paymentDataLauncher: INTERNAL_ERROR → setProcessingFinished + Failed")
                    viewModel.setProcessingFinished()
                    finishWithData(UnifiedPaymentPageResult.Failed("Google Pay error"))
                }

                else -> {
                    Log.w(TAG, "paymentDataLauncher: unhandled statusCode=${taskResult.status.statusCode} → setProcessingFinished")
                    viewModel.setProcessingFinished()
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

    private val qpayLauncher = QPayLauncher(this) { result ->
        when (result) {
            QPayLauncher.Result.Success -> finishWithData(UnifiedPaymentPageResult.Success)
            is QPayLauncher.Result.Failed -> finishWithData(UnifiedPaymentPageResult.Failed(result.error))
            QPayLauncher.Result.Canceled -> {}
            QPayLauncher.Result.InvalidRequest -> finishWithData(UnifiedPaymentPageResult.Failed("Invalid QPay request"))
        }
    }

    private val benefitLauncher = BenefitLauncher(this) { result ->
        when (result) {
            BenefitLauncher.Result.Success -> finishWithData(UnifiedPaymentPageResult.Success)
            BenefitLauncher.Result.PostAuthReview -> finishWithData(UnifiedPaymentPageResult.PostAuthReview)
            is BenefitLauncher.Result.Failed -> finishWithData(UnifiedPaymentPageResult.Failed(result.error))
            // Nothing was recorded against the payment, so the order is still payable — stay put.
            BenefitLauncher.Result.Canceled -> {}
            // Cancelling on Benefit's own page is the payer changing their mind, not a payment
            // outcome, so it hands them back to the payment page with their other options intact
            // rather than ending the payment on their behalf.
            BenefitLauncher.Result.CanceledOnProvider -> {}
            BenefitLauncher.Result.InvalidRequest -> finishWithData(UnifiedPaymentPageResult.Failed("Invalid Benefit request"))
        }
    }

    private val bnplLauncher = BnplLauncher(this) { result ->
        when (result) {
            BnplLauncher.Result.Success -> finishWithData(UnifiedPaymentPageResult.Success)
            BnplLauncher.Result.PostAuthReview -> finishWithData(UnifiedPaymentPageResult.PostAuthReview)
            is BnplLauncher.Result.Failed -> finishWithData(UnifiedPaymentPageResult.Failed(result.error))
            // The checkout never opened, so nothing is owed and every other method is still
            // available. Ending the payment here would cost the merchant a sale over a provider
            // outage, so the payer returns to the page with that row marked unavailable instead.
            is BnplLauncher.Result.Unavailable -> viewModel.markBnplUnavailable(result.provider)
            // Nothing was recorded against the payment, so the order is still payable — stay put.
            BnplLauncher.Result.Canceled -> {}
            // Cancelling on the provider's own page is the payer changing their mind, not a payment
            // outcome, so they keep their other options rather than the payment ending for them.
            BnplLauncher.Result.CanceledOnProvider -> {}
            BnplLauncher.Result.InvalidRequest -> finishWithData(UnifiedPaymentPageResult.Failed("Invalid buy-now-pay-later request"))
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
        window.setBackgroundDrawableResource(android.R.color.white)
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
            val sliceCheckState by viewModel.sliceCheckState.collectAsState()
            val visCheckState by viewModel.visCheckState.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                when (state) {
                    is UnifiedPaymentPageVMUiState.Authorized -> {
                        val authState = (state as UnifiedPaymentPageVMUiState.Authorized)
                        lastAuthorizedState = authState
                        UnifiedPaymentPageScreen(
                            supportedCards = authState.supportedCards.toMutableSet().apply {
                                add(CardType.Visa)
                            },
                            googlePayUiConfig = authState.googlePayUiConfig,
                            isSamsungPayAvailable = authState.isSamsungPayAvailable,
                            sliceCheckState = sliceCheckState,
                            visCheckState = visCheckState,
                            onCheckSliceEligibility = { pan, expiryRaw, cardScheme ->
                                viewModel.checkSliceEligibility(
                                    eligibilityCheckUrl = authState.sliceEligibilityCheckUrl,
                                    paymentCookie = authState.paymentCookie,
                                    pan = pan,
                                    expiryRaw = expiryRaw,
                                    visEligibilityCheckUrl = authState.visEligibilityCheckUrl,
                                    selfUrl = authState.selfUrl,
                                    cardScheme = cardScheme
                                )
                            },
                            onCheckSavedCardEligibility = { savedCard ->
                                // Same Slice → Vis fallback chain as manual entry. The token is
                                // sent in the API's `cardToken` field (handled by the interactor
                                // when isSavedCardToken=true) so Slice no longer 422s on tokens.
                                viewModel.checkSliceEligibility(
                                    eligibilityCheckUrl = authState.sliceEligibilityCheckUrl,
                                    paymentCookie = authState.paymentCookie,
                                    pan = savedCard.cardToken,
                                    expiryRaw = savedCard.expiry,
                                    visEligibilityCheckUrl = authState.visEligibilityCheckUrl,
                                    selfUrl = authState.selfUrl,
                                    cardScheme = savedCard.scheme,
                                    isSavedCardToken = true
                                )
                            },
                            onResetSliceCheck = { viewModel.resetSliceCheck() },
                            onMakePayment = { cardNumber, expiry, cvv, cardholderName, sliceOffer, visaPlan ->
                                val visaRequest = visaPlan
                                    ?.takeIf { it.frequency != payment.sdk.android.visaInstalments.model.PlanFrequency.PayInFull }
                                    ?.let { plan ->
                                        payment.sdk.android.core.interactor.VisaRequest(
                                            planSelectionIndicator = true,
                                            vPlanId = plan.id,
                                            acceptedTAndCVersion = plan.terms?.version ?: 1
                                        )
                                    }
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
                                    payerIp = authState.payerIp,
                                    sliceRequest = sliceOffer?.let {
                                        SliceRequest(it.period, it.rate, it.fee)
                                    },
                                    visaRequest = visaRequest,
                                    // Inline Vis flow has already determined eligibility — skip the
                                    // legacy post-tap getPlans / ShowVisaPlans activity to avoid double-prompt.
                                    skipVisaPlansCheck = visCheckState !is VisCheckState.Idle
                                )
                            },
                            onMakeSavedCardPayment = { savedCard, cvv ->
                                val url = authState.savedCardPaymentUrl ?: return@UnifiedPaymentPageScreen
                                viewModel.makeSavedCardPayment(
                                    savedCard = savedCard,
                                    savedCardPaymentUrl = url,
                                    accessToken = authState.accessToken,
                                    paymentCookie = authState.paymentCookie,
                                    orderUrl = authState.orderUrl,
                                    payerIp = authState.payerIp,
                                    cvv = cvv
                                )
                            },
                            formattedAmount = authState.orderAmount,
                            orderValue = authState.amount,
                            currencyCode = authState.currencyCode,
                            showWallets = authState.showWallets,
                            savedCards = authState.savedCards,
                            orderItems = authState.orderItems,
                            onGooglePay = {
                                viewModel.startGooglePayProcess()
                                authState.googlePayUiConfig?.let { config ->
                                    config.paymentsClient
                                        .loadPaymentData(config.paymentDataRequest)
                                        .addOnCompleteListener(paymentDataLauncher::launch)
                                }
                            },
                            onSamsungPay = {
                                val client = samsungPayClient
                                val order = viewModel.fetchedOrder
                                val merchantName = args.samsungPayConfig?.merchantName
                                if (client != null && order != null && merchantName != null) {
                                    viewModel.startProcessing()
                                    // The page already authorized the order on load (consuming the
                                    // single-use auth code), so hand Samsung Pay the payment-token
                                    // we already hold instead of letting it re-authorize (which fails).
                                    val paymentToken = authState.paymentCookie
                                        .substringAfter("payment-token=", "")
                                        .substringBefore(";")
                                        .ifBlank { null }
                                    client.startSamsungPay(
                                        order = order,
                                        merchantName = merchantName,
                                        paymentToken = paymentToken,
                                        samsungPayResponse = object : SamsungPayResponse {
                                            override fun onSuccess() {
                                                finishWithData(UnifiedPaymentPageResult.Success)
                                            }
                                            override fun onFailure(error: String) {
                                                Log.e(TAG, "Samsung Pay failed: $error")
                                                Toast.makeText(
                                                    this@UnifiedPaymentPageActivity,
                                                    "Samsung Pay: $error",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                viewModel.setProcessingFinished()
                                            }
                                            override fun onCancelled() {
                                                Log.d(TAG, "Samsung Pay cancelled by user")
                                                viewModel.setProcessingFinished()
                                            }
                                        }
                                    )
                                } else {
                                    Log.w(
                                        TAG,
                                        "Samsung Pay preconditions missing — client=${client != null}, " +
                                            "order=${order != null}, merchantName=${merchantName != null}"
                                    )
                                    finishWithData(UnifiedPaymentPageResult.SamsungPayRequested)
                                }
                            },
                            aaniConfig = authState.aaniConfig,
                            clickToPayConfig = authState.clickToPayConfig,
                            qpayConfig = authState.qpayConfig,
                            benefitConfig = authState.benefitConfig,
                            bnplConfigs = authState.bnplConfigs,
                            unavailableBnplProviders = authState.unavailableBnplProviders,
                            belowMinimumBnplProviders = authState.belowMinimumBnplProviders,
                            isProcessing = isProcessing,
                            onClickAaniPay = { config ->
                                aaniPayLauncher.launch(config)
                            },
                            onClickToPay = { config ->
                                // Resolve the gateway DPA credentials lazily (only now, on tap) and
                                // then launch via the LaunchClickToPay effect. If the resolve fails,
                                // ClickToPayActivity's paypage /vctp/config fallback still supplies
                                // the DPA credentials before initializing the Visa SDK.
                                viewModel.onClickToPaySelected(config)
                            },
                            onClickQPay = { config ->
                                qpayLauncher.launch(config)
                            },
                            onClickBenefit = { config ->
                                benefitLauncher.launch(config)
                            },
                            onClickBnpl = { config ->
                                bnplLauncher.launch(config)
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
                Log.d(TAG, "initEffects: received effect=${it::class.simpleName}")
                when (it) {
                    UnifiedPaymentPageVMEffects.Captured -> {
                        Log.d(TAG, "initEffects: Captured → finishWithData Success")
                        finishWithData(UnifiedPaymentPageResult.Success)
                    }
                    is UnifiedPaymentPageVMEffects.Failed -> {
                        Log.e(TAG, "initEffects: Failed error='${it.error}' isProcessing=${viewModel.isProcessing.value} → finishWithData Failed")
                        finishWithData(
                            UnifiedPaymentPageResult.Failed(
                                it.error
                            )
                        )
                    }

                    is UnifiedPaymentPageVMEffects.LaunchClickToPay -> {
                        clickToPayLauncher.launch(it.config)
                    }

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
        if (requestCode !in setOf(THREE_D_SECURE_REQUEST_KEY, THREE_D_SECURE_TWO_REQUEST_KEY)) {
            return
        }

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
        // Use the cached Authorized snapshot — by the time we reach the result, the live state
        // has already transitioned past Authorized (Loading → ShowPaymentResult).
        val authorizedState = lastAuthorizedState
            ?: (viewModel.uiState.value as? UnifiedPaymentPageVMUiState.Authorized)
        val formattedAmount = authorizedState?.orderAmount
        val supportedCards = authorizedState?.supportedCards ?: emptySet()
        val orderReference = viewModel.orderReference

        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateTime = dateFormatter.format(Date())

        val args = PaymentResultArgs(
            isSuccess = isSuccess,
            formattedAmount = formattedAmount,
            transactionId = orderReference,
            dateTime = dateTime,
            supportedCards = supportedCards,
            orderItems = authorizedState?.orderItems ?: emptyList(),
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
