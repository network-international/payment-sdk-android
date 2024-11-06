package payment.sdk.android.savedCard.view

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.TextUtilsCompat
import payment.sdk.android.payments.theme.SDKTheme
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.sdk.R
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SavedCardViewBottomBar(
    bringIntoViewRequester: BringIntoViewRequester,
    orderAmount: OrderAmount,
    onPayClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .fillMaxWidth()
            .imePadding(),
        backgroundColor = Color.White,
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        elevation = 16.dp
    ) {
        val isLTR =
            TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR
        TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(8.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = colorResource(id = R.color.payment_sdk_pay_button_background_color)
            ),
            onClick = {
                onPayClicked()
            },
            shape = RoundedCornerShape(percent = 15),
        ) {
            Text(
                text = stringResource(
                    id = R.string.pay_button_title,
                    orderAmount.formattedCurrencyString2Decimal(isLTR)
                ),
                color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun PreviewSavedCardViewBottomBar() {
    SDKTheme {
        SavedCardViewBottomBar(
            bringIntoViewRequester = BringIntoViewRequester(),
            orderAmount = OrderAmount(1.33, "AED")
        ) {}
    }
}