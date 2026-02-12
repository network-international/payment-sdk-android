package payment.sdk.android.savedCard.view

import TermsAndConditionsConsent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import payment.sdk.android.core.CardMapping
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.SubscriptionDetails
import payment.sdk.android.core.SavedCard
import payment.sdk.android.payments.RecurringDetailRow
import payment.sdk.android.payments.theme.SDKTheme
import payment.sdk.android.savedCard.isAmex
import payment.sdk.android.sdk.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SavedCardPaymentView(
    modifier: Modifier,
    savedCard: SavedCard,
    orderAmount: OrderAmount,
    isSubscriptionOrder: Boolean,
    subscriptionDetails: SubscriptionDetails?,
    tncUrl: String,
    isSaudiPayment: Boolean,
    orderType: String,
    onStartPayment: (cvv: String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = BringIntoViewRequester()
    val focusRequester = FocusRequester()

    var cvv by remember { mutableStateOf("") }

    val cvvLength = if (savedCard.isAmex()) 4 else 3

    var isErrorCvv by remember { mutableStateOf(false) }
    var isRecurringConsentChecked by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
    ) {

        if (isSubscriptionOrder && subscriptionDetails != null) {
            Surface(
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 10.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = stringResource(R.string.recurring_payment_details),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    Divider(modifier = Modifier.padding(bottom = 4.dp))
                    RecurringDetailRow(stringResource(R.string.first_payment_date), subscriptionDetails.startDate)
                    RecurringDetailRow(stringResource(R.string.last_payment_date), subscriptionDetails.lastPaymentDate)
                    RecurringDetailRow(stringResource(R.string.recurring_amount), subscriptionDetails.amount)
                    RecurringDetailRow(stringResource(R.string.frequency), subscriptionDetails.frequency)
                }
            }
        }

        CreditCardView(
            modifier = Modifier.padding(8.dp),
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
                    .padding(8.dp)
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
        if (!isSaudiPayment) {
            TermsAndConditionsConsent(
                checked = isRecurringConsentChecked,
                onCheckedChange = { isRecurringConsentChecked = it },
                isSubscriptionOrder = isSubscriptionOrder,
                termsUrl = tncUrl,
                orderType = orderType
            )
        }

        SavedCardViewBottomBar(bringIntoViewRequester, orderAmount) {
            isErrorCvv = if (cvv.length == cvvLength) {
                onStartPayment(cvv)
                false
            } else {
                true
            }
        }
    }
}

@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun PreviewContent() {
    SDKTheme {
        SavedCardPaymentView(
            modifier = Modifier,
            savedCard = SavedCard(
                cardholderName = "Name",
                cardToken = "",
                expiry = "2025-08",
                maskedPan = "230377******0275",
                recaptureCsc = false,
                scheme = "VISA"
            ),
            orderAmount = OrderAmount(1.33, "AED"),
            isSubscriptionOrder = true,
            subscriptionDetails = SubscriptionDetails(
                frequency = "Monthly",
                startDate = "2025-10-14",
                amount = "AED 100.00",
                lastPaymentDate = "2035-10-14"
            ),
            tncUrl = "",
            isSaudiPayment = false,
            orderType = "SINGLE"
        ) {

        }
    }
}

val String.color
    get() = Color(android.graphics.Color.parseColor(this))