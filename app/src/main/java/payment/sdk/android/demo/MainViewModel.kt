package payment.sdk.android.demo

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import payment.sdk.android.PaymentClient
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.api.CoroutinesGatewayHttpClient
import payment.sdk.android.demo.data.DataStore
import payment.sdk.android.demo.data.DataStoreImpl
import payment.sdk.android.demo.http.ApiServiceAdapter
import payment.sdk.android.demo.http.CreateOrderApiInteractor
import payment.sdk.android.demo.http.GetOrderApiInteractor
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.demo.model.PaymentOrderAmount
import payment.sdk.android.demo.model.Product
import java.util.Locale

@Keep
class MainViewModel(
    private val paymentClient: PaymentClient,
    private val createOrderApiInteractor: CreateOrderApiInteractor,
    private val getOrderApiInteractor: GetOrderApiInteractor,
    private val dataStore: DataStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var _state: MutableStateFlow<MainViewModelState> =
        MutableStateFlow(MainViewModelState())

    val state: StateFlow<MainViewModelState> = _state.asStateFlow()

    init {
        _state.update {
            MainViewModelState(
                state = MainViewModelStateType.INIT,
                products = dataStore.getProducts(),
                selectedProducts = listOf(),
                isSamsungPayAvailable = false,
                total = 0.0,
                savedCard = dataStore.getSavedCard(),
                savedCards = dataStore.getSavedCards(),
                currency = dataStore.getCurrency().code
            )
        }
        paymentClient.getSupportedPaymentMethods(object :
            PaymentClient.SupportedPaymentTypesListener {
            override fun onReady(supportedPaymentTypes: List<PaymentClient.PaymentType>) {
                supportedPaymentTypes.forEach { type ->
                    if (PaymentClient.PaymentType.SAMSUNG_PAY == type) {
                        _state.update {
                            it.copy(isSamsungPayAvailable = true)
                        }
                    }
                }
            }
        })
    }

    fun createOrderRequest(savedCard: SavedCard? = null): OrderRequest {
        return OrderRequest(
            action = dataStore.getOrderAction(),
            amount = PaymentOrderAmount(
                value = state.value.total,
                currencyCode = dataStore.getCurrency().code
            ),
            language = Locale.getDefault().language,
            merchantAttributes = dataStore.getMerchantAttributes()
                .filter { it.isActive }
                .associate { it.key to it.value },
            savedCard = savedCard
        )
    }

    fun onCardPaymentResponse(data: CardPaymentData) {
        when (data.code) {
            CardPaymentData.STATUS_PAYMENT_AUTHORIZED,
            CardPaymentData.STATUS_PAYMENT_PURCHASED,
            CardPaymentData.STATUS_PAYMENT_CAPTURED -> {
                saveCardFromOrder(state.value.orderReference)
            }

            CardPaymentData.STATUS_PAYMENT_FAILED, CardPaymentData.STATUS_GENERIC_ERROR ->
                _state.update {
                    it.copy(state = MainViewModelStateType.ERROR, message = data.reason.orEmpty())
                }

            CardPaymentData.STATUS_POST_AUTH_REVIEW -> _state.update {
                it.copy(state = MainViewModelStateType.PAYMENT_POST_AUTH_REVIEW)
            }

            CardPaymentData.STATUS_PARTIAL_AUTH_DECLINED -> _state.update {
                it.copy(state = MainViewModelStateType.PAYMENT_PARTIAL_AUTH_DECLINED)
            }

            CardPaymentData.STATUS_PARTIAL_AUTH_DECLINE_FAILED -> _state.update {
                it.copy(state = MainViewModelStateType.PAYMENT_PARTIAL_AUTH_DECLINE_FAILED)
            }

            CardPaymentData.STATUS_PARTIALLY_AUTHORISED -> _state.update {
                it.copy(state = MainViewModelStateType.PAYMENT_PARTIALLY_AUTHORISED)
            }

            else -> _state.update { it.copy(state = MainViewModelStateType.PAYMENT_FAILED) }
        }
    }

    private fun getEnvironment(): Environment? {
        return dataStore.getSelectedEnvironment() ?: run {
            _state.update {
                it.copy(state = MainViewModelStateType.ERROR, message = "No environment selected")
            }
            null
        }
    }

    private fun saveCardFromOrder(orderReference: String?) {
        _state.update {
            it.copy(state = MainViewModelStateType.LOADING, message = "Fetching Order...")
        }
        getEnvironment()?.let { environment ->
            viewModelScope.launch(dispatcher) {
                val result = getOrderApiInteractor.getOrder(environment, orderReference)
                if (result is Result.Success) {
                    result.data.embedded?.payment?.firstOrNull()?.savedCard?.let { savedCard ->
                        if (dataStore.getSavedCards().isEmpty()) {
                            dataStore.setSavedCard(savedCard)
                        }
                        dataStore.saveCard(savedCard)
                    }
                }
                _state.update {
                    it.copy(
                        state = MainViewModelStateType.PAYMENT_SUCCESS,
                        message = "Payment Successful",
                        savedCard = dataStore.getSavedCard(),
                        savedCards = dataStore.getSavedCards()
                    )
                }
            }
        }
    }

    fun onCardPaymentCancelled() {
        _state.update { it.copy(state = MainViewModelStateType.PAYMENT_CANCELLED) }
    }

    fun closeDialog() {
        _state.update { it.copy(state = MainViewModelStateType.INIT) }
    }

    fun onAddProduct(product: Product) {
        dataStore.addProduct(product)
        _state.update { it.copy(products = dataStore.getProducts()) }
    }

    fun onDeleteProduct(product: Product) {
        dataStore.deleteProduct(product)
        _state.update { it.copy(products = dataStore.getProducts()) }
    }

    fun deleteSavedCard(savedCard: SavedCard) {
        dataStore.deleteSavedCard(savedCard)
        _state.update { it.copy(savedCards = dataStore.getSavedCards()) }
    }

    fun onSelectProduct(product: Product) {
        _state.update {
            val newItems = it.selectedProducts.toMutableList()
            newItems.toggle(product)
            it.copy(
                selectedProducts = newItems.toList(),
                total = newItems.sumOf { p -> p.amount }
            )
        }
    }

    fun setSavedCard(savedCard: SavedCard) {
        dataStore.setSavedCard(savedCard)
        _state.update { it.copy(savedCard = savedCard) }
    }

    fun onRefresh() {
        _state.update {
            it.copy(
                products = dataStore.getProducts(),
                savedCard = dataStore.getSavedCard(),
                savedCards = dataStore.getSavedCards(),
                currency = dataStore.getCurrency().code
            )
        }
    }

    fun onSuccess() {
        _state.update { it.copy(state = MainViewModelStateType.PAYMENT_SUCCESS) }
    }

    fun onFailure(error: String) {
        _state.update { it.copy(state = MainViewModelStateType.ERROR, message = error) }
    }

    fun onCanceled() {
        _state.update { it.copy(state = MainViewModelStateType.PAYMENT_CANCELLED) }
    }

    fun createOrder(paymentType: PaymentType, orderRequest: OrderRequest) {
        getEnvironment()?.let { environment ->
            _state.update {
                it.copy(state = MainViewModelStateType.LOADING, message = "Creating Order...")
            }
            viewModelScope.launch(dispatcher) {
                val result = createOrderApiInteractor.createOrder(environment, orderRequest)

                when (result) {
                    is Result.Error -> {
                        _state.update {
                            it.copy(state = MainViewModelStateType.ERROR, message = result.message)
                        }
                    }

                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                state = MainViewModelStateType.PAYMENT_PROCESSING,
                                paymentType = paymentType,
                                order = result.data,
                                orderReference = result.data.reference
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {

        const val CARD_PAYMENT_REQUEST_CODE = 123

        fun provideFactory(
            activity: Activity,
            owner: SavedStateRegistryOwner,
            defaultArgs: Bundle? = null,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    val client = PaymentClient(activity, "FOC")
                    return MainViewModel(
                        paymentClient = client,
                        createOrderApiInteractor = CreateOrderApiInteractor(
                            ApiServiceAdapter(
                                CoroutinesGatewayHttpClient()
                            )
                        ),
                        getOrderApiInteractor = GetOrderApiInteractor(
                            ApiServiceAdapter(
                                CoroutinesGatewayHttpClient()
                            )
                        ),
                        dataStore = DataStoreImpl(activity)
                    ) as T
                }
            }
    }
}