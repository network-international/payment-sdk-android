package payment.sdk.android.partialAuth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

class PartialAuthViewModel(
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private var _state: MutableSharedFlow<CardPaymentData> =
        MutableSharedFlow(replay = 1)

    val state: SharedFlow<CardPaymentData> = _state.asSharedFlow()

    fun submitRequest(url: String, accessToken: String) {
        viewModelScope.launch(dispatcher) {
            val response = httpClient.put(
                url, mapOf(
                    "Cookie" to accessToken,
                    "Accept" to "application/vnd.ni-payment.v2+json",
                    "Content-Type" to "application/vnd.ni-payment.v2+json"
                ), Body.Empty()
            )

            when (response) {
                is SDKHttpResponse.Failed -> _state.emit(
                    CardPaymentData(
                        CardPaymentData.STATUS_PAYMENT_FAILED,
                        response.error.message.orEmpty()
                    )
                )

                is SDKHttpResponse.Success -> {
                    val state =
                        JsonParser.parseString(response.body).asJsonObject.get("state").asString.orEmpty()
                    _state.emit(
                        when (state) {
                            "CAPTURED", "VERIFIED" -> CardPaymentData(CardPaymentData.STATUS_PAYMENT_CAPTURED)
                            "AUTHORISED" -> CardPaymentData(CardPaymentData.STATUS_PAYMENT_AUTHORIZED)
                            "PURCHASED" -> CardPaymentData(CardPaymentData.STATUS_PAYMENT_PURCHASED)
                            "POST_AUTH_REVIEW" -> CardPaymentData(CardPaymentData.STATUS_POST_AUTH_REVIEW)
                            "PARTIAL_AUTH_DECLINED" -> CardPaymentData(CardPaymentData.STATUS_PARTIAL_AUTH_DECLINED)
                            "PARTIAL_AUTH_DECLINE_FAILED" -> CardPaymentData(CardPaymentData.STATUS_PARTIAL_AUTH_DECLINE_FAILED)
                            "PARTIALLY_AUTHORISED" -> CardPaymentData(CardPaymentData.STATUS_PARTIALLY_AUTHORISED)
                            else -> CardPaymentData(CardPaymentData.STATUS_PAYMENT_FAILED)
                        }
                    )
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
                return PartialAuthViewModel(
                    CoroutinesGatewayHttpClient()
                ) as T
            }
        }
    }
}