package payment.sdk.android.cardpayment.savedCard

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
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto
import payment.sdk.android.core.Utils.getQueryParameter
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.SavedCardPaymentApiInteractor
import payment.sdk.android.core.interactor.SavedCardResponse

class SavedPaymentViewModel(
    private val authApiInteractor: AuthApiInteractor,
    private val savedCardPaymentApiInteractor: SavedCardPaymentApiInteractor,
    private val threeDSecureFactory: ThreeDSecureFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var _state: MutableStateFlow<SavedCardPaymentState> =
        MutableStateFlow(SavedCardPaymentState.Init)

    val state: StateFlow<SavedCardPaymentState> = _state.asStateFlow()

    fun authorize(authUrl: String, paymentUrl: String, recaptureCsc: Boolean) {
        val authCode = paymentUrl.getQueryParameter("code")
        if (authCode.isNullOrBlank()) {
            _state.update {
                SavedCardPaymentState.Failed("Auth code not found")
            }
            return
        }
        _state.update { SavedCardPaymentState.Loading("Authorizing Payment") }
        viewModelScope.launch(dispatcher) {
            val authResponse = authApiInteractor.authenticate(
                authUrl = authUrl,
                authCode = authCode
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
        viewModelScope.launch(dispatcher) {
            val response = savedCardPaymentApiInteractor.doSavedCardPayment(
                accessToken = accessToken,
                savedCardUrl = savedCardUrl,
                savedCard = savedCard.toSavedCard(),
                cvv = cvv
            )
            when (response) {
                is SavedCardResponse.Error -> _state.update {
                    SavedCardPaymentState.Failed(
                        response.error.message!!
                    )
                }

                is SavedCardResponse.Success -> {
                    try {
                        if (response.paymentResponse.isThreeDSecureTwo()) {
                            val request = threeDSecureFactory.buildThreeDSecureTwoDto(
                                paymentResponse = response.paymentResponse,
                                orderUrl = orderUrl,
                                paymentCookie = paymentCookie
                            )
                            _state.update { SavedCardPaymentState.InitiateThreeDSTwo(request) }
                        } else {
                            val request =
                                threeDSecureFactory.buildThreeDSecureDto(paymentResponse = response.paymentResponse)
                            _state.update { SavedCardPaymentState.InitiateThreeDS(request) }
                        }

                    } catch (e: IllegalArgumentException) {
                        _state.update {
                            SavedCardPaymentState.Failed(
                                e.message!!
                            )
                        }
                    }
                }
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
                    AuthApiInteractor(httpClient),
                    SavedCardPaymentApiInteractor(httpClient),
                    ThreeDSecureFactory()
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

    data class InitiateThreeDS(val threeDSecureDto: ThreeDSecureDto) : SavedCardPaymentState()

    data class InitiateThreeDSTwo(val threeDSecureTwoDto: ThreeDSecureTwoDto) :
        SavedCardPaymentState()

    data class Failed(val error: String) : SavedCardPaymentState()
}