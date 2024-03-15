package payment.sdk.android.cardpayment.visaInstalments

data class InstalmentPlan(
    val currency: String,
    val price: String,
    val fee: String,
    val total: String,
    val numberOfInstallments: Int,
    val frequency: PlanFrequency
)

enum class PlanFrequency(val value: String) {
    MONTHLY("mo"), WEEKLY("week")
}