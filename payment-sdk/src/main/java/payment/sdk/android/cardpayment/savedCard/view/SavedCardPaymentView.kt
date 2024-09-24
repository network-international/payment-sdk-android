package payment.sdk.android.cardpayment.savedCard.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.SDKTheme
import payment.sdk.android.cardpayment.savedCard.SavedCardDto
import payment.sdk.android.core.CardMapping
import payment.sdk.android.sdk.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedCardPaymentView(
    savedCard: SavedCardDto,
    amount: Double,
    currency: String,
    onStartPayment: (cvv: String) -> Unit,
    onNavigationUp: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = BringIntoViewRequester()
    val focusRequester = FocusRequester()

    var cvv by remember { mutableStateOf("") }

    val cvvLength = if (savedCard.isAmex()) 4 else 3

    var isErrorCvv by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.make_payment),
                        color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
                    )
                },
                backgroundColor = colorResource(id = R.color.payment_sdk_toolbar_color),
                navigationIcon = {
                    IconButton(onClick = { onNavigationUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            tint = colorResource(id = R.color.payment_sdk_toolbar_icon_color),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(contentPadding)
                .fillMaxSize()
        ) {

            CreditCardView(
                cardNumber = savedCard.maskedPan,
                cardholderName = savedCard.cardholderName,
                cardScheme = CardMapping.getCardTypeFromString(savedCard.scheme),
                expiry = savedCard.expiry
            )

            Column {
                TextField(
                    modifier = Modifier
                        .onFocusEvent {
                            if (it.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                        .focusRequester(focusRequester)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    value = cvv,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            isErrorCvv = if (cvv.length == cvvLength) {
                                onStartPayment(cvv)
                                false
                            } else {
                                true
                            }
                        }
                    ),
                    onValueChange = {
                        if (it.length <= cvvLength) {
                            cvv = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    isError = isErrorCvv,
                    label = {
                        Text(text = "CVV")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = {
                        Text(text = "CVV (required)")
                    })
                if (isErrorCvv) {
                    Text(
                        text = "Invalid CVV",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            Spacer(modifier = Modifier.weight(1f))

            SavedCardViewBottomBar(bringIntoViewRequester, amount, currency) {
                isErrorCvv = if (cvv.length == cvvLength) {
                    onStartPayment(cvv)
                    false
                } else {
                    true
                }
            }
        }
    }
}

@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun PreviewContent() {
    SDKTheme {
        SavedCardPaymentView(
            savedCard = SavedCardDto(
                cardholderName = "Name",
                cardToken = "",
                expiry = "2025-08",
                maskedPan = "230377******0275",
                recaptureCsc = false,
                scheme = "JCB"
            ),
            amount = 134.0,
            currency = "AED", {}, {})
    }
}

val String.color
    get() = Color(android.graphics.Color.parseColor(this))