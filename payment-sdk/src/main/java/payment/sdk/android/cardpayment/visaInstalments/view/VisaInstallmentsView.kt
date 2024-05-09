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
import payment.sdk.android.cardpayment.visaInstalments.model.InstallmentPlan
import payment.sdk.android.cardpayment.visaInstalments.model.NewCardDto
import payment.sdk.android.cardpayment.visaInstalments.model.PlanFrequency
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstallmentsVMState
import payment.sdk.android.sdk.R

@Composable
fun VisaInstalmentsView(
    state: VisaInstallmentsVMState.PlanSelection,
    onNavigationUp: () -> Unit,
    onSelectPlan: (InstallmentPlan) -> Unit,
    onPayClicked: (InstallmentPlan) -> Unit,
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
            val cardNumber: String = if (state.savedCardDto != null) {
                state.savedCardDto.maskedPan
            } else if (state.newCardDto != null) {
                state.newCardDto.cardNumber
            } else {
                ""
            }
            VisaHeaderView(modifier = Modifier.padding(8.dp), cardNumber = cardNumber)
            LazyColumn {

                items(count = state.installmentPlans.size) { index ->
                    InstalmentPlanView(
                        modifier = Modifier.clickable {
                            onSelectPlan(state.installmentPlans[index])
                        },
                        state.installmentPlans[index],
                        state.selectedPlan,
                        onTermsAccepted = {
                            onSelectPlan(it)
                        }
                    )
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
            VisaInstallmentsVMState.PlanSelection(
                installmentPlans = listOf(
                    InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.PayInFull),
                    InstallmentPlan.dummyInstallmentPlan.copy(
                        id = "13",
                        frequency = PlanFrequency.MONTHLY
                    ),
                    InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.BI_MONTHLY),
                    InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.BI_WEEKLY),
                    InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.WEEKLY)
                ),
                newCardDto = NewCardDto(
                    cardNumber = "4761080127842022",
                    cvv = "",
                    expiry = "",
                    customerName = ""
                ),
                orderUrl = "",
                savedCardUrl = "",
                paymentCookie = "",
                paymentUrl = "",
                selectedPlan = InstallmentPlan.dummyInstallmentPlan.copy(id = "13"),
                isValid = true,
                savedCardDto = null,
                accessToken = ""
            ), onNavigationUp = {}, onSelectPlan = {}, onPayClicked = {}
        )
    }
}