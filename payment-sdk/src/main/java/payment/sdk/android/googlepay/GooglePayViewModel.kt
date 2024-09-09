package payment.sdk.android.googlepay

import android.app.Application
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.tasks.CancellationTokenSource
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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import payment.sdk.android.core.GooglePayConfigResponse
import payment.sdk.android.core.interactor.GooglePayConfigInteractor
import java.util.concurrent.Executor
import kotlin.coroutines.resume

internal class GooglePayViewModel(
    private val authApiInteractor: AuthApiInteractor,
    private val googlePayConfigInteractor: GooglePayConfigInteractor,
    private val googlePayJsonConfig: GooglePayJsonConfig = GooglePayJsonConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var _state: MutableStateFlow<GooglePayVMState> =
        MutableStateFlow(GooglePayVMState.Init)

    val state: StateFlow<GooglePayVMState> = _state.asStateFlow()

    fun getGooglePayConfig(accessToken: String, currencyCode: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = runCatching {
                requireNotNull(
                    googlePayConfigInteractor.getConfig(
                        "https://api-gateway-dev.ngenius-payments.com/config/outlets/b4a6e21a-6168-4e16-bd93-995625cab406/configs/google-pay",
                        accessToken
                    )
                )
            }.getOrElse {
                return@launch
            }
            getLoadPaymentDataTask(config, currencyCode, amount)
        }
    }

    private fun getLoadPaymentDataTask(
        googlePayConfigResponse: GooglePayConfigResponse,
        currencyCode: String,
        amount: Double
    ) {
        val paymentDataRequestJson =
            googlePayJsonConfig.create(
                googlePayConfigResponse = googlePayConfigResponse,
                currencyCode = currencyCode,
                amount = amount
            )
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson)
        _state.update {
            GooglePayVMState.Submit(
                request,
                googlePayJsonConfig.baseCardPaymentMethod(
                    googlePayConfigResponse.allowedPaymentMethods,
                    googlePayConfigResponse.allowedAuthMethods
                ).toString()
            )
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
                return GooglePayViewModel(
                    authApiInteractor = AuthApiInteractor(httpClient),
                    googlePayConfigInteractor = GooglePayConfigInteractor(httpClient)
                ) as T
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CreationExtras.requireApplication(): Application {
    return requireNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
}

internal suspend fun <T> Task<T>.awaitTask(cancellationTokenSource: CancellationTokenSource? = null): Task<T> {
    return if (isComplete) {
        this
    } else {
        suspendCancellableCoroutine { cont ->
            // Run the callback directly to avoid unnecessarily scheduling on the main thread.
            addOnCompleteListener(DirectExecutor, cont::resume)

            cancellationTokenSource?.let { cancellationSource ->
                cont.invokeOnCancellation { cancellationSource.cancel() }
            }
        }
    }
}

/**
 * An [Executor] that just directly executes the [Runnable].
 */
private object DirectExecutor : Executor {
    override fun execute(r: Runnable) {
        r.run()
    }
}