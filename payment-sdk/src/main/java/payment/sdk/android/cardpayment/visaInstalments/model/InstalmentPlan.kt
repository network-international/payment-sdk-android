package payment.sdk.android.cardpayment.visaInstalments.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.TermsAndCondition
import payment.sdk.android.core.VisaPlans
import java.util.Locale

@Parcelize
data class InstalmentPlan(
    val id: String,
    val currency: String,
    val amount: String,
    val totalUpFrontFees: String,
    val monthlyRate: String,
    val numberOfInstallments: Int,
    val frequency: PlanFrequency,
    val termsAccepted: Boolean = false,
    val terms: TermsAndCondition?
) : Parcelable {
    companion object {
        fun fromVisaPlans(visaPlans: VisaPlans): List<InstalmentPlan> {
            return visaPlans.matchedPlans.map { matchedPlans ->
                val terms = matchedPlans.termsAndConditions
                    .firstOrNull { it.languageCode == Locale.getDefault().isO3Language }
                InstalmentPlan(
                    currency = matchedPlans.costInfo.currency,
                    terms = terms,
                    amount = matchedPlans.costInfo.lastInstallment.totalAmount.toString(),
                    totalUpFrontFees = matchedPlans.costInfo.totalUpfrontFees.toString(),
                    monthlyRate = matchedPlans.costInfo.annualPercentageRate.toString(),
                    id = matchedPlans.vPlanID,
                    numberOfInstallments = matchedPlans.numberOfInstallments,
                    frequency = getPlanFrequency(matchedPlans.installmentFrequency)
                )
            }
        }

        private fun getPlanFrequency(frequency: String): PlanFrequency {
            return when (frequency) {
                "MONTHLY" -> PlanFrequency.MONTHLY
                "WEEKLY" -> PlanFrequency.WEEKLY
                else -> PlanFrequency.PayInFull
            }
        }

        fun payInFull(): InstalmentPlan {
            return InstalmentPlan(
                id = "",
                currency = "",
                amount = "",
                totalUpFrontFees = "",
                monthlyRate = "",
                numberOfInstallments = 0,
                frequency = PlanFrequency.PayInFull,
                termsAccepted = false,
                terms = null,
            )
        }
    }


}

enum class PlanFrequency(val value: String) {
    MONTHLY("mo"), WEEKLY("week"), PayInFull("Full")
}