package payment.sdk.android.payments

import android.app.Application
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.wallet.Wallet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.core.interactor.ClickToPayConfig
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.threedsecuretwo.webview.toIntent
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.model.PlanFrequency
import payment.sdk.android.cardpayment.widget.DateFormatter
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.CardMapping
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.Utils.getQueryParameter
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import payment.sdk.android.core.getAaniPayLink
import payment.sdk.android.core.getAaniQrPayLink
import payment.sdk.android.core.getCardPaymentUrl
import payment.sdk.android.core.getGooglePayConfigUrl
import payment.sdk.android.core.getGooglePayUrl
import payment.sdk.android.core.getPaymentReference
import payment.sdk.android.core.getSelfUrl
import payment.sdk.android.core.getPayPageUrl
import payment.sdk.android.core.getVisaClickToPayUrl
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.CardPaymentInteractor
import payment.sdk.android.core.interactor.MakeCardPaymentRequest
import payment.sdk.android.core.interactor.CardPaymentResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.GetPayerIpInteractor
import payment.sdk.android.core.interactor.GooglePayAcceptInteractor
import payment.sdk.android.core.interactor.GooglePayConfigInteractor
import payment.sdk.android.core.interactor.VisaInstallmentPlanInteractor
import payment.sdk.android.core.interactor.VisaPlansResponse
import payment.sdk.android.core.interactor.VisaRequest
import payment.sdk.android.googlepay.GooglePayConfigFactory
import payment.sdk.android.SDKConfig
import payment.sdk.android.googlepay.GooglePayJsonConfig
import payment.sdk.android.googlepay.env

