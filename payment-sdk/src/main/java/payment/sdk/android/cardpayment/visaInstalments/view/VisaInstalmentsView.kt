package payment.sdk.android.cardpayment.visaInstalments.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import payment.sdk.android.cardpayment.SDKTheme
import payment.sdk.android.cardpayment.savedCard.view.SavedCardViewBottomBar
import payment.sdk.android.cardpayment.visaInstalments.InstalmentPlan
import payment.sdk.android.cardpayment.visaInstalments.PlanFrequency
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.sdk.R
import java.util.Locale

@Composable
fun VisaInstalmentsView(
    onNavigationUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_activity_visa_instalments),
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
    ) { contentPadding ->


        val plans = listOf(
            InstalmentPlan(
                price = "108",
                currency = "AED",
                fee = "20",
                total = "648",
                numberOfInstallments = 3,
                frequency = PlanFrequency.MONTHLY
            ),
            InstalmentPlan(
                price = "64",
                currency = "AED",
                fee = "15",
                total = "718",
                numberOfInstallments = 6,
                frequency = PlanFrequency.MONTHLY
            )
        )

        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            VisaHeaderView(modifier = Modifier.padding(8.dp), cardNumber = "1234")

            PlanPayInFull(isSelected = false)

            LazyColumn(

            ) {
                items(count = plans.size) { index ->
                    InstalmentPlanView(plans[index], index == 0)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            VisaInstalmentBottomBar(123, "AED") {
            }
        }
    }
}

@Composable
fun VisaInstalmentBottomBar(
    amount: Int,
    currency: String,
    onPayClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        backgroundColor = Color.White,
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        elevation = 16.dp
    ) {
        val isLTR =
            TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_LTR
        val orderAmount = OrderAmount(amount.toDouble(), currency)
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = true, onCheckedChange = {})
                Text(text = "Terms and conditions")
            }

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(8.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = colorResource(id = R.color.payment_sdk_pay_button_background_color)
                ),
                onClick = {
                    onPayClicked()
                },
                shape = RoundedCornerShape(percent = 15),
            ) {
                Text(
                    text = stringResource(
                        id = R.string.pay_button_title,
                        orderAmount.formattedCurrencyString(isLTR)
                    ),
                    color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
                )
            }
        }
    }
}


@Composable
fun PlanPayInFull(isSelected: Boolean) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        border = BorderStroke(2.dp, if (isSelected) Color.Blue else Color.Gray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pay in full",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = Color(red = 0, green = 35, blue = 93)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "AED 5634.")
        }
    }
}


@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun VisaInstalmentsView_Preview() {
    SDKTheme {
        VisaInstalmentsView() {

        }
    }
}