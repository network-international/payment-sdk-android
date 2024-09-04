package payment.sdk.android.googlepay

import android.app.Application
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import payment.sdk.android.core.Utils.getQueryParameter
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants

internal class GooglePayViewModel(
    private val config: GooglePayLauncher.Config,
    private val authApiInteractor: AuthApiInteractor,
    private val paymentsClient: PaymentsClient,
    private val googlePayJsonConfig: GooglePayJsonConfig = GooglePayJsonConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var _state: MutableStateFlow<GooglePayVMState> =
        MutableStateFlow(GooglePayVMState.Init)

    val state: StateFlow<GooglePayVMState> = _state.asStateFlow()


    init {
        config.payPageUrl.getQueryParameter("code")?.let {
            handleAuthentication(it)
        }
    }

    private fun handleAuthentication(authCode: String) {
        viewModelScope.launch(dispatcher) {
            when (val authResponse = authenticate(authCode)) {
                is AuthResponse.Error -> handleAuthError(authResponse)
                is AuthResponse.Success -> handleAuthSuccess(authResponse)
            }
        }
    }

    private suspend fun authenticate(authCode: String): AuthResponse {
        return authApiInteractor.authenticate(
            authUrl = config.authUrl,
            authCode = authCode
        )
    }

    private fun handleAuthError(error: AuthResponse.Error) {
        _state.update { GooglePayVMState.Error(error.error.message ?: "Auth Failed") }
    }

    private fun handleAuthSuccess(success: AuthResponse.Success) {
        _state.update {
            GooglePayVMState.Authorized(
                success.getAccessToken(),
                success.getPaymentCookie(),
                success.orderUrl
            )
        }
    }

    private fun startGooglePay() {
        viewModelScope.launch(dispatcher) {
            resolveLoadPaymentDataTask().fold(
                onSuccess = {
                },
                onFailure = {

                }
            )
        }
    }

    private suspend fun resolveLoadPaymentDataTask(): Result<Task<PaymentData>> {
        return runCatching {
            googlePayJsonConfig.getPaymentDataRequest(100L).toString()
        }.mapCatching { json ->
            PaymentDataRequest.fromJson(json)
        }.map { request ->
            paymentsClient.loadPaymentData(request)
        }
    }

    internal class Factory(private val args: GooglePayLauncher.Config) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val application = extras.requireApplication()
            val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()

            val authApiInteractor = AuthApiInteractor(CoroutinesGatewayHttpClient())
            return GooglePayViewModel(
                config = args,
                authApiInteractor = authApiInteractor,
                paymentsClient = Wallet.getPaymentsClient(application, walletOptions)
            ) as T
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CreationExtras.requireApplication(): Application {
    return requireNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
}