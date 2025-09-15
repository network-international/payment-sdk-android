package payment.sdk.android.demo.ui.screen.environment

import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.MerchantAttribute
import payment.sdk.android.visaInstalments.model.PlanFrequency

data class EnvironmentViewModelState(
    val environments: List<Environment> = listOf(),
    val selectedEnvironment: Environment? = null,
    val merchantAttributes: List<MerchantAttribute> = listOf(),
    val language: String = "en",
    val orderAction: String = "SALE",
    val orderType: String = "SINGLE",
    val recurringType: String = "FIXED",
    val tenure: Int? = null,
    val frequency: String = "HOURLY"
)
