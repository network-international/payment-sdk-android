package payment.sdk.android.visaInstalments.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.visaInstalments.model.PlanFrequency
import payment.sdk.android.payments.theme.SDKTheme

@Composable
fun VisaInstalmentsView(
    modifier: Modifier = Modifier,
    instalmentPlans: List<InstallmentPlan>,
    cardNumber: String,
    onPayClicked: (InstallmentPlan) -> Unit,
) {
    var selectedPlan by remember { mutableStateOf<InstallmentPlan?>(null) }
    var isValid by remember { mutableStateOf(false) }

    Column(modifier = modifier.background(Color.White)) {
        VisaHeaderView(modifier = Modifier.padding(8.dp), cardNumber = cardNumber)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(instalmentPlans) { plan ->
                InstalmentPlanView(
                    modifier = Modifier.clickable {
                        selectedPlan = plan
                        isValid =
                            selectedPlan?.frequency == PlanFrequency.PayInFull || selectedPlan?.termsAccepted ?: false
                    },
                    plan = plan,
                    selectedPlan = selectedPlan,
                    onTermsAccepted = {
                        selectedPlan = it
                        isValid =
                            selectedPlan?.frequency == PlanFrequency.PayInFull || selectedPlan?.termsAccepted ?: false
                    }
                )
            }
        }
        VisaInstalmentBottomBar(isValid = isValid) {
            selectedPlan?.let {
                onPayClicked(it)
            }
        }
    }
}

@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun VisaInstalmentsView_Preview() {
    SDKTheme {
        VisaInstalmentsView(
            instalmentPlans = listOf(
                InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.PayInFull),
                InstallmentPlan.dummyInstallmentPlan.copy(
                    id = "13",
                    frequency = PlanFrequency.MONTHLY
                ),
                InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.BI_MONTHLY),
                InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.BI_WEEKLY),
                InstallmentPlan.dummyInstallmentPlan.copy(frequency = PlanFrequency.WEEKLY)
            ),
            cardNumber = "476108******2022"
        ) {}
    }
}