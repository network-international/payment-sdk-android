package payment.sdk.android.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class VisaPlans(
    val matchedPlans: List<MatchedPlan>,
)

data class MatchedPlan(
    val costInfo: CostInfo,
    val fundedBy: List<String>,
    val installmentFrequency: String,
    val name: String,
    val numberOfInstallments: Int,
    val termsAndConditions: List<TermsAndCondition>,
    val type: String,
    val vPlanID: String,
    val vPlanIDRef: String
)

data class CostInfo(
    val annualPercentageRate: Double,
    val currency: String,
    val lastInstallment: LastInstallment,
    val totalFees: Double,
    val totalPlanCost: Double,
    val totalRecurringFees: Double,
    val totalUpfrontFees: Double
)

@Parcelize
data class TermsAndCondition(
    val languageCode: String,
    val text: String,
    val url: String,
    val version: Int
): Parcelable {
    fun formattedText(): String {
        return text.replace("\\n\\n", "\n\n")
            .replace("\\n", "\n")
            .replace("\\", "")
            .replace("<", "(")
            .replace(">",")")
    }
}

data class LastInstallment(
    val amount: Double,
    val installmentFee: Double,
    val totalAmount: Double,
    val upfrontFee: Double
)