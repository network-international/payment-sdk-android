package payment.sdk.android.cardpayment.partialAuth.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import payment.sdk.android.cardpayment.partialAuth.PartialAuthState
import payment.sdk.android.cardpayment.partialAuth.model.PartialAuthProperties
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.sdk.R

@Composable
fun PartialAuthView(
    state: PartialAuthState,
    properties: PartialAuthProperties,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Scaffold(
        backgroundColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.paypage_title_awaiting_partial_auth_approval),
                        color = colorResource(id = R.color.payment_sdk_pay_button_text_color),
                        textAlign = TextAlign.Center
                    )
                },
                backgroundColor = colorResource(id = R.color.payment_sdk_toolbar_color),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(contentPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state == PartialAuthState.LOADING) {
                CircularProgressDialog(message = LoadingMessage.PAYMENT)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD3d3d3))
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(vertical = 36.dp)
                        .align(Alignment.Center),
                    painter = painterResource(R.drawable.network_international_logo),
                    contentDescription = ""
                )
            }
            val titleText = if (properties.bankName == null) {
                stringResource(
                    R.string.partial_auth_message,
                    properties.approvedAmount,
                    properties.fullAmount
                )
            } else {
                stringResource(
                    R.string.partial_auth_message_with_org,
                    properties.bankName,
                    properties.approvedAmount,
                    properties.fullAmount
                )
            }
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                text = titleText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6
            )

            Divider()

            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                text = stringResource(R.string.partial_auth_message_question),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.weight(1f), onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(id = R.color.payment_sdk_pay_button_background_color),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.button_yes))
                }

                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDecline,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(stringResource(R.string.cancel_alert))
                }
            }
        }
    }
}

@Preview
@Composable
fun PartialAuthViewPreview() {
    Box {
        PartialAuthView(
            PartialAuthState.LOADING,
            PartialAuthProperties("HDFC", "500.0", "100.0"),
            {}) { }
    }
}