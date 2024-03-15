package payment.sdk.android.cardpayment.visaInstalments.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.cardpayment.SDKTheme
import payment.sdk.android.cardpayment.visaInstalments.InstalmentPlan
import payment.sdk.android.cardpayment.visaInstalments.PlanFrequency

@Composable
fun InstalmentPlanView(plan: InstalmentPlan, isSelected: Boolean = false) {
    Card(
        modifier = Modifier.padding(8.dp),
        border = BorderStroke(2.dp, if (isSelected) Color(0xFF00C7B1) else Color.Gray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.White)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pay in ${plan.numberOfInstallments}",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = Color(red = 0, green = 35, blue = 93)
                )

                Icon(Icons.Filled.Face, "menu")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${plan.currency} ${plan.price}/${plan.frequency.value}",
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Total fees: ${plan.currency} ${plan.fee}")
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Total ${plan.currency} ${plan.total}")
            }
        }
    }
}

@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun InstalmentPlanView_Preview() {
    SDKTheme {
        InstalmentPlanView(
            InstalmentPlan(
                price = "108",
                currency = "AED",
                fee = "20",
                total = "648",
                numberOfInstallments = 6,
                frequency = PlanFrequency.MONTHLY
            ),
            true
        )
    }
}