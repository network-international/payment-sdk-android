package payment.sdk.android.cardpayment.visaInstalments.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.cardpayment.SDKTheme
import payment.sdk.android.sdk.R

@Composable
fun VisaHeaderView(modifier: Modifier, cardNumber: String) {
    Row(
        modifier = modifier.height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .width(100.dp)
                .height(60.dp),
            painter = painterResource(id = R.drawable.ic_logo_visa),
            contentDescription = ""
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "4111 **** **** $cardNumber", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            Text(text = stringResource(id = R.string.visa_instalment_eligible), color = Color(0xFFC6C6C6), style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp))
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