package payment.sdk.android.cardpayment.visaInstalments.model

import android.os.Parcelable
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import kotlinx.parcelize.Parcelize
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.TermsAndCondition
import payment.sdk.android.core.VisaPlans
import java.util.Locale
import java.util.UUID

@Parcelize
data class InstallmentPlan(
    val id: String,
    val currency: String,
    val amount: String,
    val totalUpFrontFees: String,
    val monthlyRate: String,
    val numberOfInstallments: Int,
    val frequency: PlanFrequency,
    val termsAccepted: Boolean = false,
    val terms: TermsAndCondition?,
    val termsExpanded: Boolean = false
) : Parcelable {
    companion object {
        fun fromVisaPlans(visaPlans: VisaPlans, orderAmount: OrderAmount): List<InstallmentPlan> {
            val isLTR =
                TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR
            val remotePlans = visaPlans.matchedPlans.map { matchedPlans ->
                val terms = matchedPlans.termsAndConditions
                    .firstOrNull { it.languageCode == Locale.getDefault().isO3Language }
                val currency = matchedPlans.costInfo.currency
                InstallmentPlan(
                    terms = terms,
                    currency = currency,
                    amount = OrderAmount(
                        matchedPlans.costInfo.lastInstallment.totalAmount,
                        currency
                    ).formattedCurrencyString(isLTR),
                    totalUpFrontFees = OrderAmount(
                        matchedPlans.costInfo.totalUpfrontFees,
                        currency
                    ).formattedCurrencyString(isLTR),
                    monthlyRate = (matchedPlans.costInfo.annualPercentageRate / 100.00).toString(),
                    id = matchedPlans.vPlanID,
                    numberOfInstallments = matchedPlans.numberOfInstallments,
                    frequency = getPlanFrequency(matchedPlans.installmentFrequency)
                )
            }
            return listOf(payInFull(amount = orderAmount.formattedCurrencyString(isLTR))) + remotePlans
        }

        private fun getPlanFrequency(frequency: String): PlanFrequency {
            return when (frequency) {
                "MONTHLY" -> PlanFrequency.MONTHLY
                "WEEKLY" -> PlanFrequency.WEEKLY
                "BIMONTHLY" -> PlanFrequency.BI_MONTHLY
                "BIWEEKLY" -> PlanFrequency.BI_WEEKLY
                else -> PlanFrequency.PayInFull
            }
        }

        private fun payInFull(amount: String): InstallmentPlan {
            return InstallmentPlan(
                id = UUID.randomUUID().toString(),
                amount = amount,
                currency = "",
                totalUpFrontFees = "",
                monthlyRate = "",
                numberOfInstallments = 0,
                frequency = PlanFrequency.PayInFull,
                termsAccepted = false,
                terms = null,
            )
        }

        val dummyInstallmentPlan = InstallmentPlan(
            id = "12",
            amount = "108",
            currency = "AED",
            totalUpFrontFees = "20",
            monthlyRate = "2.8",
            numberOfInstallments = 6,
            frequency = PlanFrequency.MONTHLY,
            terms = TermsAndCondition(
                languageCode = "en",
                text = "These terms of use constitute an agreement between you and X Pay Pvt Ltd ABN 123456 trading as X Pay(we, our, or us) (and any person who acquires your Payment Plan from us).\\nOur Buy Now Pay Later option allows you to purchase goods or services over a period of time by repaying us in equal instalments (Payment Plan).\\nBy entering into a Payment Plan, you agree to be bound by these Terms of Use.\\nYou should also read our Privacy Policy which forms a part of this agreement.\\n",
                url = "xyz",
                version = 1
            )
        )
    }
}

enum class PlanFrequency {
    MONTHLY, WEEKLY, PayInFull, BI_WEEKLY, BI_MONTHLY,
}