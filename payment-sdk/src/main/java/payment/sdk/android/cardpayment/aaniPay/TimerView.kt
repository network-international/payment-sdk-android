package payment.sdk.android.cardpayment.aaniPay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.sdk.R
import java.util.Locale

@Composable
fun TimerView(modifier: Modifier, minutes: Int, seconds: Int, amount: String) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.aani_paying_amount, amount),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colorResource(id = R.color.payment_sdk_pay_button_background_color)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.aani_tap_notification),
            style = TextStyle(
                fontWeight = FontWeight.W500,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.aani_note_do_not_close),
            style = TextStyle(
                fontWeight = FontWeight.W400,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Preview
@Composable
fun TimerViewPreview() {
    Box(modifier = Modifier.background(Color.White)) {
        TimerView(Modifier, 29, 42, "AED 100")
    }
}