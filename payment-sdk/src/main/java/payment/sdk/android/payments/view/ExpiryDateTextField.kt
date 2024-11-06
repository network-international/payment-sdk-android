package payment.sdk.android.payments.view

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import payment.sdk.android.cardpayment.theme.SDKTextFieldColors
import payment.sdk.android.cardpayment.widget.ExpireDateEditText
import payment.sdk.android.sdk.R

@Composable
fun ExpiryDateTextField(
    modifier: Modifier = Modifier,
    text: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusCvv: () -> Unit
) {
    TextField(
        value = text,
        onValueChange = { newValue ->
            if (newValue.text.length <= 5) {
                var rawText = newValue.text.filter { it.isDigit() }
                if (rawText.isBlank()) {
                    onValueChange(newValue.copy(text = "", selection = TextRange(0)))
                    return@TextField
                }

                rawText = if (rawText.length >= 3) {
                    rawText.take(2) + "/" + rawText.drop(2)
                } else {
                    rawText
                }
                if (ExpireDateEditText.isValidExpire(rawText)) {
                    onValueChange(
                        newValue.copy(
                            text = rawText,
                            selection = TextRange(rawText.length)
                        )
                    )
                    if (rawText.length == 5) {
                        focusCvv()
                    }
                }
            }
        },
        modifier = modifier,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        label = { Text(stringResource(R.string.placeholder_expire_date)) },
        colors = SDKTextFieldColors()
    )
}