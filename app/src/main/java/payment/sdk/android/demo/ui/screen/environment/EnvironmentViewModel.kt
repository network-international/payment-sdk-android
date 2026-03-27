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
import payment.sdk.android.demo.model.OrderAction
import payment.sdk.android.demo.model.OrderType
import payment.sdk.android.demo.model.Region
import payment.sdk.android.SDKConfig
import payment.sdk.android.sdk.R

class EnvironmentViewModel(
    private val dataStore: DataStore
) : ViewModel() {
    private var _state: MutableStateFlow<EnvironmentViewModelState> =
        MutableStateFlow(EnvironmentViewModelState())

    val state: StateFlow<EnvironmentViewModelState> = _state.asStateFlow()

    // SDK Color definitions: key for SharedPreferences, resource ID, default hex
    data class SDKColorDef(val key: String, val resId: Int, val defaultHex: String, val label: String)

    val sdkColorDefs = listOf(
        SDKColorDef("sdk_color_button", R.color.payment_sdk_pay_button_background_color, "#0055DE", "Button"),
        SDKColorDef("sdk_color_button_text", R.color.payment_sdk_pay_button_text_color, "#FFFFFF", "Button Text"),
        SDKColorDef("sdk_color_button_disabled", R.color.payment_sdk_button_disabled_background_color, "#D1D1D6", "Button Disabled"),
        SDKColorDef("sdk_color_button_disabled_text", R.color.payment_sdk_button_disabled_text_color, "#8E8E93", "Button Disabled Text"),
        SDKColorDef("sdk_color_toolbar", R.color.payment_sdk_toolbar_color, "#0055DE", "Toolbar"),
        SDKColorDef("sdk_color_toolbar_text", R.color.payment_sdk_toolbar_text_color, "#FFFFFF", "Toolbar Text"),
        SDKColorDef("sdk_color_input_field_bg", R.color.payment_sdk_input_field_background_color, "#FFFFFF", "Input Field BG"),
        SDKColorDef("sdk_color_auth_view_bg", R.color.payment_sdk_auth_view_background_color, "#F2F2F2", "Auth View BG"),
        SDKColorDef("sdk_color_auth_view_indicator", R.color.payment_sdk_auth_view_indicator_color, "#3A8377", "Auth Indicator"),
        SDKColorDef("sdk_color_auth_view_label", R.color.payment_sdk_auth_view_label_color, "#000000", "Auth Label"),
        SDKColorDef("sdk_color_3ds_view_bg", R.color.payment_sdk_3ds_view_background_color, "#FFFFFF", "3DS View BG"),
        SDKColorDef("sdk_color_3ds_view_label", R.color.payment_sdk_3ds_view_label_color, "#000000", "3DS Label"),
        SDKColorDef("sdk_color_3ds_view_indicator", R.color.payment_sdk_3ds_view_indicator_color, "#3A8377", "3DS Indicator"),
    )

    // Mutable state for SDK color hex values
    private val _sdkColors = MutableStateFlow<Map<String, String>>(emptyMap())
    val sdkColors: StateFlow<Map<String, String>> = _sdkColors.asStateFlow()

    init {
        _state.update {
            EnvironmentViewModelState(
                environments = dataStore.getEnvironments(),
                selectedEnvironment = dataStore.getSelectedEnvironment(),
                merchantAttributes = dataStore.getMerchantAttributes(),
                orderAction = dataStore.getOrderAction()
            )
        }
        // Load saved SDK colors
        val colors = mutableMapOf<String, String>()
        sdkColorDefs.forEach { def ->
            colors[def.key] = dataStore.getSDKColor(def.key, def.defaultHex)
        }
        _sdkColors.value = colors
        applySDKColors()
    }

    fun setSDKColor(key: String, hex: String) {
        dataStore.setSDKColor(key, hex)
        _sdkColors.update { it.toMutableMap().apply { put(key, hex) } }
        applySDKColors()
    }

    fun applySDKColors() {
        sdkColorDefs.forEach { def ->
            val hex = _sdkColors.value[def.key] ?: def.defaultHex
            parseHexColor(hex)?.let { colorInt ->
                SDKConfig.setColor(def.resId, colorInt)
            }
        }
    }

    private fun parseHexColor(hex: String): Int? {
        return try {
            val sanitized = hex.removePrefix("#")
            if (sanitized.length == 6) {
                (0xFF shl 24) or sanitized.toLong(16).toInt()
            } else null
        } catch (e: Exception) {
            null
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

    fun updateEnvironment(environment: Environment) {
        dataStore.updateEnvironment(environment)
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

    fun getOrderAction(): OrderAction {
        val code = dataStore.getOrderAction()
        return OrderAction.entries.firstOrNull { it.code == code } ?: OrderAction.SALE
    }

    fun setOrderAction(action: OrderAction) {
        dataStore.setOrderAction(action.code)
        _state.update { it.copy(orderAction = action.code) }
    }

    fun getOrderType(): OrderType {
        val code = dataStore.getOrderType()
        return OrderType.entries.firstOrNull { it.code == code } ?: OrderType.SINGLE
    }

    fun setOrderType(type: OrderType) {
        dataStore.setOrderType(type.code)
        _state.update { it.copy(orderType = type.code) }
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

    fun getRegion() = dataStore.getRegion()

    fun setRegion(region: Region) {
        dataStore.setRegion(region)
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