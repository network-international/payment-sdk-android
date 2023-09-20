package payment.sdk.android.cardpayment.savedCard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoConfig
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoRequest
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.AuthRepository
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.SavedCardPaymentRepository
import payment.sdk.android.core.interactor.SavedCardResponse

class SavedPaymentViewModel(
    private val authRepository: AuthRepository,
    private val savedCardPaymentRepository: SavedCardPaymentRepository
) : ViewModel() {

    private var _state: MutableStateFlow<SavedCardPaymentState> =
        MutableStateFlow(SavedCardPaymentState.Init)

    val state: StateFlow<SavedCardPaymentState> = _state.asStateFlow()

    fun authorize(authUrl: String, paymentUrl: String, recaptureCsc: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { SavedCardPaymentState.Loading("Authorizing Payment") }
            val authResponse = authRepository.run(
                authUrl = authUrl,
                paymentUrl = paymentUrl
            )
            when (authResponse) {
                is AuthResponse.Error -> _state.update {
                    SavedCardPaymentState.Failed(authResponse.error.message!!)
                }

                is AuthResponse.Success -> _state.update {
                    if (recaptureCsc) {
                        SavedCardPaymentState.CaptureCvv(
                            accessToken = authResponse.getAccessToken(),
                            paymentCookie = authResponse.getPaymentCookie(),
                            orderUrl = authResponse.orderUrl
                        )
                    } else {
                        SavedCardPaymentState.Authorized(
                            accessToken = authResponse.getAccessToken(),
                            paymentCookie = authResponse.getPaymentCookie(),
                            orderUrl = authResponse.orderUrl
                        )
                    }
                }
            }
        }
    }

    fun doSavedCardPayment(
        accessToken: String,
        savedCardUrl: String,
        savedCard: SavedCardDto,
        cvv: String?,
        orderUrl: String,
        paymentCookie: String
    ) {
        _state.update { SavedCardPaymentState.Loading("Initiating Payment") }
        viewModelScope.launch(Dispatchers.IO) {
            when (val response = savedCardPaymentRepository.run(
                accessToken = accessToken,
                savedCardUrl = savedCardUrl,
                savedCard = savedCard.toSavedCard(),
                cvv = cvv
            )) {
                is SavedCardResponse.Failed -> _state.update {
                    SavedCardPaymentState.Failed(
                        response.error.message!!
                    )
                }

                is SavedCardResponse.Success -> {
                    try {
                        initiateThreeDS(
                            paymentResponse = response.paymentResponse,
                            orderUrl = orderUrl,
                            paymentCookie = paymentCookie
                        )
                    } catch (e: IllegalArgumentException) {
                        SavedCardPaymentState.Failed(
                            e.message!!
                        )
                    }
                }
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    fun initiateThreeDS(
        paymentResponse: PaymentResponse,
        paymentCookie: String,
        orderUrl: String
    ) {
        val threeDSecureTwoConfig = ThreeDSecureTwoConfig.buildFromPaymentResponse(paymentResponse)
        if (threeDSecureTwoConfig.directoryServerID != null &&
            threeDSecureTwoConfig.threeDSMessageVersion != null &&
            threeDSecureTwoConfig.threeDSTwoAuthenticationURL != null &&
            threeDSecureTwoConfig.threeDSTwoChallengeResponseURL != null
        ) {
            val threeDSecureRequest =
                ThreeDSecureTwoRequest.buildFromPaymentResponse(paymentResponse)

            _state.update {
                SavedCardPaymentState.InitiateThreeDSTwo(
                    paymentCookie = paymentCookie,
                    orderUrl = orderUrl,
                    directoryServerID = requireNotNull(paymentResponse.threeDSTwo?.directoryServerID) {
                        "directoryServerID not found"
                    },
                    orderRef = requireNotNull(paymentResponse.orderReference) {
                        "order ref not found"
                    },
                    paymentReference = requireNotNull(paymentResponse.reference) {
                        "Payment reference not found"
                    },
                    outletRef = requireNotNull(paymentResponse.outletId) {
                        "outlet id not found"
                    },
                    threeDSMessageVersion = requireNotNull(paymentResponse.threeDSTwo?.messageVersion) {
                        "threeDSMessageVersion not found"
                    },
                    threeDSMethodData = threeDSecureRequest.threeDSMethodData,
                    threeDSMethodNotificationURL = threeDSecureRequest.threeDSMethodNotificationURL,
                    threeDSMethodURL = paymentResponse.threeDSTwo?.threeDSMethodURL,
                    threeDSServerTransID = paymentResponse.threeDSTwo?.threeDSServerTransID,
                    threeDSTwoAuthenticationURL = requireNotNull(
                        paymentResponse.links?.threeDSAuthenticationsUrl?.href
                    ) {
                        "threeDSTwoAuthenticationURL not found"
                    },
                    threeDSTwoChallengeResponseURL = requireNotNull(
                        paymentResponse.links?.threeDSChallengeResponseUrl?.href
                    ) {
                        "3ds challenge response url not found"
                    }
                )
            }
        } else {
            _state.update {
                SavedCardPaymentState.InitiateThreeDS(
                    acsMd = requireNotNull(paymentResponse.threeDSOne?.acsMd) {
                        "ThreeDS one acsMd not found"
                    },
                    acsPaReq = requireNotNull(paymentResponse.threeDSOne?.acsPaReq) {
                        "ThreeDS one acsPaReq not found"
                    },
                    acsUrl = requireNotNull(paymentResponse.threeDSOne?.acsUrl) {
                        "ThreeDS one acs url not found"
                    },
                    threeDSOneUrl = requireNotNull(paymentResponse.links?.threeDSOneUrl?.href) {
                        "ThreeDS one url not found"
                    }
                )
            }
        }
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
                    AuthRepository(httpClient),
                    SavedCardPaymentRepository(httpClient)
                ) as T
            }
        }
    }

}


sealed class SavedCardPaymentState {
    object Init : SavedCardPaymentState()

    data class Loading(val message: String) : SavedCardPaymentState()

    data class Authorized(
        val accessToken: String,
        val paymentCookie: String,
        val orderUrl: String
    ) : SavedCardPaymentState()

    data class CaptureCvv(
        val accessToken: String,
        val paymentCookie: String,
        val orderUrl: String
    ) : SavedCardPaymentState()

    data class InitiateThreeDS(
        val acsUrl: String,
        val acsPaReq: String,
        val acsMd: String,
        val threeDSOneUrl: String,
    ) : SavedCardPaymentState()

    data class InitiateThreeDSTwo(
        val threeDSMethodData: String?,
        val threeDSMethodNotificationURL: String,
        val threeDSMethodURL: String?,
        val threeDSServerTransID: String?,
        val paymentCookie: String,
        val threeDSTwoAuthenticationURL: String,
        val directoryServerID: String,
        val threeDSMessageVersion: String,
        val threeDSTwoChallengeResponseURL: String,
        val outletRef: String,
        val orderRef: String,
        val orderUrl: String,
        val paymentReference: String
    ) : SavedCardPaymentState()

    data class Failed(val error: String) : SavedCardPaymentState()
}