package payment.sdk.android.googlepay

import androidx.annotation.Keep
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
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.Utils.getQueryParameter
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import payment.sdk.android.core.getGooglePayConfigUrl
import payment.sdk.android.core.getGooglePayUrl
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.GooglePayAcceptInteractor
import payment.sdk.android.core.interactor.GooglePayConfigInteractor
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.payments.requireApplication

@Keep
internal class GooglePayViewModel(
    private val config: GooglePayLauncher.Config,
    private val authApiInteractor: AuthApiInteractor,
    private val getOrderApiInteractor: GetOrderApiInteractor,
    private val googlePayConfigFactory: GooglePayConfigFactory,
    private val googlePayAcceptInteractor: GooglePayAcceptInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadingMessage = MutableStateFlow(LoadingMessage.AUTH)
    val loadingMessage: StateFlow<LoadingMessage> = _loadingMessage.asStateFlow()

    private val _effects = MutableSharedFlow<GooglePayVMEffects>(replay = 1)
    val effect = _effects.asSharedFlow()

    private var accessToken: String? = null
    private var orderUrl: String? = null
    private var googlePayAcceptUrl: String? = null

    fun start() {
        _loading.value = true
        _loadingMessage.value = LoadingMessage.AUTH
        viewModelScope.launch(dispatcher) {
            val authCode = config.payPageUrl.getQueryParameter("code")
            if (authCode.isNullOrBlank()) {
                finish(GooglePayLauncher.Result.Failed("Authorization code is missing from the pay page URL"))
                return@launch
            }
            when (val authResponse = authApiInteractor.authenticate(
                authUrl = config.gatewayAuthorizationUrl,
                authCode = authCode
            )) {
                is AuthResponse.Error -> {
                    finish(GooglePayLauncher.Result.Failed(authResponse.error.message.orEmpty()))
                }
                is AuthResponse.Success -> {
                    loadOrderAndPrepareSheet(
                        orderUrl = authResponse.orderUrl,
                        accessToken = authResponse.getAccessToken()
                    )
                }
            }
        }
    }

    private suspend fun loadOrderAndPrepareSheet(orderUrl: String, accessToken: String) {
        _loadingMessage.value = LoadingMessage.LOADING_ORDER
        val order = getOrderApiInteractor.getOrder(orderUrl, accessToken)
        if (order == null) {
            finish(GooglePayLauncher.Result.Failed("Failed to fetch order details"))
            return
        }

        val supportedWallets = order.paymentMethods?.wallet.orEmpty()
        val acceptUrl = order.getGooglePayUrl()
        if (!supportedWallets.contains("GOOGLE_PAY") || acceptUrl.isNullOrBlank()) {
            finish(GooglePayLauncher.Result.Failed("Google Pay is not enabled on this order"))
            return
        }

        val amount = order.amount?.value
        val currencyCode = order.amount?.currencyCode
        if (amount == null || currencyCode.isNullOrBlank()) {
            finish(GooglePayLauncher.Result.Failed("Order amount or currency is missing"))
            return
        }

        val uiConfig = googlePayConfigFactory.checkGooglePayConfig(
            googlePayConfigUrl = order.getGooglePayConfigUrl(),
            accessToken = accessToken,
            amount = amount,
            currencyCode = currencyCode,
            googlePayAcceptUrl = acceptUrl
        )
        if (uiConfig == null || !uiConfig.canUseGooglePay) {
            finish(GooglePayLauncher.Result.Failed("Google Pay is not available on this device"))
            return
        }

        this.accessToken = accessToken
        this.orderUrl = orderUrl
        this.googlePayAcceptUrl = uiConfig.googlePayAcceptUrl
        _effects.emit(GooglePayVMEffects.LaunchSheet(uiConfig))
    }

    fun acceptGooglePay(paymentDataJson: String) {
        _loading.value = true
        _loadingMessage.value = LoadingMessage.PAYMENT
        viewModelScope.launch(dispatcher) {
            val token = accessToken
            val acceptUrl = googlePayAcceptUrl
            if (token.isNullOrBlank() || acceptUrl.isNullOrBlank()) {
                finish(GooglePayLauncher.Result.Failed("Authorization or Google Pay URL is missing"))
                return@launch
            }
            try {
                when (val response = googlePayAcceptInteractor.accept(acceptUrl, token, paymentDataJson)) {
                    is SDKHttpResponse.Failed -> {
                        finish(
                            GooglePayLauncher.Result.Failed(
                                "Google Pay accept failed: ${response.error.message}"
                            )
                        )
                    }
                    is SDKHttpResponse.Success -> {
                        val state = resolvePaymentState(response.body, orderUrl, token)
                        finish(WalletPaymentStateMapper.toGooglePayResult(state))
                    }
                }
            } catch (e: Exception) {
                finish(GooglePayLauncher.Result.Failed("Google Pay unexpected error: ${e.message}"))
            }
        }
    }

    fun onUserCancelled() {
        viewModelScope.launch {
            finish(GooglePayLauncher.Result.Cancelled)
        }
    }

    private suspend fun resolvePaymentState(
        acceptBody: String,
        orderUrl: String?,
        accessToken: String
    ): String? {
        WalletPaymentStateMapper.parseState(acceptBody)?.let { return it }
        if (orderUrl.isNullOrBlank()) return null
        return getOrderApiInteractor.getOrder(orderUrl, accessToken)
            ?.embedded?.payment?.firstOrNull()?.state
    }

    private suspend fun finish(result: GooglePayLauncher.Result) {
        _loading.value = false
        _effects.emit(GooglePayVMEffects.Finished(result))
    }

    internal class Factory(private val config: GooglePayLauncher.Config) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(config.googlePayConfig.env())
                .build()
            val httpClient = CoroutinesGatewayHttpClient()
            val application = extras.requireApplication()
            return GooglePayViewModel(
                config = config,
                authApiInteractor = AuthApiInteractor(httpClient, application),
                getOrderApiInteractor = GetOrderApiInteractor(httpClient),
                googlePayConfigFactory = GooglePayConfigFactory(
                    paymentsClient = Wallet.getPaymentsClient(application, walletOptions),
                    googlePayJsonConfig = GooglePayJsonConfig(),
                    googlePayConfigInteractor = GooglePayConfigInteractor(httpClient),
                    merchantGatewayId = config.googlePayConfig.merchantGatewayId
                ),
                googlePayAcceptInteractor = GooglePayAcceptInteractor(httpClient, application)
            ) as T
        }
    }
}

internal sealed class GooglePayVMEffects {
    data class LaunchSheet(val uiConfig: GooglePayUiConfig) : GooglePayVMEffects()
    data class Finished(val result: GooglePayLauncher.Result) : GooglePayVMEffects()
}
