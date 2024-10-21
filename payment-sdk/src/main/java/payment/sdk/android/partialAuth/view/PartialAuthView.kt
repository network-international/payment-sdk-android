package payment.sdk.android.partialAuth.view

import android.view.View
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.TextUtilsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.partialAuth.PartialAuthViewModel
import payment.sdk.android.partialAuth.model.PartialAuthActivityArgs
import payment.sdk.android.cardpayment.widget.CircularProgressDialog
import payment.sdk.android.cardpayment.widget.LoadingMessage
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.sdk.R
import java.util.Locale

@Composable
fun PartialAuthView(
    modifier: Modifier = Modifier,
    args: PartialAuthActivityArgs,
    onResult: (CardPaymentData) -> Unit
) {
    val viewModel = viewModel<PartialAuthViewModel>(factory = PartialAuthViewModel.Factory)
    LaunchedEffect(key1 = viewModel.state) {
        viewModel.state.collect { result ->
            onResult(result)
        }
    }
    val isLTR =
        TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR

    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
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
        val approvedAmount = OrderAmount(
            args.partialAmount,
            args.currency
        ).formattedCurrencyString2Decimal(isLTR)

        val fullAmount =
            OrderAmount(args.amount, args.currency).formattedCurrencyString2Decimal(isLTR)
        val titleText = if (args.issuingOrg == null) {
            stringResource(
                R.string.partial_auth_message,
                approvedAmount,
                fullAmount
            )
        } else {
            stringResource(
                R.string.partial_auth_message_with_org,
                args.issuingOrg,
                approvedAmount,
                fullAmount
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
                modifier = Modifier.weight(1f), onClick = {
                    isLoading = true
                    viewModel.submitRequest(args.acceptUrl, args.paymentCookie)
                },
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
                onClick = {
                    isLoading = true
                    viewModel.submitRequest(args.declineUrl, args.paymentCookie)
                },
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

@Preview
@Composable
fun PartialAuthViewPreview() {
    Box {
        PartialAuthView(
            args = PartialAuthActivityArgs(
                partialAmount = 50.0,
                amount = 100.0,
                currency = "USD",
                acceptUrl = "https://example.com/accept",
                declineUrl = "https://example.com/decline",
                issuingOrg = "Example Bank",
                paymentCookie = "dummyPaymentCookie123"
            )
        ) {}
    }
}