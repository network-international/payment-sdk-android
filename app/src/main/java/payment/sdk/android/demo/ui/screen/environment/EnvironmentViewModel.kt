package payment.sdk.android.demo.ui.screen.environment

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import payment.sdk.android.demo.data.DataStore
import payment.sdk.android.demo.data.DataStoreImpl
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.MerchantAttribute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.AppLanguage

class EnvironmentViewModel(
    private val dataStore: DataStore
) : ViewModel() {
    private var _state: MutableStateFlow<EnvironmentViewModelState> =
        MutableStateFlow(EnvironmentViewModelState())

    val state: StateFlow<EnvironmentViewModelState> = _state.asStateFlow()

    init {
        _state.update {
            EnvironmentViewModelState(
                environments = dataStore.getEnvironments(),
                selectedEnvironment = dataStore.getSelectedEnvironment(),
                merchantAttributes = dataStore.getMerchantAttributes(),
                orderAction = dataStore.getOrderAction()
            )
        }
    }

    fun saveMerchantAttribute(merchantAttribute: MerchantAttribute) {
        dataStore.saveMerchantAttribute(merchantAttribute)
        _state.update { it.copy(merchantAttributes = dataStore.getMerchantAttributes()) }
    }

    fun deleteMerchantAttribute(merchantAttribute: MerchantAttribute) {
        dataStore.deleteMerchantAttribute(merchantAttribute)
        _state.update { it.copy(merchantAttributes = dataStore.getMerchantAttributes()) }
    }

    fun onDeleteEnvironment(environment: Environment) {
        dataStore.deleteEnvironment(environment)
        _state.update { it.copy(environments = dataStore.getEnvironments()) }
    }

    fun saveEnvironment(environment: Environment) {
        if (dataStore.getEnvironments().isEmpty()) {
            dataStore.setSelectedEnvironment(environment)
        }
        dataStore.saveEnvironment(environment)
        _state.update {
            it.copy(
                environments = dataStore.getEnvironments(),
                selectedEnvironment = dataStore.getSelectedEnvironment()
            )
        }
    }

    fun onSelectEnvironment(environment: Environment) {
        dataStore.setSelectedEnvironment(environment)
        _state.update { it.copy(selectedEnvironment = environment) }
    }

    fun onOrderActionSelected(action: String) {
        dataStore.setOrderAction(action)
        _state.update { it.copy(orderAction = action) }
    }

    fun onOrderTypeSelected(type: String) {
        dataStore.setOrderType(type)
        _state.update { it.copy(orderType = type) }
    }

    fun updateMerchantAttribute(merchantAttribute: MerchantAttribute) {
        dataStore.updateMerchantAttribute(merchantAttribute)
        _state.update { it.copy(merchantAttributes = dataStore.getMerchantAttributes()) }
    }

    fun getCurrency() = dataStore.getCurrency()

    fun setCurrency(currency: AppCurrency) {
        dataStore.setCurrency(currency)
    }

    fun getLanguage() = dataStore.getLanguage()

    fun setLanguage(language: AppLanguage) {
        dataStore.setLanguage(language)
    }

    companion object {
        fun provideFactory(
            context: Activity,
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
                    return EnvironmentViewModel(dataStore = DataStoreImpl(context)) as T
                }
            }
    }
}