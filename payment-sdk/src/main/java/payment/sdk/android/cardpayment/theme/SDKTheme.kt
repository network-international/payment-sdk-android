package payment.sdk.android.cardpayment.theme

import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import payment.sdk.android.sdk.R

@Composable
internal fun SDKTextFieldColors(
    cursorColor: Color = colorResource(id = R.color.payment_sdk_text_field_cursor_color),
    textColor: Color = colorResource(id = R.color.payment_sdk_text_field_text_color),
    placeholderColor: Color = colorResource(id = R.color.payment_sdk_text_field_placeholder_color),
    backgroundColor: Color = colorResource(id = R.color.payment_sdk_text_field_background_color),
    unfocusedIndicatorColor: Color = colorResource(id = R.color.payment_sdk_text_field_unfocused_indicator_color),
    focusedIndicatorColor: Color = colorResource(id = R.color.payment_sdk_text_field_focused_indicator_color),
    focusedLabelColor: Color = colorResource(id = R.color.payment_sdk_text_field_focused_label_color),
    errorIndicatorColor: Color = colorResource(id = R.color.payment_sdk_text_field_error_indicator_color),
    unfocusedLabelColor: Color = colorResource(id = R.color.payment_sdk_text_field_unfocused_label_color),
) = TextFieldDefaults.textFieldColors(
    cursorColor = cursorColor,
    textColor = textColor,
    placeholderColor = placeholderColor,
    backgroundColor = backgroundColor,
    focusedIndicatorColor = focusedIndicatorColor,
    focusedLabelColor = focusedLabelColor,
    unfocusedIndicatorColor = unfocusedIndicatorColor,
    errorIndicatorColor = errorIndicatorColor,
    unfocusedLabelColor = unfocusedLabelColor,
)
