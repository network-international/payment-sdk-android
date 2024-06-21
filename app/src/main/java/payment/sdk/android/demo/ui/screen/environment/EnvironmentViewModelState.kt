package payment.sdk.android.demo.ui.screen.environment

import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.MerchantAttribute

data class EnvironmentViewModelState(
    val environments: List<Environment> = listOf(),
    val selectedEnvironment: Environment? = null,
    val merchantAttributes: List<MerchantAttribute> = listOf(),
    val language: String = "en",
    val orderAction: String = "SALE",
)
