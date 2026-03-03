package payment.sdk.android.aaniPay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
import payment.sdk.android.core.interactor.AaniQrApiInteractor
import payment.sdk.android.core.interactor.AaniQrCreateResponse

internal class AaniPayViewModel(
    private val aaniPayApiInterator: AaniPayApiInterator,
    private val aaniPoolingApiInteractor: AaniPoolingApiInteractor,
    private val aaniQrApiInteractor: AaniQrApiInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private var _state: MutableStateFlow<AaniPayVMState> =
        MutableStateFlow(AaniPayVMState.Init)

    val state: StateFlow<AaniPayVMState> = _state.asStateFlow()
    @Volatile private var pollingJob: Job? = null
    private var qrCodeId: String? = null
    private var qrTransactionId: String? = null
    private var qrUrl: String? = null
    private var qrAccessToken: String? = null

    fun onSubmit(
        args: AaniPayLauncher.Config,
        accessToken: String,
        alias: AaniIDType,
        value: String,
        payerIp: String
    ) {
        if (alias == AaniIDType.QR_CODE) {
            onQrSubmit(args, accessToken)
            return
        }
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
                        response.aaniPayResponse._links?.aaniStatus?.href.orEmpty(),
                        accessToken
                    )
                }
            }
        }
    }


    private var lastQrArgs: AaniPayLauncher.Config? = null
    private var lastQrAmount: Double = 0.0
    private var lastQrCurrencyCode: String = "AED"

    private fun onQrSubmit(args: AaniPayLauncher.Config, accessToken: String) {
        lastQrArgs = args
        _state.update { AaniPayVMState.QrLoading }
        viewModelScope.launch(dispatcher) {
            val response = aaniQrApiInteractor.createQr(
                    args.anniQrPaymentLink,
                    accessToken
            )
            when (response) {
                is AaniQrCreateResponse.Error -> {
                    val amount = lastQrAmount
                    val currency = lastQrCurrencyCode
                    _state.update {
                        AaniPayVMState.QrFailed(amount = amount, currencyCode = currency)
                    }
                }

                is AaniQrCreateResponse.Success -> {
                    qrCodeId = response.aaniPayResponse.aani?.qrCodeId.orEmpty()
                    qrTransactionId = response.aaniPayResponse.aani?.qrCodeTransactionId.orEmpty()
                    qrUrl = args.anniQrPaymentLink
                    qrAccessToken = accessToken
                    lastQrAmount = response.aaniPayResponse.amount.value ?: 0.0
                    lastQrCurrencyCode = response.aaniPayResponse.amount.currencyCode ?: "AED"
                    _state.update {
                        AaniPayVMState.QrDisplay(
                                amount = response.aaniPayResponse.amount.value ?: 0.0,
                                currencyCode = response.aaniPayResponse.amount.currencyCode ?: "AED",
                                qrContent = response.aaniPayResponse.aani?.deepLinkUrl ?: ""
                        )
                    }
                    pollingJob = viewModelScope.launch(dispatcher) {
                        startQrPolling(
                                args.anniQrPaymentLink,
                                accessToken,
                                qrCodeId.orEmpty(),
                                qrTransactionId.orEmpty()
                        )
                    }
                }
            }
        }
    }

    fun cancelQr() {
        pollingJob?.cancel()
        val url = qrUrl ?: return
        val codeId = qrCodeId ?: return
        val transId = qrTransactionId ?: return
        val token = qrAccessToken ?: return
        viewModelScope.launch(dispatcher) {
            aaniQrApiInteractor.cancelQr(url, token, codeId, transId)
        }
        _state.update { current ->
            if (current is AaniPayVMState.Success || current is AaniPayVMState.Error) current
            else AaniPayVMState.Cancelled
        }
    }

    fun onQrExpired() {
        pollingJob?.cancel()
        _state.update {
            AaniPayVMState.QrExpired(amount = lastQrAmount, currencyCode = lastQrCurrencyCode)
        }
    }

    fun retryQr() {
        val args = lastQrArgs ?: return
        val token = qrAccessToken ?: return
        onQrSubmit(args, token)
    }

    fun retryPayment() {
        _state.update { AaniPayVMState.Init }
    }

    private suspend fun startQrPolling(
            url: String,
            accessToken: String,
            qrCodeId: String,
            qrTransactionId: String
    ) {
        val terminalStates = setOf("CAPTURED", "PURCHASED", "FAILED")
        do {
            delay(5000)
            val state = aaniQrApiInteractor.pollQrStatus(url, accessToken, qrCodeId, qrTransactionId)
            // Guard: if cancelQr() was called while the HTTP call was in-flight, stop here
            // without applying the result to the state.
            currentCoroutineContext().ensureActive()
            when (state) {
                "CAPTURED", "PURCHASED" -> {
                    _state.update { AaniPayVMState.Success }
                }
                "FAILED" -> {
                    _state.update { AaniPayVMState.Error(message = "Failed") }
                }
            }
        } while (state !in terminalStates)
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
            AaniIDType.QR_CODE -> throw IllegalStateException("QR_CODE does not use createRequest")
        }
    }

    companion object {

        // Must match the UI countdown in AaniPayTimerScreen (5 minutes)
        internal const val POLLING_TIMEOUT_MS = 5L * 60 * 1_000

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val httpClient = CoroutinesGatewayHttpClient()

                return AaniPayViewModel(
                    aaniPayApiInterator = AaniPayApiInterator(httpClient),
                    aaniPoolingApiInteractor = AaniPoolingApiInteractor(httpClient),
                    aaniQrApiInteractor = AaniQrApiInteractor(httpClient)
                ) as T
            }
        }
    }
}
