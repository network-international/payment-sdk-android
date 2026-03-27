package payment.sdk.android.cardpayment.theme

import androidx.annotation.ColorRes
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import payment.sdk.android.SDKConfig
import payment.sdk.android.sdk.R

/**
 * Resolves a color resource, checking SDKConfig for runtime overrides first.
 */
@Composable
internal fun sdkColor(@ColorRes resId: Int): Color {
    return SDKConfig.getColorOverride(resId)?.let { Color(it) } ?: colorResource(resId)
}

@Composable
internal fun SDKTextFieldColors(
    cursorColor: Color = Color.Black,
    textColor: Color = Color.Black,
    placeholderColor: Color = Color.Gray,
    backgroundColor: Color = sdkColor(R.color.payment_sdk_input_field_background_color),
    unfocusedIndicatorColor: Color = Color(0xFFF1F1F1),
    focusedIndicatorColor: Color = Color(0xFFF1F1F1),
    focusedLabelColor: Color = Color.Gray,
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

@Composable
internal fun SDKOutlinedTextFieldColors(
    cursorColor: Color = Color.Black,
    textColor: Color = Color.Black,
    placeholderColor: Color = Color.Gray,
    backgroundColor: Color = sdkColor(R.color.payment_sdk_input_field_background_color),
    unfocusedBorderColor: Color = Color(0xFFDADADA),
    focusedBorderColor: Color = Color(0xFF333333),
    focusedLabelColor: Color = Color(0xFF333333),
    unfocusedLabelColor: Color = Color.Gray,
    errorBorderColor: Color = Color.Red
) = TextFieldDefaults.outlinedTextFieldColors(
    cursorColor = cursorColor,
    textColor = textColor,
    placeholderColor = placeholderColor,
    backgroundColor = backgroundColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedBorderColor = focusedBorderColor,
    focusedLabelColor = focusedLabelColor,
    unfocusedLabelColor = unfocusedLabelColor,
    errorBorderColor = errorBorderColor
)