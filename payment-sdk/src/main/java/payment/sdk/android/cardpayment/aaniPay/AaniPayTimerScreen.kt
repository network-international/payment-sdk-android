package payment.sdk.android.cardpayment.aaniPay

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.text.TextUtilsCompat
import kotlinx.coroutines.delay
import payment.sdk.android.core.OrderAmount
import java.util.Locale

@Composable
fun AaniPayTimerScreen(amount: Double, currencyCode: String) {
    var remainingTime by remember { mutableIntStateOf(3 * 60) } // 3 minutes in seconds
    val minutes = remainingTime / 60
    val seconds = remainingTime % 60
    val isLtr =
        TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR
    val formattedAmount = OrderAmount(amount, currencyCode).formattedCurrencyString2Decimal(isLtr)
    LaunchedEffect(Unit) {
        while (remainingTime > 0) {
            delay(1000L) // 1 second delay
            remainingTime -= 1
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Box(modifier = Modifier.fillMaxSize()) {
        TimerView(Modifier.align(Alignment.Center), minutes, seconds, formattedAmount)
    }
}