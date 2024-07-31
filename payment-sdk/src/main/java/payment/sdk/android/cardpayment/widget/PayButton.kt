package payment.sdk.android.cardpayment.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import payment.sdk.android.sdk.R

@Composable
fun PayButton(
    text: String,
    isValid: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 8.dp),
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = if (isValid) {
                colorResource(id = R.color.payment_sdk_pay_button_background_color)
            } else {
                Color.Gray
            },
        ),
        onClick = onClick,
        enabled = isValid,
        shape = RoundedCornerShape(percent = 15),
    ) {
        Text(
            text = text,
            color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
        )
    }
}