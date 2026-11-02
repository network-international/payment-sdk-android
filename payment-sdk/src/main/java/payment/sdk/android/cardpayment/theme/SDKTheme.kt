package payment.sdk.android.cardpayment.theme

import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import payment.sdk.android.sdk.R

@Composable
internal fun SDKTextFieldColors(
    cursorColor: Color = colorResource(id = R.color.payment_sdk_default_label_color),
    textColor: Color = colorResource(id = R.color.payment_sdk_default_label_color),
    placeholderColor: Color = colorResource(id = R.color.payment_sdk_secondary_label_color),
    backgroundColor: Color = colorResource(id = R.color.payment_sdk_surface_color),
    unfocusedIndicatorColor: Color = colorResource(id = R.color.payment_sdk_field_background_color),
    focusedIndicatorColor: Color = colorResource(id = R.color.payment_sdk_field_background_color),
    focusedLabelColor: Color = colorResource(id = R.color.payment_sdk_secondary_label_color),
    errorIndicatorColor: Color = Color.Red
) = TextFieldDefaults.textFieldColors(
    cursorColor = cursorColor,
    textColor = textColor,
    placeholderColor = placeholderColor,
    backgroundColor = backgroundColor,
    focusedIndicatorColor = focusedIndicatorColor,
    focusedLabelColor = focusedLabelColor,
    unfocusedIndicatorColor = unfocusedIndicatorColor,
    errorIndicatorColor = errorIndicatorColor
)