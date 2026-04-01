package payment.sdk.android.payments.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.googlepay.GooglePayButton
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.sdk.R

@Composable
fun WalletButtonsSection(
    modifier: Modifier = Modifier,
    googlePayUiConfig: GooglePayUiConfig?,
    isSamsungPayAvailable: Boolean,
    aaniConfig: AaniPayLauncher.Config?,
    isProcessing: Boolean,
    onGooglePay: () -> Unit,
    onSamsungPay: () -> Unit,
    onClickAaniPay: (AaniPayLauncher.Config) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        googlePayUiConfig?.let {
            GooglePayButton(
                enabled = !isProcessing,
                onClick = {
                    if (!isProcessing) {
                        onGooglePay()
                    }
                },
                radius = 8.dp,
                allowedPaymentMethods = it.allowedPaymentMethods,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sdk_paymentpage_button_googlePay")
            )
            Spacer(Modifier.height(8.dp))
        }

        if (isSamsungPayAvailable) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sdk_paymentpage_button_samsungPay"),
                onClick = onSamsungPay,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Black,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.samsung_pay_logo),
                    contentDescription = stringResource(R.string.samsung_pay_button),
                    modifier = Modifier.height(24.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        aaniConfig?.let { config ->
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sdk_paymentpage_button_aaniPay"),
                onClick = { onClickAaniPay(config) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                ),
                border = BorderStroke(width = 1.dp, Color.Gray),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.aani_logo),
                    contentDescription = stringResource(R.string.aani),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
