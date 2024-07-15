package payment.sdk.android.cardpayment.partialAuth

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
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class PartialAuthViewModel(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private var _state: MutableStateFlow<PartialAuthVMState> =
        MutableStateFlow(PartialAuthVMState())

    val state: StateFlow<PartialAuthVMState> = _state.asStateFlow()

    private fun updateState(url: String, accessToken: String, successState: PartialAuthState) {
        _state.update { it.copy(state = PartialAuthState.LOADING) }
        viewModelScope.launch(dispatcher) {
            val response = httpClient.put(
                url, mapOf(
                    "Cookie" to accessToken,
                    "Accept" to "application/vnd.ni-payment.v2+json",
                    "Content-Type" to "application/vnd.ni-payment.v2+json"
                ), Body.Empty()
            )

            when (response) {
                is SDKHttpResponse.Failed -> _state.update { it.copy(state = PartialAuthState.ERROR) }
                is SDKHttpResponse.Success -> _state.update { it.copy(state = successState) }
            }
        }
    }

    fun accept(url: String, accessToken: String) {
        updateState(url, accessToken, PartialAuthState.SUCCESS)
    }

    fun decline(url: String, accessToken: String) {
        updateState(url, accessToken, PartialAuthState.DECLINED)
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                return PartialAuthViewModel(
                    CoroutinesGatewayHttpClient()
                ) as T
            }
        }
    }
}

enum class PartialAuthState {
    LOADING, INIT, SUCCESS, ERROR, DECLINED
}

data class PartialAuthVMState(
    val state: PartialAuthState = PartialAuthState.INIT
)