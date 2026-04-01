package payment.sdk.android.payments.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun PaymentRadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedColor = Color(0xFF0069B1)
    val unselectedColor = Color(0xFFDADADA)

    Canvas(modifier = modifier.size(18.dp).testTag("sdk_paymentpage_radio_${if (selected) "selected" else "unselected"}")) {
        if (selected) {
            drawCircle(
                color = selectedColor,
                radius = size.minDimension / 2,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = selectedColor,
                radius = 4.5.dp.toPx()
            )
        } else {
            drawCircle(
                color = unselectedColor,
                radius = size.minDimension / 2,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
