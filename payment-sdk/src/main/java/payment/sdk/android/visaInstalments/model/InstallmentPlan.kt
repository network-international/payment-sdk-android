package payment.sdk.android.visaInstalments.model

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
                    ).formattedCurrencyString2Decimal(isLTR),
                    totalUpFrontFees = OrderAmount(
                        matchedPlans.costInfo.totalUpfrontFees,
                        currency
                    ).formattedCurrencyString2Decimal(isLTR),
                    monthlyRate =  String.format(Locale.ENGLISH, "%.2f", matchedPlans.costInfo.annualPercentageRate / 100.00),
                    id = matchedPlans.vPlanID,
                    numberOfInstallments = matchedPlans.numberOfInstallments,
                    frequency = getPlanFrequency(matchedPlans.installmentFrequency)
                )
            }
            return listOf(payInFull(amount = orderAmount.formattedCurrencyString2Decimal(isLTR))) + remotePlans
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
                text = "[Test Bank] This is a sample T&amp;C text containing Special Ch@racters that govern the Visa Installment services. Refer to https://www.visa.com for more information.\\n\\n1) Eligibility: To be eligible for \\\"Visa installment\\\" payments, you must hold a valid and active Visa credit card issued by an authorized bank. Your credit limit and repayment capacity will determine your eligibility for installment plans.\\n\\n2) Installment Plan: Installments can be availed for purchases at participating merchants, subject to a minimum transaction amount. The specific installment plan (number of months, interest rate, etc.) will be determined at the time of purchase.\\n\\n3) Interest &amp; Fees: 20% Interest will be charged on the unpaid principal amount. If you fail to pay any installment by the due date, a late payment fee will be charged. All fees/charges are non-refundable and subject to change.\\n\\n4) Prepayment &amp; Cancellation: You may choose to prepay the entire outstanding amount at any time. However, prepayment may attract a fee. The installment plan can be cancelled if your Visa card is cancelled &lt;or if you default on payment&gt;.\\n\\n5) Changes to Terms: Visa reserves the right to change these terms and conditions at any time. Any changes will be communicated to you via email or through our website. It is your responsibility to keep yourself updated with the latest terms and conditions.\\n\\nPlease note that these terms are subject to change and it's always recommended to read the actual terms and conditions provided by the card issuer.",
                url = "xyz",
                version = 1
            )
        )
    }
}

enum class PlanFrequency {
    MONTHLY, WEEKLY, PayInFull, BI_WEEKLY, BI_MONTHLY,
}