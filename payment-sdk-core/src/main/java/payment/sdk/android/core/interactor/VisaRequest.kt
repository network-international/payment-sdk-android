package payment.sdk.android.core.interactor

data class VisaRequest(
    val planSelectionIndicator: Boolean,
    val acceptedTAndCVersion: Int,
    val vPlanId: String
)