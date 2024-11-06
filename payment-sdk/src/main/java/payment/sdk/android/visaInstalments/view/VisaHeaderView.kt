package payment.sdk.android.visaInstalments.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.payments.theme.SDKTheme
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
            val maskedCardNumber = if (cardNumber.isNotEmpty()) {
                cardNumber
                    .replaceRange(6, 12, "*".repeat(6))
                    .replace("....".toRegex(), "$0 ")
            } else ""
            Text(
                text = maskedCardNumber,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textDirection = TextDirection.Ltr
                )
            )
            Text(
                text = stringResource(id = R.string.visa_instalment_eligible),
                color = Color(0xFF33845C),
                style = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp)
            )
        }
    }
}


@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun VisaHeaderView_Preview() {
    SDKTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            VisaHeaderView(modifier = Modifier, cardNumber = "4761080127842022")
        }
    }
}