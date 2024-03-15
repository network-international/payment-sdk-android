package payment.sdk.android.cardpayment.visaInstalments.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import payment.sdk.android.cardpayment.SDKTheme
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VisaHeaderView(modifier: Modifier, cardNumber: String) {
    Row(
        modifier = modifier.height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .width(100.dp)
                .height(60.dp),
            backgroundColor = Color.Blue,
            shape = RoundedCornerShape(8.dp),
            elevation = 16.dp
        ) {

        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "VISA****$cardNumber")
            Text(text = "Eligible for instalments")
        }
    }
}


@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun VisaHeaderView_Preview() {
    SDKTheme {
        VisaHeaderView(modifier = Modifier, cardNumber = "1234")
    }
}