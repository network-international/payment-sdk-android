package payment.sdk.android.visaInstalments.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import payment.sdk.android.payments.theme.SDKTheme
import payment.sdk.android.sdk.R

@Composable
fun VisaInstalmentBottomBar(
    modifier: Modifier = Modifier,
    isValid: Boolean,
    onPayClicked: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(bottom = 8.dp),
        backgroundColor = Color.White,
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        elevation = 16.dp
    ) {

        Column {
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(8.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = if (isValid) colorResource(id = R.color.payment_sdk_pay_button_background_color) else Color.Gray,
                ),
                onClick = {
                    if (isValid) {
                        onPayClicked()
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(percent = 15),
            ) {
                Text(
                    text = stringResource(id = R.string.make_payment),
                    color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
                )
            }

            Image(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .height(12.dp),
                painter = painterResource(id = R.drawable.visa_branding),
                contentDescription = "Localized description"
            )
        }
    }
}

@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun VisaInstalmentBottomBar_Preview() {
    SDKTheme {
        VisaInstalmentBottomBar(isValid = true) {}
    }
}