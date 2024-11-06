package payment.sdk.android.aaniPay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import payment.sdk.android.aaniPay.model.AaniIDType
import payment.sdk.android.aaniPay.model.AaniPayVMState
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.AaniPayRequest
import payment.sdk.android.core.MobileNumber
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.AaniPayApiInterator
import payment.sdk.android.core.interactor.AaniPayApiResponse
import payment.sdk.android.core.interactor.AaniPoolingApiInteractor

internal class AaniPayViewModel(
    private val aaniPayApiInterator: AaniPayApiInterator,
    private val aaniPoolingApiInteractor: AaniPoolingApiInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private var _state: MutableStateFlow<AaniPayVMState> =
        MutableStateFlow(AaniPayVMState.Init)

    val state: StateFlow<AaniPayVMState> = _state.asStateFlow()

    fun onSubmit(
        args: AaniPayLauncher.Config,
        accessToken: String,
        alias: AaniIDType,
        value: String,
        payerIp: String
    ) {
        _state.update { AaniPayVMState.Loading(LoadingMessage.PAYMENT) }
        viewModelScope.launch(dispatcher) {
            val response = aaniPayApiInterator.makePayment(
                args.anniPaymentLink,
                accessToken,
                createRequest(alias = alias, value = value, payerIp = payerIp).toBody()
            )

            when (response) {
                is AaniPayApiResponse.Error -> _state.update {
                    AaniPayVMState.Error(response.error.message.orEmpty())
                }

                is AaniPayApiResponse.Success -> {
                    _state.update {
                        AaniPayVMState.Pooling(
                            amount = response.aaniPayResponse.amount.value ?: 0.0,
                            currencyCode = response.aaniPayResponse.amount.currencyCode ?: "AED",
                            deepLink = response.aaniPayResponse.aani?.deepLinkUrl ?: ""
                        )
                    }
                    startPooling(
                        response.aaniPayResponse._links.aaniStatus.href.orEmpty(),
                        accessToken
                    )
                }
            }
        }
    }

    private suspend fun startPooling(url: String, accessToken: String) {
        do {
            delay(6000)
            val state = aaniPoolingApiInteractor.startPooling(url, accessToken)
            when (state) {
                "CAPTURED", "PURCHASED" -> {
                    _state.update { AaniPayVMState.Success }
                }

                "FAILED" -> {
                    _state.update { AaniPayVMState.Error(message = "Failed") }
                }
            }

        } while (state == "PENDING")
    }

    private fun createRequest(
        alias: AaniIDType,
        value: String,
        payerIp: String
    ) = with(
        AaniPayRequest(
            aliasType = alias.label,
            payerIp = payerIp,
        )
    ) {
        when (alias) {
            AaniIDType.MOBILE_NUMBER -> copy(mobileNumber = MobileNumber(number = value))
            AaniIDType.EMIRATES_ID -> copy(
                emiratesId = value.replace(
                    "(\\w{3})(\\w{4})(\\w{7})(\\w{1})".toRegex(), "$1-$2-$3-$4"
                )
            )

            AaniIDType.PASSPORT_ID -> copy(passportId = value)
            AaniIDType.EMAIL_ID -> copy(emailId = value)
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

                return AaniPayViewModel(
                    aaniPayApiInterator = AaniPayApiInterator(httpClient),
                    aaniPoolingApiInteractor = AaniPoolingApiInteractor(httpClient)
                ) as T
            }
        }
    }
}