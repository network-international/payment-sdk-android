package payment.sdk.android.cardpayment.partialAuth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.gson.JsonParser
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

    fun submitRequest(url: String, accessToken: String) {
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
                is SDKHttpResponse.Failed -> _state.update {
                    it.copy(
                        state = PartialAuthState.ERROR,
                        message = response.error.message
                    )
                }

                is SDKHttpResponse.Success -> {
                    val state =
                        JsonParser.parseString(response.body).asJsonObject.get("state").asString.orEmpty()
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: String) {
        _state.update {
            it.copy(
                state = when (state) {
                    "CAPTURED", "AUTHORISED", "VERIFIED", "PURCHASED" -> PartialAuthState.SUCCESS
                    "PARTIAL_AUTH_DECLINED" -> PartialAuthState.DECLINED
                    "PARTIAL_AUTH_DECLINE_FAILED" -> PartialAuthState.ERROR
                    "PARTIALLY_AUTHORISED" -> PartialAuthState.PARTIALLY_AUTHORISED
                    else -> PartialAuthState.ERROR
                }
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
                return PartialAuthViewModel(
                    CoroutinesGatewayHttpClient()
                ) as T
            }
        }
    }
}

enum class PartialAuthState {
    LOADING, INIT, SUCCESS, ERROR, DECLINED, PARTIALLY_AUTHORISED
}

data class PartialAuthVMState(
    val state: PartialAuthState = PartialAuthState.INIT,
    val message: String? = null
)