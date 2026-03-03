package payment.sdk.android.aaniPay.views

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.cardpayment.theme.sdkColor
import payment.sdk.android.sdk.R

@Composable
internal fun AaniQrLoadingScreen() {
    val goldColor = sdkColor(R.color.payment_sdk_pay_button_gold)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Golden bordered loading container
        Box(
            modifier = Modifier
                .size(250.dp)
                .border(
                    width = 3.dp,
                    color = goldColor,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = goldColor,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.aani_generating_qr),
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.aani_keep_modal_open),
            style = TextStyle(
                fontWeight = FontWeight.W400,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}
