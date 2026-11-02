package payment.sdk.android.aaniPay.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import payment.sdk.android.sdk.R

@Composable
internal fun CountryCodeView() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colorResource(id = R.color.payment_sdk_surface_color),
        border = BorderStroke(width = 1.dp, color = colorResource(id = R.color.payment_sdk_border_color)),
        modifier = Modifier.clip(MaterialTheme.shapes.small)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 12.dp)
                    .wrapContentSize(Alignment.Center),
                text = "+971",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1
            )
        }
    }
}