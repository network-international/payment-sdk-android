package payment.sdk.android.payments.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.sdk.R

@Composable
fun PaymentSectionSeparator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFDBDBDC),
            thickness = 1.dp
        )
        Text(
            text = stringResource(R.string.payment_separator_text),
            modifier = Modifier.padding(horizontal = 12.dp),
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFDBDBDC),
            thickness = 1.dp
        )
    }
}
