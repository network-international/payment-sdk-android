package payment.sdk.android.savedCard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.cardpayment.threedsecuretwo.webview.PartialAuthIntent
import payment.sdk.android.cardpayment.threedsecuretwo.webview.toIntent
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.Order
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.Utils.getQueryParameter
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.getSavedCardPaymentUrl
import payment.sdk.android.core.getSelfUrl
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.GetPayerIpInteractor
import payment.sdk.android.core.interactor.SavedCardPaymentApiInteractor
import payment.sdk.android.core.interactor.SavedCardPaymentApiRequest
import payment.sdk.android.core.interactor.SavedCardResponse
import payment.sdk.android.core.interactor.VisaInstallmentPlanInteractor
import payment.sdk.android.core.interactor.VisaPlansResponse
import payment.sdk.android.core.interactor.VisaRequest
import payment.sdk.android.savedCard.model.SavedCardPaymentState
import payment.sdk.android.savedCard.model.SavedCardPaymentsVMEffects
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.model.PlanFrequency

internal class SavedPaymentViewModel(
    private val authApiInteractor: AuthApiInteractor,
    private val savedCardPaymentApiInteractor: SavedCardPaymentApiInteractor,
    private val getPayerIpInteractor: GetPayerIpInteractor,
    private val visaInstalmentPlanInteractor: VisaInstallmentPlanInteractor,
    private val getOrderApiInteractor: GetOrderApiInteractor,
    private val threeDSecureFactory: ThreeDSecureFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var _state: MutableStateFlow<SavedCardPaymentState> =
        MutableStateFlow(SavedCardPaymentState.Init)

    val state: StateFlow<SavedCardPaymentState> = _state.asStateFlow()

    private var _effects = MutableSharedFlow<SavedCardPaymentsVMEffects>(replay = 1)

    val effect = _effects.asSharedFlow()

    fun authorize(payPageUrl: String, authorizationUrl: String, cvv: String?) {
        _state.update { SavedCardPaymentState.Loading(LoadingMessage.AUTH) }
        viewModelScope.launch(dispatcher) {
            val authCode = payPageUrl.getQueryParameter("code")
            if (authCode.isNullOrBlank()) {
                _effects.emit(SavedCardPaymentsVMEffects.Failed("Auth code not found"))
                return@launch
            }
            val authResponse = authApiInteractor.authenticate(
                authUrl = authorizationUrl,
                authCode = authCode
            )
            when (authResponse) {
                is AuthResponse.Error -> _effects.emit(
                    SavedCardPaymentsVMEffects.Failed(
                        authResponse.error.message ?: "Auth Failed"
                    )
                )

                is AuthResponse.Success -> {
                    getOrder(
                        orderUrl = authResponse.orderUrl,
                        paymentCookie = authResponse.getPaymentCookie(),
                        accessToken = authResponse.getAccessToken(),
                        payPageUrl = payPageUrl,
                        cvv = cvv
                    )
                }
            }
        }
    }

    private suspend fun checkForVisaEligibility(
        matchedCandidates: List<Order.MatchedCandidates>,
        cardToken: String,
        paymentCookie: String,
        selfLink: String
    ): VisaPlans? {
        return when {
            matchedCandidates.firstOrNull { it.cardToken == cardToken }?.eligibilityStatus == Order.MatchedCandidates.MATCHED_CANDIDATES_ELIGIBLE -> {
                when (val visaResponse = visaInstalmentPlanInteractor.getPlans(
                    selfUrl = selfLink,
                    token = paymentCookie,
                    cardToken = cardToken
                )) {
                    is VisaPlansResponse.Error -> null
                    is VisaPlansResponse.Success -> visaResponse.visaPlans
                }
            }

            else -> null
        }
    }

    suspend fun getOrder(
        orderUrl: String,
        accessToken: String,
        paymentCookie: String,
        payPageUrl: String,
        cvv: String?
    ) {
        val order = requireNotNull(getOrderApiInteractor.getOrder(orderUrl, accessToken)) {
            _effects.emit(SavedCardPaymentsVMEffects.Failed("Failed to fetch order details"))
            return
        }

        val payerIp = getPayerIpInteractor.getPayerIp(payPageUrl).orEmpty()

        val amount =
            order.amount?.value ?: return emitMissingFieldEffect("Failed to fetch order amount")

        val currencyCode = order.amount?.currencyCode
            ?: return emitMissingFieldEffect("Failed to fetch order currencyCode")

        val matchedCandidates = order.savedCardVisMatchedCandidates?.matchedCandidates ?: listOf()

        val savedCard = order.savedCard ?: return emitMissingFieldEffect("Saved card not found")

        val savedCardUrl = order.getSavedCardPaymentUrl()
            ?: return emitMissingFieldEffect("Saved card payment url not found")

        val selfUrl = order.getSelfUrl() ?: return emitMissingFieldEffect("Self url not found")

        val orderAmount = OrderAmount(amount, currencyCode)
        val visaPlans = checkForVisaEligibility(
            matchedCandidates = matchedCandidates,
            cardToken = savedCard.cardToken,
            paymentCookie = paymentCookie,
            selfLink = selfUrl
        )
        val savedCardPaymentRequest = SavedCardPaymentApiRequest(
            accessToken = accessToken,
            savedCardUrl = savedCardUrl,
            savedCard = savedCard,
            cvv = cvv,
            payerIp = payerIp
        )
        val captureCvvState = SavedCardPaymentState.CaptureCvv(
            paymentCookie = paymentCookie,
            orderUrl = orderUrl,
            visaPlans = visaPlans,
            orderAmount = orderAmount,
            savedCardPaymentRequest = savedCardPaymentRequest
        )
        if (visaPlans != null && visaPlans.matchedPlans.isNotEmpty()) {
            if (savedCard.recaptureCsc && cvv == null) {
                _state.update { captureCvvState }
            } else {
                _state.update {
                    SavedCardPaymentState.ShowVisaPlans(
                        savedCardPaymentRequest = savedCardPaymentRequest,
                        paymentCookie = paymentCookie,
                        orderUrl = orderUrl,
                        cardNumber = savedCard.maskedPan,
                        visaPlans = visaPlans,
                        orderAmount = orderAmount
                    )
                }
            }

        } else {
            if (savedCard.recaptureCsc && cvv == null) {
                _state.update { captureCvvState }
            } else {
                initiatePayment(savedCardPaymentRequest, orderUrl, paymentCookie)
            }
        }
    }

    fun doSavedCardPayment(
        orderUrl: String,
        paymentCookie: String,
        orderAmount: OrderAmount,
        savedCardPaymentRequest: SavedCardPaymentApiRequest,
        visaPlans: VisaPlans? = null
    ) {
        _state.update { SavedCardPaymentState.Loading(LoadingMessage.PAYMENT) }
        viewModelScope.launch(dispatcher) {
            if (visaPlans != null && visaPlans.matchedPlans.isNotEmpty()) {
                _state.update {
                    SavedCardPaymentState.ShowVisaPlans(
                        savedCardPaymentRequest = savedCardPaymentRequest,
                        paymentCookie = paymentCookie,
                        orderUrl = orderUrl,
                        cardNumber = savedCardPaymentRequest.savedCard.maskedPan,
                        visaPlans = visaPlans,
                        orderAmount = orderAmount,
                    )
                }
                return@launch
            }
            initiatePayment(savedCardPaymentRequest, orderUrl, paymentCookie)
        }
    }

    fun initiateVisPayment(
        selectedPlan: InstallmentPlan,
        savedCardPaymentRequest: SavedCardPaymentApiRequest,
        orderUrl: String,
        paymentCookie: String
    ) {
        var visaRequest: VisaRequest? = null
        if (selectedPlan.frequency != PlanFrequency.PayInFull) {
            visaRequest = VisaRequest(
                planSelectionIndicator = true,
                vPlanId = selectedPlan.id,
                acceptedTAndCVersion = selectedPlan.terms?.version ?: 0
            )
        }
        viewModelScope.launch(dispatcher) {
            initiatePayment(
                savedCardPaymentRequest.copy(visaRequest = visaRequest),
                orderUrl = orderUrl,
                paymentCookie = paymentCookie
            )
        }
    }

    private suspend fun initiatePayment(
        savedCardPaymentRequest: SavedCardPaymentApiRequest,
        orderUrl: String,
        paymentCookie: String
    ) {
        val response = savedCardPaymentApiInteractor.doSavedCardPayment(savedCardPaymentRequest)
        when (response) {
            is SavedCardResponse.Error -> emitFailedEffect(
                response.error.message ?: "Saved card Payment Failed"
            )

            is SavedCardResponse.Success -> handleSuccessResponse(
                response.paymentResponse,
                orderUrl,
                paymentCookie
            )
        }
    }

    private suspend fun handleSuccessResponse(
        paymentResponse: PaymentResponse,
        orderUrl: String,
        paymentCookie: String
    ) {
        when (paymentResponse.state) {
            "AUTHORISED" -> _effects.emit(SavedCardPaymentsVMEffects.PaymentAuthorised)
            "PURCHASED" -> _effects.emit(SavedCardPaymentsVMEffects.Purchased)
            "CAPTURED" -> _effects.emit(SavedCardPaymentsVMEffects.Captured)
            "POST_AUTH_REVIEW" -> _effects.emit(SavedCardPaymentsVMEffects.PostAuthReview)
            "AWAIT_3DS" -> initiate3DSecure(paymentResponse, orderUrl, paymentCookie)
            "AWAITING_PARTIAL_AUTH_APPROVAL" -> {
                paymentResponse.toIntent(paymentCookie).let { intent ->
                    initiatePartialAuth(intent)
                }
            }

            "FAILED" -> emitFailedEffect("Payment Failed")
            else -> emitFailedEffect("Unknown payment state: ${paymentResponse.state}")
        }
    }

    private suspend fun initiate3DSecure(
        paymentResponse: PaymentResponse,
        orderUrl: String,
        paymentCookie: String
    ) {
        try {
            if (paymentResponse.isThreeDSecureTwo()) {
                val request = threeDSecureFactory.buildThreeDSecureTwoDto(
                    paymentResponse = paymentResponse,
                    orderUrl = orderUrl,
                    paymentCookie = paymentCookie
                )
                _effects.emit(SavedCardPaymentsVMEffects.InitiateThreeDSTwo(request))
            } else {
                val request =
                    threeDSecureFactory.buildThreeDSecureDto(paymentResponse = paymentResponse)
                _effects.emit(SavedCardPaymentsVMEffects.InitiateThreeDS(request))
            }

        } catch (e: IllegalArgumentException) {
            updateFailed(e.message ?: "IllegalArgumentException")
        }
    }

    private suspend fun emitFailedEffect(message: String) {
        _effects.emit(SavedCardPaymentsVMEffects.Failed(message))
    }

    private suspend fun emitMissingFieldEffect(field: String) {
        emitFailedEffect("Failed to fetch $field")
    }

    fun initiatePartialAuth(partialAuthIntent: PartialAuthIntent) {
        _state.update {
            SavedCardPaymentState.InitiatePartialAuth(partialAuthIntent)
        }
    }

    private suspend fun updateFailed(message: String) {
        _effects.emit(SavedCardPaymentsVMEffects.Failed(message))
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val httpClient = CoroutinesGatewayHttpClient()

                return SavedPaymentViewModel(
                    authApiInteractor = AuthApiInteractor(httpClient),
                    savedCardPaymentApiInteractor = SavedCardPaymentApiInteractor(httpClient),
                    getPayerIpInteractor = GetPayerIpInteractor(httpClient),
                    visaInstalmentPlanInteractor = VisaInstallmentPlanInteractor(httpClient),
                    getOrderApiInteractor = GetOrderApiInteractor(httpClient),
                    threeDSecureFactory = ThreeDSecureFactory()
                ) as T
            }
        }
    }
}

internal fun SavedCard.isAmex(): Boolean = scheme == "AMERICAN_EXPRESS"
