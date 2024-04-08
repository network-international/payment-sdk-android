package payment.sdk.android.cardpayment.visaInstalments.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import payment.sdk.android.cardpayment.SDKTheme
import payment.sdk.android.cardpayment.visaInstalments.model.InstalmentPlan
import payment.sdk.android.cardpayment.visaInstalments.model.PlanFrequency
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstalmentsVMState
import payment.sdk.android.core.TermsAndCondition
import payment.sdk.android.sdk.R

@Composable
fun VisaInstalmentsView(
    state: VisaInstalmentsVMState.PlanSelection,
    onNavigationUp: () -> Unit,
    onSelectPlan: (plan: InstalmentPlan) -> Unit,
    onPayClicked: (plan: InstalmentPlan) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.make_payment),
                        color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
                    )
                },
                backgroundColor = colorResource(id = R.color.payment_sdk_toolbar_color),
                navigationIcon = {
                    IconButton(onClick = { onNavigationUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            tint = colorResource(id = R.color.payment_sdk_toolbar_icon_color),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            VisaInstalmentBottomBar(state.isValid) {
                state.selectedPlan?.let(onPayClicked)
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            VisaHeaderView(modifier = Modifier.padding(8.dp), cardNumber = "1111")
            LazyColumn() {

                items(count = state.installmentPlans.size) { index ->
                    InstalmentPlanView(
                        modifier = Modifier.clickable {
                            onSelectPlan(state.installmentPlans[index])
                        },
                        state.installmentPlans[index],
                        state.selectedPlan
                    ) {
                        onSelectPlan(it)
                    }
                }
            }
        }
    }
}

@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun VisaInstalmentsView_Preview() {
    SDKTheme {
        VisaInstalmentsView(
            VisaInstalmentsVMState.PlanSelection(
                installmentPlans = listOf(
                    InstalmentPlan(
                        id = "10",
                        currency = "AED",
                        amount = "64",
                        monthlyRate = "718",
                        totalUpFrontFees = "15",
                        numberOfInstallments = 0,
                        frequency = PlanFrequency.PayInFull,
                        terms = null
                    ),
                    InstalmentPlan(
                        id = "11",
                        currency = "AED",
                        amount = "64",
                        monthlyRate = "718",
                        totalUpFrontFees = "15",
                        numberOfInstallments = 3,
                        frequency = PlanFrequency.MONTHLY,
                        terms = TermsAndCondition(
                            languageCode = "en",
                            text = "These terms of use constitute an agreement between you and X Pay Pvt Ltd ABN 123456 trading as X Pay(we, our, or us) (and any person who acquires your Payment Plan from us).\\nOur Buy Now Pay Later option allows you to purchase goods or services over a period of time by repaying us in equal instalments (Payment Plan).\\nBy entering into a Payment Plan, you agree to be bound by these Terms of Use.\\nYou should also read our Privacy Policy which forms a part of this agreement.\\n",
                            url = "xyz",
                            version = 1
                        )
                    ),
                    InstalmentPlan(
                        id = "13",
                        currency = "AED",
                        amount = "64",
                        monthlyRate = "718",
                        totalUpFrontFees = "15",
                        numberOfInstallments = 12,
                        frequency = PlanFrequency.MONTHLY,
                        terms = TermsAndCondition(
                            languageCode = "en",
                            text = "These terms of use constitute an agreement between you and X Pay Pvt Ltd ABN 123456 trading as X Pay(we, our, or us) (and any person who acquires your Payment Plan from us).\\nOur Buy Now Pay Later option allows you to purchase goods or services over a period of time by repaying us in equal instalments (Payment Plan).\\nBy entering into a Payment Plan, you agree to be bound by these Terms of Use.\\nYou should also read our Privacy Policy which forms a part of this agreement.\\n",
                            url = "xyz",
                            version = 1
                        )
                    )
                ),
                newCardDto = null,
                orderUrl = "",
                savedCardUrl = "",
                paymentCookie = "",
                paymentUrl = "",
                selectedPlan = InstalmentPlan(
                    id = "11",
                    currency = "AED",
                    amount = "64",
                    monthlyRate = "718",
                    totalUpFrontFees = "15",
                    numberOfInstallments = 12,
                    frequency = PlanFrequency.MONTHLY,
                    termsAccepted = true,
                    terms = TermsAndCondition(
                        languageCode = "en",
                        text = "These terms of use constitute an agreement between you and X Pay Pvt Ltd ABN 123456 trading as X Pay(we, our, or us) (and any person who acquires your Payment Plan from us).\\nOur Buy Now Pay Later option allows you to purchase goods or services over a period of time by repaying us in equal instalments (Payment Plan).\\nBy entering into a Payment Plan, you agree to be bound by these Terms of Use.\\nYou should also read our Privacy Policy which forms a part of this agreement.\\n",
                        url = "xyz",
                        version = 1
                    )
                ),
                isValid = true,
                savedCardDto = null
            ), onNavigationUp = {}, onSelectPlan = {}, onPayClicked = {}
        )
    }
}