@Keep
internal class UnifiedPaymentPageViewModel(
    private val cardPaymentsIntent: UnifiedPaymentPageRequest,
    private val authApiInteractor: AuthApiInteractor,
    private val cardPaymentInteractor: CardPaymentInteractor,
    private val visaInstalmentPlanInteractor: VisaInstallmentPlanInteractor,
    private val getPayerIpInteractor: GetPayerIpInteractor,
    private val googlePayConfigFactory: GooglePayConfigFactory,
    private val threeDSecureFactory: ThreeDSecureFactory,
    private val googlePayAcceptInteractor: GooglePayAcceptInteractor,
    private val getOrderApiInteractor: GetOrderApiInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var _uiState: MutableStateFlow<UnifiedPaymentPageVMUiState> =
        MutableStateFlow(UnifiedPaymentPageVMUiState.Init)

    val uiState: StateFlow<UnifiedPaymentPageVMUiState> = _uiState.asStateFlow()

    private var _effects = MutableSharedFlow<UnifiedPaymentPageVMEffects>(replay = 1)

    val effect = _effects.asSharedFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    var orderReference: String = ""
        private set

    fun startGooglePayProcess() {
        _isProcessing.value = true
    }

    fun setProcessingFinished() {
        _isProcessing.value = false
    }

    fun showPaymentResult(state: UnifiedPaymentPageVMUiState.ShowPaymentResult) {
        _isProcessing.value = false
        _uiState.update { state }
    }

    fun authorize() {
        _uiState.update { UnifiedPaymentPageVMUiState.Loading(LoadingMessage.AUTH) }
        viewModelScope.launch(dispatcher) {
            val authCode = cardPaymentsIntent.paymentUrl.getQueryParameter("code")
            if (authCode.isNullOrBlank()) {
                return@launch
            }
            val authResponse = authApiInteractor.authenticate(
                authUrl = cardPaymentsIntent.authorizationUrl, authCode = authCode
            )
            when (authResponse) {
                is AuthResponse.Error -> _effects.emit(UnifiedPaymentPageVMEffects.Failed(authResponse.error.message.orEmpty()))

                is AuthResponse.Success -> {
                    getOrder(
                        authResponse.orderUrl,
                        authResponse.getAccessToken(),
                        authResponse.getPaymentCookie()
                    )
                }
            }
        }
    }

    suspend fun getOrder(orderUrl: String, accessToken: String, paymentCookie: String) {
        val order = requireNotNull(getOrderApiInteractor.getOrder(orderUrl, accessToken)) {
            _effects.emit(UnifiedPaymentPageVMEffects.Failed("Failed to fetch order details"))
            return
        }

        val payerIp = getPayerIpInteractor.getPayerIp(cardPaymentsIntent.paymentUrl).orEmpty()

        val amount = requireNotNull(order.amount?.value) {
            _effects.emit(UnifiedPaymentPageVMEffects.Failed("Failed to fetch order amount"))
            return
        }

        val currencyCode = requireNotNull(order.amount?.currencyCode) {
            _effects.emit(UnifiedPaymentPageVMEffects.Failed("Failed to fetch order currencyCode"))
            return
        }

        val supportedWallets = order.paymentMethods?.wallet.orEmpty()
        val apm = order.paymentMethods?.apm.orEmpty()

        val googlePayUrl = order.getGooglePayUrl()
        val googlePayConfig =
            takeIf { supportedWallets.contains("GOOGLE_PAY") && googlePayUrl != null }?.run {
                googlePayConfigFactory.checkGooglePayConfig(
                    googlePayConfigUrl = order.getGooglePayConfigUrl(),
                    accessToken = accessToken,
                    amount = amount,
                    currencyCode = currencyCode,
                    googlePayAcceptUrl = googlePayUrl.orEmpty()
                )
            }

        val aaniConfig = takeIf {
            apm.contains("AANI") && !order.getAaniPayLink().isNullOrBlank()
        }?.let {
            AaniPayLauncher.Config(
                amount = amount,
                currencyCode = currencyCode,
                payerIp = payerIp,
                accessToken = accessToken,
                anniPaymentLink = order.getAaniPayLink().orEmpty(),
                anniQrPaymentLink = order.getAaniQrPayLink().orEmpty(),
            )
        }

        // Configure Click to Pay if VISA_CLICK_TO_PAY is in the wallet array and merchant has configured it
        val clickToPayUrl = order.getVisaClickToPayUrl()
        val merchantClickToPayConfig = cardPaymentsIntent.clickToPayConfig
        val isVisaClickToPayEnabled = supportedWallets.contains("VISA_CLICK_TO_PAY")

        val clickToPayConfig = takeIf {
            merchantClickToPayConfig != null && isVisaClickToPayEnabled
        }?.let {
            // Extract order details for building the unified-click-to-pay URL
            val payment = order.embedded?.payment?.firstOrNull()
            val outletId = payment?.outletId ?: order.outletId
            val paymentRef = order.getPaymentReference()
            // Order ID is typically in the reference before the colon
            val orderId = order.reference

            val minorUnit = try {
                java.util.Currency.getInstance(currencyCode).defaultFractionDigits
            } catch (e: Exception) { 2 }
            val displayAmount = amount / Math.pow(10.0, minorUnit.toDouble())

            ClickToPayLauncher.Config(
                clickToPayConfig = merchantClickToPayConfig!!,
                clickToPayUrl = clickToPayUrl ?: order.getCardPaymentUrl().orEmpty(),
                amount = displayAmount,
                currencyCode = currencyCode,
                accessToken = accessToken,
                paymentCookie = paymentCookie,
                orderReference = order.reference,
                merchantName = merchantClickToPayConfig.dpaName,
                outletId = outletId,
                orderId = orderId,
                paymentRef = paymentRef,
                payPageUrl = cardPaymentsIntent.paymentUrl,
                orderUrl = orderUrl,
                testOtpMode = merchantClickToPayConfig.testOtpMode,
                locale = SDKConfig.getLanguage()
            )
        }

        val supportedCards = order.paymentMethods?.card.orEmpty()

        if (supportedCards.isEmpty()) {
            _effects.emit(UnifiedPaymentPageVMEffects.Failed("No supported card scheme found"))
            return
        }

        val isSamsungPayAvailable = supportedWallets.contains("SAMSUNG_PAY")

        orderReference = order.reference.orEmpty()

        _uiState.update {
            UnifiedPaymentPageVMUiState.Authorized(
                accessToken = accessToken,
                paymentCookie = paymentCookie,
                orderUrl = orderUrl,
                supportedCards = CardMapping.mapSupportedCards(supportedCards),
                googlePayUiConfig = googlePayConfig,
                isSamsungPayAvailable = isSamsungPayAvailable,
                showWallets = supportedWallets.contains("GOOGLE_PAY") || isSamsungPayAvailable || apm.contains("AANI") || clickToPayConfig != null,
                orderAmount = order.formattedAmount.orEmpty(),
                cardPaymentUrl = order.getCardPaymentUrl().orEmpty(),
                amount = amount,
                currencyCode = currencyCode,
                selfUrl = order.getSelfUrl().orEmpty(),
                locale = SDKConfig.getLanguage(),
                aaniConfig = aaniConfig,
                clickToPayConfig = clickToPayConfig,
                payerIp = payerIp,
                orderReference = order.reference.orEmpty()
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun makeCardPayment(
        selfUrl: String,
        cardPaymentUrl: String,
        accessToken: String,
        paymentCookie: String,
        cardNumber: String,
        orderUrl: String,
        expiry: String,
        cvv: String,
        cardholderName: String,
        amount: Double,
        currencyCode: String,
        payerIp: String
    ) {
        val makeCardPaymentRequest = MakeCardPaymentRequest(
            payerIp = payerIp,
            paymentCookie = paymentCookie,
            cardHolder = cardholderName,
            expiry = DateFormatter.formatExpireDateForApi(expiry),
            cvv = cvv,
            paymentUrl = cardPaymentUrl,
            pan = cardNumber
        )
        _uiState.update { UnifiedPaymentPageVMUiState.Loading(LoadingMessage.PAYMENT) }
        viewModelScope.launch(dispatcher) {
            val response = visaInstalmentPlanInteractor.getPlans(
                cardNumber = cardNumber,
                token = paymentCookie,
                selfUrl = selfUrl
            )

            if (response is VisaPlansResponse.Success && response.visaPlans.matchedPlans.isNotEmpty()) {
                _uiState.update {
                    UnifiedPaymentPageVMUiState.ShowVisaPlans(
                        makeCardPaymentRequest = makeCardPaymentRequest,
                        visaPlans = response.visaPlans,
                        orderUrl = orderUrl,
                        orderAmount = OrderAmount(amount, currencyCode)
                    )
                }
            } else {
                initiateCardPayment(
                    makeCardPaymentRequest = makeCardPaymentRequest,
                    orderUrl = orderUrl,
                )
            }
        }
    }

    fun acceptGooglePay(paymentDataJson: String) {
        viewModelScope.launch(dispatcher) {
            val currentState = uiState.value

            if (currentState !is UnifiedPaymentPageVMUiState.Authorized || currentState.googlePayUiConfig?.googlePayAcceptUrl == null) {
                _effects.emit(UnifiedPaymentPageVMEffects.Failed("Authorization or Google Pay URL is missing"))
                return@launch
            }

            val googlePayUrl = currentState.googlePayUiConfig.googlePayAcceptUrl
            val accessToken = currentState.accessToken
            val response =
                googlePayAcceptInteractor.accept(googlePayUrl, accessToken, paymentDataJson)

            when (response) {
                is SDKHttpResponse.Failed -> _effects.emit(
                    UnifiedPaymentPageVMEffects.Failed(
                        error = "Google Pay accept failed: ${response.error.message}"
                    )
                )

                is SDKHttpResponse.Success -> _effects.emit(UnifiedPaymentPageVMEffects.Captured)
            }
        }
    }

    private suspend fun initiateCardPayment(
        makeCardPaymentRequest: MakeCardPaymentRequest,
        orderUrl: String,
    ) {
        val response = cardPaymentInteractor.makeCardPayment(makeCardPaymentRequest)

        if (response is CardPaymentResponse.Success) {
            when (response.paymentResponse.state) {
                "AUTHORISED" -> _effects.emit(UnifiedPaymentPageVMEffects.PaymentAuthorised)
                "PURCHASED" -> _effects.emit(UnifiedPaymentPageVMEffects.Purchased)
                "CAPTURED" -> _effects.emit(UnifiedPaymentPageVMEffects.Captured)
                "POST_AUTH_REVIEW" -> _effects.emit(UnifiedPaymentPageVMEffects.PostAuthReview)
                "AWAIT_3DS" -> {
                    try {
                        if (response.paymentResponse.isThreeDSecureTwo()) {
                            val request = threeDSecureFactory.buildThreeDSecureTwoDto(
                                paymentResponse = response.paymentResponse,
                                orderUrl = orderUrl,
                                paymentCookie = makeCardPaymentRequest.paymentCookie
                            )
                            _effects.emit(UnifiedPaymentPageVMEffects.InitiateThreeDSTwo(request))
                        } else {
                            val request =
                                threeDSecureFactory.buildThreeDSecureDto(paymentResponse = response.paymentResponse)
                            _effects.emit(UnifiedPaymentPageVMEffects.InitiateThreeDS(request))
                        }

                    } catch (e: IllegalArgumentException) {
                        _effects.emit(UnifiedPaymentPageVMEffects.Failed(e.message.orEmpty()))
                    }
                }

                "AWAITING_PARTIAL_AUTH_APPROVAL" -> {
                    response.paymentResponse.toIntent(makeCardPaymentRequest.paymentCookie).let { intent ->
                        startPartialAuth(intent)
                    }
                }

                "FAILED" -> _effects.emit(UnifiedPaymentPageVMEffects.Failed(" Payment Failed ${response.paymentResponse.threeDSOne?.summaryText.orEmpty()}"))
                else -> _effects.emit(UnifiedPaymentPageVMEffects.Failed("Unknown payment state: $uiState"))
            }
        } else {
            _effects.emit(UnifiedPaymentPageVMEffects.Failed((response as CardPaymentResponse.Error).error.message.orEmpty()))
        }
    }

    fun makeVisPayment(
        makeCardPaymentRequest: MakeCardPaymentRequest,
        selectedPlan: InstallmentPlan,
        orderUrl: String,
    ) {
        _uiState.update { UnifiedPaymentPageVMUiState.Loading(LoadingMessage.PAYMENT) }
        var visaRequest: VisaRequest? = null
        if (selectedPlan.frequency != PlanFrequency.PayInFull) {
            visaRequest = VisaRequest(
                planSelectionIndicator = true,
                vPlanId = selectedPlan.id,
                acceptedTAndCVersion = selectedPlan.terms?.version ?: 0
            )
        }
        viewModelScope.launch(dispatcher) {
            initiateCardPayment(
                makeCardPaymentRequest.copy(visaRequest = visaRequest),
                orderUrl = orderUrl
            )
        }
    }

    fun startPartialAuth(partialAuthIntent: PartialAuthIntent) {
        _uiState.update {
            UnifiedPaymentPageVMUiState.InitiatePartialAuth(partialAuthIntent)
        }
    }

    internal class Factory(private val cardPaymentsIntent: UnifiedPaymentPageRequest) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>, extras: CreationExtras
        ): T {
            val walletOptions =
                Wallet.WalletOptions.Builder()
                    .setEnvironment(cardPaymentsIntent.googlePayConfig.env())
                    .build();
            val httpClient = CoroutinesGatewayHttpClient()
            return UnifiedPaymentPageViewModel(
                cardPaymentsIntent = cardPaymentsIntent,
                authApiInteractor = AuthApiInteractor(httpClient, extras.requireApplication()),
                cardPaymentInteractor = CardPaymentInteractor(httpClient, extras.requireApplication()),
                visaInstalmentPlanInteractor = VisaInstallmentPlanInteractor(httpClient),
                getPayerIpInteractor = GetPayerIpInteractor(httpClient),
                threeDSecureFactory = ThreeDSecureFactory(),
                googlePayConfigFactory = GooglePayConfigFactory(
                    paymentsClient = Wallet.getPaymentsClient(
                        extras.requireApplication(), walletOptions
                    ),
                    googlePayJsonConfig = GooglePayJsonConfig(),
                    googlePayConfigInteractor = GooglePayConfigInteractor(httpClient),
                    merchantGatewayId = cardPaymentsIntent.googlePayConfig?.merchantGatewayId ?: ""
                ),
                googlePayAcceptInteractor = GooglePayAcceptInteractor(httpClient, extras.requireApplication()),
                getOrderApiInteractor = GetOrderApiInteractor(httpClient)
            ) as T
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CreationExtras.requireApplication(): Application {
    return requireNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
}