package payment.sdk.android.payments.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.sdk.R
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.payments.theme.PgType
import payment.sdk.android.payments.theme.Radius
import payment.sdk.android.payments.theme.Spacing
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.model.PlanFrequency

/**
 * Inline Visa Installments selector — rendered inside the unified payment page card section
 * after eligibility returns matched plans. Mirrors the design: header with megaphone icon,
 * horizontal pill row (Pay in full / N months / …), monthly rate / total / processing fees,
 * T&C checkbox, "Instalment enabled by VISA" footer.
 */
@Composable
internal fun VisaInstallmentSection(
    plans: VisaPlans,
    orderAmount: OrderAmount,
    selectedPlan: InstallmentPlan?,
    termsAccepted: Boolean,
    onPlanSelected: (InstallmentPlan?) -> Unit,
    onTermsToggled: (Boolean) -> Unit,
    showSelectionError: Boolean = false,
) {
    val installmentPlans = remember(plans, orderAmount) {
        InstallmentPlan.fromVisaPlans(plans, orderAmount)
    }

    val brandBlue = Color(0xFF2E6FF2)
    val mutedGrey = Color(0xFF8F8F8F)
    val borderGrey = Color(0xFFE0E0E0)

    Spacer(Modifier.height(Spacing.rowGap))

    // Header — megaphone icon + "Eligible for instalments". Icon shape mirrors the iOS
    // SF Symbol `megaphone.fill` used in VisaInstallmentInlineView.
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_megaphone_fill),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(brandBlue),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Eligible for instalments",
            color = brandBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(Modifier.height(8.dp))

    // Pill row — horizontally scrollable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        installmentPlans.forEach { plan ->
            val selected = plan.id == selectedPlan?.id
            val title = if (plan.frequency == PlanFrequency.PayInFull) "Pay in full" else "${plan.numberOfInstallments} months"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.row))
                    .background(Color.White, RoundedCornerShape(Radius.row))
                    .border(
                        width = 1.dp,
                        color = if (selected) brandBlue else borderGrey,
                        shape = RoundedCornerShape(Radius.row)
                    )
                    .clickable { onPlanSelected(plan) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) brandBlue else Color(0xFF1A1A1A)
                )
            }
        }
    }

    if (showSelectionError) {
        Spacer(Modifier.height(6.dp))
        androidx.compose.material.Text(
            text = androidx.compose.ui.res.stringResource(R.string.select_payment_option_validation),
            color = Color(0xFFD32F2F),
            fontSize = 13.sp
        )
    }

    val activePlan = selectedPlan
    if (activePlan != null && activePlan.frequency != PlanFrequency.PayInFull) {
        Spacer(Modifier.height(12.dp))

        // Monthly amount + label "for N months"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${activePlan.amount} / Month",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = "for ${activePlan.numberOfInstallments} months",
                fontSize = 14.sp,
                color = mutedGrey
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Monthly rate: ${activePlan.monthlyRate}%",
                    fontSize = 13.sp,
                    color = mutedGrey
                )
                Text(
                    text = "+ Processing fees: ${activePlan.totalUpFrontFees}",
                    fontSize = 13.sp,
                    color = mutedGrey
                )
            }
            Text(
                text = "Total: ${activePlan.amount}",
                fontSize = 13.sp,
                color = mutedGrey
            )
        }

        Spacer(Modifier.height(12.dp))

        // T&C checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = onTermsToggled,
                colors = CheckboxDefaults.colors(checkedColor = brandBlue)
            )
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    text = "Terms and condition's",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "By continuing you agree to the terms and conditions",
                    fontSize = 12.sp,
                    color = mutedGrey
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Footer — "Instalment enabled by [Visa logo]". Logo uses the official brand asset
    // (ic_logo_visa) that ships with the rest of the SDK for visual consistency.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Instalment enabled by ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = brandBlue
        )
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_logo_visa),
            contentDescription = "Visa",
            modifier = Modifier.height(14.dp)
        )
    }
}
