package payment.sdk.android.visaInstalments.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.model.PlanFrequency
import payment.sdk.android.payments.theme.SDKTheme
import payment.sdk.android.sdk.R

@Composable
fun InstalmentPlanView(
    modifier: Modifier,
    plan: InstallmentPlan,
    selectedPlan: InstallmentPlan?,
    onTermsAccepted: (InstallmentPlan) -> Unit,
) {
    val isSelected = selectedPlan?.id == plan.id
    val isTermsAccepted = selectedPlan?.termsAccepted ?: false
    val isTermsExpanded = selectedPlan?.termsExpanded ?: false
    Card(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF1D33C3) else Color(0xFF808080)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(if (isSelected) Color(0xFFF0F4FE) else Color.White)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    InstalmentPlanHeader(plan.frequency, plan.numberOfInstallments)

                    Spacer(modifier = Modifier.height(4.dp))

                    InstallmentPlanAmount(plan.frequency, plan.amount)

                    Spacer(modifier = Modifier.height(4.dp))

                    InstallmentFeeAndRateView(
                        plan.frequency,
                        totalUpFrontFees = plan.totalUpFrontFees,
                        monthlyRate = plan.monthlyRate
                    )
                }
            }

            selectedPlan?.terms?.let { terms ->
                VisaPlanTermsView(
                    isTermsAccepted = isTermsAccepted,
                    isSelected = isSelected,
                    termsExpanded = selectedPlan.termsExpanded,
                    frequency = plan.frequency,
                    termsAndCondition = terms,
                    onTermsAccepted = {
                        onTermsAccepted(
                            plan.copy(
                                termsAccepted = it,
                                termsExpanded = isTermsExpanded
                            )
                        )
                    },
                    onTermsExpanded = {
                        onTermsAccepted(
                            plan.copy(
                                termsExpanded = it,
                                termsAccepted = isTermsAccepted
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun InstallmentFeeAndRateView(
    frequency: PlanFrequency,
    totalUpFrontFees: String,
    monthlyRate: String
) {
    if (frequency != PlanFrequency.PayInFull) {

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    id = R.string.visa_processing_fee,
                    totalUpFrontFees
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    id = R.string.visa_monthly_rate,
                    monthlyRate
                )
            )
        }
    }
}

@Composable
fun InstallmentPlanAmount(frequency: PlanFrequency, amount: String) {
    if (frequency == PlanFrequency.PayInFull) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = amount,
            style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        )
    } else {
        val res = when (frequency) {
            PlanFrequency.MONTHLY -> R.string.visa_monthly_instalment
            PlanFrequency.WEEKLY -> R.string.visa_weekly_instalment
            PlanFrequency.BI_WEEKLY -> R.string.visa_bi_weekly_instalment
            PlanFrequency.BI_MONTHLY -> R.string.visa_bi_weekly_instalment
            else -> -1
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(
                id = res, amount
            ),
            style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        )
    }
}

@Composable
fun InstalmentPlanHeader(frequency: PlanFrequency, numberOfInstallments: Int) {
    Text(
        text = if (frequency == PlanFrequency.PayInFull) stringResource(
            id = R.string.visa_pay_in_full
        ).uppercase() else stringResource(
            id = R.string.visa_pay_in_instalment, numberOfInstallments
        ).uppercase(),
        style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
        color = Color(0xFF00235D)
    )
}

@Preview(name = "Plan Pay In full", device = Devices.PIXEL_4_XL)
@Composable
fun InstalmentPlanPayInFull_Preview() {
    SDKTheme {
        InstalmentPlanView(
            modifier = Modifier,
            InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.PayInFull),
            null
        ) {}
    }
}

@Preview(name = "Pay In Installments", device = Devices.PIXEL_4_XL)
@Composable
fun InstalmentPlanView_Preview() {
    SDKTheme {
        InstalmentPlanView(
            modifier = Modifier,
            InstallmentPlan.dummyInstallmentPlan,
            null
        ) {}
    }
}

@Preview(name = "Plan Terms Expanded", device = Devices.PIXEL_4_XL)
@Composable
fun InstalmentPlanViewTerms_Preview() {
    SDKTheme {
        InstalmentPlanView(
            modifier = Modifier,
            InstallmentPlan.dummyInstallmentPlan,
            InstallmentPlan.dummyInstallmentPlan.copy(termsExpanded = true)
        ) {}
    }
}