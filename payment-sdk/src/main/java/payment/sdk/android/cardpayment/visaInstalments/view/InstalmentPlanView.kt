package payment.sdk.android.cardpayment.visaInstalments.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.cardpayment.SDKTheme
import payment.sdk.android.cardpayment.visaInstalments.model.InstalmentPlan
import payment.sdk.android.cardpayment.visaInstalments.model.PlanFrequency
import payment.sdk.android.core.TermsAndCondition
import payment.sdk.android.sdk.R

@Composable
fun InstalmentPlanView(
    modifier: Modifier,
    plan: InstalmentPlan,
    selectedPlan: InstalmentPlan?,
    onTermsAccepted: (plan: InstalmentPlan) -> Unit
) {
    val isSelected = selectedPlan?.id == plan.id
    val isTermsAccepted = selectedPlan?.termsAccepted ?: false
    Card(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        border = BorderStroke(3.dp, if (isSelected) Color(0xFF1D33C3) else Color(0xFF808080)),
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
                    Text(
                        text = if (plan.frequency == PlanFrequency.PayInFull) stringResource(
                            id = R.string.visa_pay_in_full
                        ) else stringResource(
                            id = R.string.visa_pay_in_instalment, plan.numberOfInstallments
                        ),
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = Color(red = 0, green = 35, blue = 93)
                    )
                    if (plan.frequency != PlanFrequency.PayInFull) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
                    if (plan.frequency == PlanFrequency.PayInFull) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "${plan.currency} ${plan.amount}",
                            style = MaterialTheme.typography.h6,
                        )
                    } else {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(
                                id = R.string.visa_monthly_instalment,
                                plan.amount,
                                plan.currency
                            ),
                            style = MaterialTheme.typography.h6,
                        )
                    }
                    if (plan.frequency != PlanFrequency.PayInFull) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (plan.frequency != PlanFrequency.PayInFull) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.visa_processing_fee,
                                    plan.totalUpFrontFees,
                                    plan.currency
                                )
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.visa_monthly_rate,
                                    plan.monthlyRate
                                )
                            )
                        }
                    }
                }
                if (plan.frequency != PlanFrequency.PayInFull) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            AnimatedVisibility(
                visible = isSelected && plan.frequency != PlanFrequency.PayInFull,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Checkbox(
                        checked = isTermsAccepted,
                        onCheckedChange = {
                            onTermsAccepted(plan.copy(termsAccepted = true))
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF1D33C3),
                            uncheckedColor = Color(0xFF808080)
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = plan.terms?.text ?: ""
                    )
                }
            }
        }
    }
}

@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun InstalmentPlanView_Preview() {
    SDKTheme {
        InstalmentPlanView(
            modifier = Modifier,
            InstalmentPlan(
                id = "12",
                amount = "108",
                currency = "AED",
                totalUpFrontFees = "20",
                monthlyRate = "648",
                numberOfInstallments = 6,
                frequency = PlanFrequency.MONTHLY,
                terms = TermsAndCondition(
                    languageCode = "en",
                    text = "These terms of use constitute an agreement between you and X Pay Pvt Ltd ABN 123456 trading as X Pay(we, our, or us) (and any person who acquires your Payment Plan from us).\\nOur Buy Now Pay Later option allows you to purchase goods or services over a period of time by repaying us in equal instalments (Payment Plan).\\nBy entering into a Payment Plan, you agree to be bound by these Terms of Use.\\nYou should also read our Privacy Policy which forms a part of this agreement.\\n",
                    url = "xyz",
                    version = 1
                )
            ),
            null
        ) {

        }
    }
}