package payment.sdk.android.cardpayment.aaniPay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AaniPayTimerScreen(amount: String) {
    var remainingTime by remember { mutableIntStateOf(3 * 60) } // 3 minutes in seconds
    val minutes = remainingTime / 60
    val seconds = remainingTime % 60

    LaunchedEffect(Unit) {
        while (remainingTime > 0) {
            delay(1000L) // 1 second delay
            remainingTime -= 1
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Box(modifier = Modifier.fillMaxSize()) {
        TimerView(Modifier.align(Alignment.Center), minutes, seconds, amount)
    }
}