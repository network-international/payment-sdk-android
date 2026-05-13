package payment.sdk.android.payments.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import payment.sdk.android.cardpayment.card.PaymentCard
import payment.sdk.android.cardpayment.card.SpacingPatterns
import payment.sdk.android.payments.CreditCardVisualTransformation
import payment.sdk.android.payments.SliceCheckState
import payment.sdk.android.payments.theme.PgSize
import payment.sdk.android.savedCard.view.getCardImage
import payment.sdk.android.sdk.R

@Composable
fun CardNumberTextField(
    modifier: Modifier = Modifier,
    pan: String,
    paymentCard: PaymentCard?,
    sliceCheckState: SliceCheckState = SliceCheckState.Idle,
    isError: Boolean = false,
    errorText: String? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    onValueChanged: (String) -> Unit
) {
    PgTextField(
        modifier = modifier,
        value = pan,
        onValueChange = onValueChanged,
        label = stringResource(R.string.card_number_label_title),
        placeholder = "0000 0000 0000 0000",
        trailingIcon = if (paymentCard != null) {
            {
                Image(
                    modifier = Modifier.size(PgSize.optionLogoSize * 0.7f),
                    painter = getCardImage(paymentCard.type, isWhiteBackground = true),
                    contentDescription = paymentCard.type?.name,
                    contentScale = ContentScale.Fit
                )
            }
        } else null,
        statusLine = { SliceEligibilityRow(state = sliceCheckState) },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        visualTransformation = CreditCardVisualTransformation(
            paymentCard?.binRange?.length?.pattern ?: SpacingPatterns.Default
        ),
        isError = isError,
        errorText = errorText,
        onFocusChanged = onFocusChanged,
        testTag = "sdk_cardinput_field_pan"
    )
}
