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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import payment.sdk.android.cardpayment.SDKTheme
import payment.sdk.android.cardpayment.savedCard.SavedCardDto

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedCardPaymentView(
    savedCard: SavedCardDto,
    amount: Int,
    currency: String,
    onStartPayment: (cvv: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = BringIntoViewRequester()
    val focusRequester = FocusRequester()

    var cvv by remember { mutableStateOf("") }

    var isErrorCvv by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(text = "Saved Card payment")
            })
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(contentPadding)
                .fillMaxSize()
        ) {

            CardView(savedCard = savedCard)

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
                            isErrorCvv = if (cvv.length in 3..4) {
                                onStartPayment(cvv)
                                false
                            } else {
                                true
                            }
                        }
                    ),
                    onValueChange = {
                        if (it.length <= 4) {
                            cvv = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    isError = isErrorCvv,
                    label = {
                        Text(text = "CVV")
                    },
                    placeholder = {
                        Text(text = "CVV (required)")
                    })
                if (isErrorCvv) {
                    Text(
                        text = "CVV is required",
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
                if (cvv.length in 3..4) {
                    isErrorCvv = false
                    onStartPayment(cvv)
                } else {
                    isErrorCvv = true
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
            amount = 134,
            currency = "AED"
        ) { }
    }
}

val String.color
    get() = Color(android.graphics.Color.parseColor(this))