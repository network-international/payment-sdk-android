package payment.sdk.android.cardpayment.aaniPay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.aaniPay.model.AaniPayRequest
import payment.sdk.android.cardpayment.aaniPay.model.AaniPayVMState
import payment.sdk.android.cardpayment.aaniPay.model.MobileNumber
import payment.sdk.android.core.Utils.getQueryParameter
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse

class AaniPayViewModel(
    private val authApiInteractor: AuthApiInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private var _state: MutableStateFlow<AaniPayVMState> =
        MutableStateFlow(AaniPayVMState())

    val state: StateFlow<AaniPayVMState> = _state.asStateFlow()

    fun authorize(
        authUrl: String,
        paymentUrl: String,
    ) {
        val authCode = paymentUrl.getQueryParameter("code")
        if (authCode.isNullOrBlank()) {

            return
        }
        viewModelScope.launch(dispatcher) {
            val authResponse = authApiInteractor.authenticate(
                authUrl = authUrl,
                authCode = authCode
            )
            when (authResponse) {
                is AuthResponse.Error -> {

                }

                is AuthResponse.Success -> {

                }
            }
        }
    }

    fun submitMobileNumber(mobileNumber: MobileNumber) {
        submit(AaniPayRequest(aliasType = "mobilenumer", mobileNumber = mobileNumber, source = "", backLink = "", payerIp = ""))
    }

    fun submitPassportId(passportId: String) {

    }

    fun submit(aaniPayRequest: AaniPayRequest) {

    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val httpClient = CoroutinesGatewayHttpClient()

                return AaniPayViewModel(
                    AuthApiInteractor(httpClient)
                ) as T
            }
        }
    }
}