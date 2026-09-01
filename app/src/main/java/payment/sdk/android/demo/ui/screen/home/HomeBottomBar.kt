package payment.sdk.android.demo.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale
import payment.sdk.android.core.testId

@Composable
fun HomeBottomBar(
    modifier: Modifier,
    total: Double,
    currency: String,
    onClickPayByCard: () -> Unit,
    onClickGooglePay: () -> Unit = {},
    onClickSamsungPay: () -> Unit = {},
    showSamsungPay: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testId("storefront_button_pay"),
                onClick = onClickPayByCard
            ) {
                Text(text = "Pay $currency ${"%.2f".format(Locale.ENGLISH, total)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testId("storefront_button_google_pay"),
                onClick = onClickGooglePay
            ) {
                Text(text = "Google Pay")
            }
            if (showSamsungPay) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testId("storefront_button_samsung_pay"),
                    onClick = onClickSamsungPay
                ) {
                    Text(text = "Samsung Pay")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}