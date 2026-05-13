package payment.sdk.android.payments

import android.app.Application
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import java.util.Locale
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.wallet.Wallet
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.qpay.QPayLauncher
import payment.sdk.android.core.getQPayUrl
import payment.sdk.android.core.interactor.ClickToPayConfig
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.threedsecuretwo.webview.toIntent
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.model.PlanFrequency
import payment.sdk.android.cardpayment.widget.DateFormatter
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.CardMapping
import payment.sdk.android.core.Order
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
import payment.sdk.android.core.SliceRequest
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.CardPaymentInteractor
import payment.sdk.android.core.interactor.MakeCardPaymentRequest
import payment.sdk.android.core.interactor.CardPaymentResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.GetPayerIpInteractor
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.getSavedCardPaymentUrl
import payment.sdk.android.core.getSliceEligibilityCheckUrl
import payment.sdk.android.core.getVisEligibilityCheckUrl
import payment.sdk.android.core.interactor.GooglePayAcceptInteractor
import payment.sdk.android.core.interactor.GooglePayConfigInteractor
import payment.sdk.android.core.interactor.SavedCardPaymentApiInteractor
import payment.sdk.android.core.interactor.SavedCardPaymentApiRequest
import payment.sdk.android.core.interactor.SavedCardResponse
import payment.sdk.android.core.interactor.SliceEligibilityInteractor
import payment.sdk.android.core.interactor.SliceEligibilityResult
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
    private val sliceEligibilityInteractor: SliceEligibilityInteractor,
    private val getPayerIpInteractor: GetPayerIpInteractor,
    private val googlePayConfigFactory: GooglePayConfigFactory,
    private val threeDSecureFactory: ThreeDSecureFactory,
    private val googlePayAcceptInteractor: GooglePayAcceptInteractor,
    private val getOrderApiInteractor: GetOrderApiInteractor,
    private val savedCardPaymentApiInteractor: SavedCardPaymentApiInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var _uiState: MutableStateFlow<UnifiedPaymentPageVMUiState> =
        MutableStateFlow(UnifiedPaymentPageVMUiState.Init)

    val uiState: StateFlow<UnifiedPaymentPageVMUiState> = _uiState.asStateFlow()

    private var _effects = MutableSharedFlow<UnifiedPaymentPageVMEffects>(replay = 1)

    val effect = _effects.asSharedFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _sliceCheckState = MutableStateFlow<SliceCheckState>(SliceCheckState.Idle)
    val sliceCheckState: StateFlow<SliceCheckState> = _sliceCheckState.asStateFlow()

    private var sliceCheckJob: Job? = null

    private val _visCheckState = MutableStateFlow<VisCheckState>(VisCheckState.Idle)
    val visCheckState: StateFlow<VisCheckState> = _visCheckState.asStateFlow()

    private var visCheckJob: Job? = null
    private var lastVisCheckKey: String? = null

    var orderReference: String = ""
        private set

    private var _fetchedOrder: Order? = null
    val fetchedOrder: Order? get() = _fetchedOrder

    fun startGooglePayProcess() {
        Log.d(TAG, "startGooglePayProcess: _isProcessing → true")
        _isProcessing.value = true
    }

    fun setProcessingFinished() {
        Log.d(TAG, "setProcessingFinished: _isProcessing → false")
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
        _fetchedOrder = order

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

        // Configure QPay if `payment:qpay` link exists in the order AND currency is QAR.
        // QCB only supports QAR; the SDK gates here so the row never shows for other currencies.
        val qpayConfig = run {
            val qpayUrl = order.getQPayUrl() ?: return@run null
            if (!currencyCode.equals("QAR", ignoreCase = true)) return@run null
            val payPageUrl = cardPaymentsIntent.paymentUrl ?: return@run null
            QPayLauncher.Config(
                qpayUrl = qpayUrl,
                payPageUrl = payPageUrl,
                orderUrl = orderUrl,
                accessToken = accessToken,
                currencyCode = currencyCode
            )
        }

        val supportedCards = order.paymentMethods?.card.orEmpty()

        if (supportedCards.isEmpty()) {
            _effects.emit(UnifiedPaymentPageVMEffects.Failed("No supported card scheme found"))
            return
        }

        val isSamsungPayAvailable = supportedWallets.contains("SAMSUNG_PAY")

        orderReference = order.reference.orEmpty()

        val isLTR = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR

        _uiState.update {
            UnifiedPaymentPageVMUiState.Authorized(
                accessToken = accessToken,
                paymentCookie = paymentCookie,
                orderUrl = orderUrl,
                supportedCards = CardMapping.mapSupportedCards(supportedCards),
                googlePayUiConfig = googlePayConfig,
                isSamsungPayAvailable = isSamsungPayAvailable,
                showWallets = supportedWallets.contains("GOOGLE_PAY") || isSamsungPayAvailable || apm.contains("AANI") || clickToPayConfig != null,
                orderAmount = OrderAmount(amount, currencyCode).formattedCurrencyString2Decimal(isLTR),
                cardPaymentUrl = order.getCardPaymentUrl().orEmpty(),
                amount = amount,
                currencyCode = currencyCode,
                selfUrl = order.getSelfUrl().orEmpty(),
                locale = SDKConfig.getLanguage(),
                aaniConfig = aaniConfig,
                clickToPayConfig = clickToPayConfig,
                qpayConfig = qpayConfig,
                payerIp = payerIp,
                orderReference = order.reference.orEmpty(),
                savedCards = cardPaymentsIntent.savedCards,
                savedCardPaymentUrl = order.getSavedCardPaymentUrl(),
                orderItems = cardPaymentsIntent.orderItems,
                sliceEligibilityCheckUrl = order.getSliceEligibilityCheckUrl(),
                visEligibilityCheckUrl = order.getVisEligibilityCheckUrl()
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
        payerIp: String,
        sliceRequest: SliceRequest? = null,
        visaRequest: VisaRequest? = null,
        skipVisaPlansCheck: Boolean = false
    ) {
        val makeCardPaymentRequest = MakeCardPaymentRequest(
            payerIp = payerIp,
            paymentCookie = paymentCookie,
            cardHolder = cardholderName,
            expiry = DateFormatter.formatExpireDateForApi(expiry),
            cvv = cvv,
            paymentUrl = cardPaymentUrl,
            pan = cardNumber,
            sliceRequest = sliceRequest,
            visaRequest = visaRequest
        )
        _uiState.update { UnifiedPaymentPageVMUiState.Loading(LoadingMessage.PAYMENT) }
        viewModelScope.launch(dispatcher) {
            // The inline Vis selector (new flow) handles eligibility BEFORE Pay is tapped.
            // When the caller signals it has already run, skip the legacy post-tap getPlans
            // entirely so we don't trigger the full-screen ShowVisaPlans activity twice.
            if (visaRequest != null || skipVisaPlansCheck) {
                initiateCardPayment(
                    makeCardPaymentRequest = makeCardPaymentRequest,
                    orderUrl = orderUrl,
                )
                return@launch
            }
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
        Log.d(TAG, "acceptGooglePay: called, token length=${paymentDataJson.length}")
        viewModelScope.launch(dispatcher) {
            try {
                val currentState = uiState.value
                Log.d(TAG, "acceptGooglePay: currentState=${currentState::class.simpleName}")

                if (currentState !is UnifiedPaymentPageVMUiState.Authorized || currentState.googlePayUiConfig?.googlePayAcceptUrl == null) {
                    Log.w(TAG, "acceptGooglePay: invalid state or missing URL — isAuthorized=${currentState is UnifiedPaymentPageVMUiState.Authorized}, hasUrl=${(currentState as? UnifiedPaymentPageVMUiState.Authorized)?.googlePayUiConfig?.googlePayAcceptUrl != null}")
                    _isProcessing.value = false
                    Log.d(TAG, "acceptGooglePay: _isProcessing → false (bad state)")
                    _effects.emit(UnifiedPaymentPageVMEffects.Failed("Authorization or Google Pay URL is missing"))
                    Log.d(TAG, "acceptGooglePay: emitted Failed (bad state)")
                    return@launch
                }

                val googlePayUrl = currentState.googlePayUiConfig.googlePayAcceptUrl
                Log.d(TAG, "acceptGooglePay: calling accept API, url=$googlePayUrl")
                val response =
                    googlePayAcceptInteractor.accept(googlePayUrl, currentState.accessToken, paymentDataJson)
                Log.d(TAG, "acceptGooglePay: accept API response=${response::class.simpleName}")

                when (response) {
                    is SDKHttpResponse.Failed -> {
                        Log.e(TAG, "acceptGooglePay: API failed — ${response.error.message}", response.error)
                        _isProcessing.value = false
                        Log.d(TAG, "acceptGooglePay: _isProcessing → false (API failure)")
                        _effects.emit(
                            UnifiedPaymentPageVMEffects.Failed(
                                error = "Google Pay accept failed: ${response.error.message}"
                            )
                        )
                        Log.d(TAG, "acceptGooglePay: emitted Failed (API failure)")
                    }

                    is SDKHttpResponse.Success -> {
                        Log.d(TAG, "acceptGooglePay: API success, emitting Captured")
                        _effects.emit(UnifiedPaymentPageVMEffects.Captured)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "acceptGooglePay: uncaught exception — ${e.message}", e)
                _isProcessing.value = false
                Log.d(TAG, "acceptGooglePay: _isProcessing → false (exception)")
                _effects.emit(UnifiedPaymentPageVMEffects.Failed("Google Pay unexpected error: ${e.message}"))
            }
        }
    }

    fun makeSavedCardPayment(
        savedCard: SavedCard,
        savedCardPaymentUrl: String,
        accessToken: String,
        paymentCookie: String,
        orderUrl: String,
        payerIp: String,
        cvv: String?
    ) {
        _uiState.update { UnifiedPaymentPageVMUiState.Loading(LoadingMessage.PAYMENT) }
        viewModelScope.launch(dispatcher) {
            val request = SavedCardPaymentApiRequest(
                accessToken = accessToken,
                paymentCookie = paymentCookie,
                savedCardUrl = savedCardPaymentUrl,
                savedCard = savedCard,
                payerIp = payerIp,
                cvv = cvv?.takeIf { it.isNotBlank() }
            )
            when (val response = savedCardPaymentApiInteractor.doSavedCardPayment(request)) {
                is SavedCardResponse.Success -> {
                    when (response.paymentResponse.state) {
                        "AUTHORISED" -> _effects.emit(UnifiedPaymentPageVMEffects.PaymentAuthorised)
                        "PURCHASED" -> _effects.emit(UnifiedPaymentPageVMEffects.Purchased)
                        "CAPTURED" -> _effects.emit(UnifiedPaymentPageVMEffects.Captured)
                        "POST_AUTH_REVIEW" -> _effects.emit(UnifiedPaymentPageVMEffects.PostAuthReview)
                        "AWAIT_3DS" -> {
                            try {
                                if (response.paymentResponse.isThreeDSecureTwo()) {
                                    // Pull from the function parameters — uiState is Loading
                                    // here (we transitioned to it at the top of this fn) so
                                    // casting to Authorized would yield null and the cookie
                                    // would be empty, breaking the 3DS2 /authentications call.
                                    val dto = threeDSecureFactory.buildThreeDSecureTwoDto(
                                        paymentResponse = response.paymentResponse,
                                        orderUrl = orderUrl,
                                        paymentCookie = paymentCookie
                                    )
                                    _effects.emit(UnifiedPaymentPageVMEffects.InitiateThreeDSTwo(dto))
                                } else {
                                    val dto = threeDSecureFactory.buildThreeDSecureDto(response.paymentResponse)
                                    _effects.emit(UnifiedPaymentPageVMEffects.InitiateThreeDS(dto))
                                }
                            } catch (e: IllegalArgumentException) {
                                _effects.emit(UnifiedPaymentPageVMEffects.Failed(e.message.orEmpty()))
                            }
                        }
                        "FAILED" -> _effects.emit(UnifiedPaymentPageVMEffects.Failed("Saved card payment failed"))
                        else -> _effects.emit(UnifiedPaymentPageVMEffects.Failed("Unknown state: ${response.paymentResponse.state}"))
                    }
                }
                is SavedCardResponse.Error -> _effects.emit(UnifiedPaymentPageVMEffects.Failed(response.error.message.orEmpty()))
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

    fun checkSliceEligibility(
        eligibilityCheckUrl: String?,
        paymentCookie: String,
        pan: String,
        expiryRaw: String,
        visEligibilityCheckUrl: String? = null,
        selfUrl: String? = null,
        cardScheme: String? = null,
        isSavedCardToken: Boolean = false
    ) {
        sliceCheckJob?.cancel()
        // Saved-card flow already provides expiry in API format (YYYY-MM); manual entry passes
        // raw digits which need formatting.
        val expiry = if (isSavedCardToken) expiryRaw else DateFormatter.formatExpireDateForApi(expiryRaw)
        if (eligibilityCheckUrl == null) {
            _sliceCheckState.update { SliceCheckState.Unavailable }
            // Slice not configured — go straight to Vis if applicable.
            checkVisEligibility(visEligibilityCheckUrl, selfUrl, paymentCookie, pan, cardScheme, isSavedCardToken)
            return
        }
        sliceCheckJob = viewModelScope.launch(dispatcher) {
            _sliceCheckState.update { SliceCheckState.Checking }
            when (val result = sliceEligibilityInteractor.checkEligibility(eligibilityCheckUrl, paymentCookie, pan, expiry, isSavedCardToken)) {
                is SliceEligibilityResult.Success -> {
                    val offers = result.response.offers
                    if (offers.isNotEmpty()) {
                        _sliceCheckState.update { SliceCheckState.Available(offers) }
                        // Slice succeeded — clear any prior Vis state, Slice has priority.
                        resetVisCheck()
                    } else {
                        _sliceCheckState.update { SliceCheckState.Unavailable }
                        checkVisEligibility(visEligibilityCheckUrl, selfUrl, paymentCookie, pan, cardScheme, isSavedCardToken)
                    }
                }
                is SliceEligibilityResult.Error -> {
                    _sliceCheckState.update { SliceCheckState.Unavailable }
                    checkVisEligibility(visEligibilityCheckUrl, selfUrl, paymentCookie, pan, cardScheme, isSavedCardToken)
                }
            }
        }
    }

    fun resetSliceCheck() {
        sliceCheckJob?.cancel()
        _sliceCheckState.update { SliceCheckState.Idle }
        resetVisCheck()
    }

    /**
     * Visa Installments eligibility — only fires for Visa cards when the order has the
     * payment:vis-eligibility-check link. Manual entry sends the raw PAN in the `pan` field;
     * saved-card flows send the previously-issued `cardToken`. Keyed on (panOrToken) so we
     * don't refire while the user is editing other fields.
     */
    fun checkVisEligibility(
        visEligibilityCheckUrl: String?,
        selfUrl: String?,
        paymentCookie: String,
        cardTokenOrPan: String,
        cardScheme: String?,
        isSavedCardToken: Boolean = false
    ) {
        if (visEligibilityCheckUrl == null || selfUrl == null) {
            _visCheckState.update { VisCheckState.Unavailable }
            return
        }
        // Only fire for Visa cards.
        if (cardScheme?.equals("Visa", ignoreCase = true) != true) {
            _visCheckState.update { VisCheckState.Idle }
            return
        }
        if (cardTokenOrPan == lastVisCheckKey) return
        lastVisCheckKey = cardTokenOrPan

        visCheckJob?.cancel()
        visCheckJob = viewModelScope.launch(dispatcher) {
            _visCheckState.update { VisCheckState.Checking }
            when (val response = visaInstalmentPlanInteractor.getPlans(
                selfUrl = selfUrl,
                cardToken = if (isSavedCardToken) cardTokenOrPan else null,
                cardNumber = if (isSavedCardToken) null else cardTokenOrPan,
                token = paymentCookie
            )) {
                is VisaPlansResponse.Error -> _visCheckState.update { VisCheckState.Unavailable }
                is VisaPlansResponse.Success -> {
                    if (response.visaPlans.matchedPlans.isNotEmpty()) {
                        _visCheckState.update { VisCheckState.Available(response.visaPlans) }
                    } else {
                        _visCheckState.update { VisCheckState.Unavailable }
                    }
                }
            }
        }
    }

    fun resetVisCheck() {
        visCheckJob?.cancel()
        lastVisCheckKey = null
        _visCheckState.update { VisCheckState.Idle }
    }

    fun startPartialAuth(partialAuthIntent: PartialAuthIntent) {
        _uiState.update {
            UnifiedPaymentPageVMUiState.InitiatePartialAuth(partialAuthIntent)
        }
    }

    companion object {
        private const val TAG = "NI-SDK-GPay-Debug"
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
                sliceEligibilityInteractor = SliceEligibilityInteractor(httpClient),
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
                getOrderApiInteractor = GetOrderApiInteractor(httpClient),
                savedCardPaymentApiInteractor = SavedCardPaymentApiInteractor(httpClient, extras.requireApplication())
            ) as T
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CreationExtras.requireApplication(): Application {
    return requireNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
}