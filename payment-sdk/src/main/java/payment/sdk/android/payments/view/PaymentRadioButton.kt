package payment.sdk.android.payments.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import payment.sdk.android.core.testId
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.payments.theme.PgSize

@Composable
fun PaymentRadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(PgSize.radioOuter).testId("sdk_paymentpage_radio_${if (selected) "selected" else "unselected"}")) {
        if (selected) {
            drawCircle(
                color = PgColors.accentPrimary,
                radius = size.minDimension / 2,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = PgColors.accentPrimary,
                radius = PgSize.radioInner.toPx() / 2
            )
        } else {
            drawCircle(
                color = PgColors.borderInput,
                radius = size.minDimension / 2,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
