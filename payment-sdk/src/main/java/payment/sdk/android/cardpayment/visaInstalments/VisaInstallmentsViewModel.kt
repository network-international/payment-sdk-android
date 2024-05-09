package payment.sdk.android.cardpayment.visaInstalments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.cardpayment.visaInstalments.model.InstallmentPlan
import payment.sdk.android.cardpayment.visaInstalments.model.PlanFrequency
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstallmentsVMState
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstalmentActivityArgs
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.CardPaymentInteractor
import payment.sdk.android.core.interactor.CardPaymentResponse
import payment.sdk.android.core.interactor.GetPayerIpInteractor
import payment.sdk.android.core.interactor.SavedCardPaymentApiInteractor
import payment.sdk.android.core.interactor.SavedCardResponse
import payment.sdk.android.core.interactor.VisaRequest

class VisaInstallmentsViewModel(
    private val cardPaymentInteractor: CardPaymentInteractor,
    private val savedCardPaymentApiInteractor: SavedCardPaymentApiInteractor,
    private val getPayerIpInteractor: GetPayerIpInteractor,
    private val threeDSecureFactory: ThreeDSecureFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var _state: MutableStateFlow<VisaInstallmentsVMState> =
        MutableStateFlow(VisaInstallmentsVMState.Init)

    val state: StateFlow<VisaInstallmentsVMState> = _state.asStateFlow()

    fun init(
        args: VisaInstalmentActivityArgs
    ) {
        _state.update {
            VisaInstallmentsVMState.PlanSelection(
                paymentUrl = args.paymentUrl,
                newCardDto = args.newCard,
                savedCardDto = args.savedCard,
                paymentCookie = args.paymentCookie,
                orderUrl = args.orderUrl,
                installmentPlans = args.instalmentPlan,
                selectedPlan = null,
                isValid = false,
                savedCardUrl = args.savedCardUrl,
                accessToken = args.accessToken
            )
        }
    }

    fun onSelectPlan(selectedPlan: InstallmentPlan, state: VisaInstallmentsVMState.PlanSelection) {
        val isValid =
            selectedPlan.frequency == PlanFrequency.PayInFull || selectedPlan.termsAccepted
        _state.update {
            state.copy(isValid = isValid, selectedPlan = selectedPlan)
        }
    }

    fun makeCardPayment(
        plan: InstallmentPlan,
        state: VisaInstallmentsVMState.PlanSelection,
        payPageUrl: String,
        cvv: String?
    ) {
        _state.update { VisaInstallmentsVMState.Loading(LoadingMessage.PAYMENT) }

        var visaRequest: VisaRequest? = null
        if (plan.frequency != PlanFrequency.PayInFull) {
            visaRequest = VisaRequest(
                planSelectionIndicator = true,
                vPlanId = plan.id,
                acceptedTAndCVersion = plan.terms?.version ?: 0
            )
        }
        viewModelScope.launch(dispatcher) {
            val payerIp = getPayerIpInteractor.getPayerIp(payPageUrl)
            if (state.paymentUrl != null && state.newCardDto != null) {
                val response = cardPaymentInteractor.makeCardPayment(
                    paymentUrl = state.paymentUrl,
                    cardHolder = state.newCardDto.customerName,
                    pan = state.newCardDto.cardNumber,
                    cvv = state.newCardDto.cvv,
                    expiry = state.newCardDto.expiry,
                    paymentCookie = state.paymentCookie,
                    visaRequest = visaRequest
                )
                when (response) {
                    is CardPaymentResponse.Error -> updateFailed(response.error.message!!)
                    is CardPaymentResponse.Success -> {
                        executeThreeDS(
                            paymentResponse = response.paymentResponse,
                            orderUrl = state.orderUrl,
                            paymentCookie = state.paymentCookie
                        )
                    }
                }
            } else if (state.savedCardUrl != null && state.savedCardDto != null) {
                val response = savedCardPaymentApiInteractor.doSavedCardPayment(
                    savedCardUrl = state.savedCardUrl,
                    accessToken = state.accessToken,
                    cvv = cvv,
                    savedCard = state.savedCardDto.toSavedCard(),
                    payerIp = payerIp,
                    visaRequest = visaRequest
                )
                when (response) {
                    is SavedCardResponse.Error -> updateFailed(response.error.message!!)
                    is SavedCardResponse.Success -> {
                        _state.update { VisaInstallmentsVMState.Loading(LoadingMessage.THREE_DS) }
                        executeThreeDS(
                            paymentResponse = response.paymentResponse,
                            orderUrl = state.orderUrl,
                            paymentCookie = state.paymentCookie
                        )
                    }
                }
            }
        }
    }

    private fun executeThreeDS(
        paymentResponse: PaymentResponse,
        orderUrl: String,
        paymentCookie: String
    ) {
        when (paymentResponse.state) {
            "AUTHORISED" -> _state.update { VisaInstallmentsVMState.PaymentAuthorised }
            "PURCHASED" -> _state.update { VisaInstallmentsVMState.Purchased }
            "CAPTURED" -> _state.update { VisaInstallmentsVMState.Captured }
            "POST_AUTH_REVIEW" -> _state.update { VisaInstallmentsVMState.PostAuthReview }
            "AWAIT_3DS" -> {
                try {
                    if (paymentResponse.isThreeDSecureTwo()) {
                        val request =
                            threeDSecureFactory.buildThreeDSecureTwoDto(
                                paymentResponse = paymentResponse,
                                orderUrl = orderUrl,
                                paymentCookie = paymentCookie
                            )
                        _state.update {
                            VisaInstallmentsVMState.InitiateThreeDSTwo(
                                request
                            )
                        }
                    } else {
                        val request =
                            threeDSecureFactory.buildThreeDSecureDto(
                                paymentResponse = paymentResponse
                            )
                        _state.update {
                            VisaInstallmentsVMState.InitiateThreeDS(
                                request
                            )
                        }
                    }

                } catch (e: IllegalArgumentException) {
                    updateFailed(e.message!!)
                }
            }

            "FAILED" -> updateFailed("FAILED")
            else -> updateFailed("Unknown payment state: ${paymentResponse.state}")
        }
    }

    private fun updateFailed(message: String) {
        _state.update { VisaInstallmentsVMState.Failed(message) }
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val httpClient = CoroutinesGatewayHttpClient()
                return VisaInstallmentsViewModel(
                    CardPaymentInteractor(httpClient),
                    SavedCardPaymentApiInteractor(httpClient),
                    GetPayerIpInteractor(httpClient),
                    ThreeDSecureFactory()
                ) as T
            }
        }
    }
}