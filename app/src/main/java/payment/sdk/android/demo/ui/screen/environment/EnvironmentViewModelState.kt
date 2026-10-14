package payment.sdk.android.demo.ui.screen.environment

import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.MerchantAttribute
import payment.sdk.android.demo.model.Region

data class EnvironmentViewModelState(
    val environments: List<Environment> = listOf(),
    val selectedEnvironment: Environment? = null,
    val merchantAttributes: List<MerchantAttribute> = listOf(),
    val language: String = "en",
    val orderAction: String = "SALE",
    val orderType: String = "SINGLE",
    /** QA override of the four outlet values; see [payment.sdk.android.demo.model.EnvironmentOverride]. */
    val override: OverrideState = OverrideState(),
)

data class OverrideState(
    val enabled: Boolean = false,
    val region: Region = Region.UAE,
    val currency: AppCurrency = AppCurrency.AED,
    val orderAction: String = "SALE",
    val orderType: String = "SINGLE",
)
