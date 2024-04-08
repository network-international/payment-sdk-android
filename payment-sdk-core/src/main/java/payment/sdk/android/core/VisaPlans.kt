package payment.sdk.android.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class VisaPlans(
    val matchedPlans: List<MatchedPlan>,
    val merchantInfo: MerchantInfo,
    val paymentAccountReference: String,
    val transactionAmount: Int,
    val transactionCurrency: String
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

data class MerchantInfo(
    val category: String,
    val partnerMerchantReferenceID: String
)

data class CostInfo(
    val annualPercentageRate: Int,
    val currency: String,
    val feeInfo: List<FeeInfo>,
    val firstInstallment: FirstInstallment,
    val lastInstallment: LastInstallment,
    val totalFees: Int,
    val totalPlanCost: Int,
    val totalRecurringFees: Int,
    val totalUpfrontFees: Int
)

@Parcelize
data class TermsAndCondition(
    val languageCode: String,
    val text: String,
    val url: String,
    val version: Int
): Parcelable

data class FeeInfo(
    val flatFee: Int,
    val ratePercentage: Int,
    val type: String
)

data class FirstInstallment(
    val amount: Int,
    val installmentFee: Int,
    val totalAmount: Int,
    val upfrontFee: Int
)

data class LastInstallment(
    val amount: Int,
    val installmentFee: Int,
    val totalAmount: Int,
    val upfrontFee: Int
)