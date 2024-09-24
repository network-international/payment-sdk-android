package payment.sdk.android.cardpayment.theme

import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal fun SDKTextFieldColors(
    cursorColor: Color = Color.Black,
    textColor: Color = Color.Black,
    placeholderColor: Color = Color.Gray,
    backgroundColor: Color = Color.White,
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