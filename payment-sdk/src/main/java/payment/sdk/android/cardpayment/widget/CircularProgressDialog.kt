package payment.sdk.android.cardpayment.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import payment.sdk.android.sdk.R

@Composable
fun CircularProgressDialog(message: LoadingMessage) {
    Dialog(
        onDismissRequest = { },
        DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val messageText = stringResource(id = when (message) {
            LoadingMessage.AUTH -> R.string.message_authorizing
            LoadingMessage.PAYMENT -> R.string.submitting_payment
            LoadingMessage.LOADING_ORDER -> R.string.message_loading_order_details
            LoadingMessage.THREE_DS -> R.string.launching_3d_secure
        })
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.White),
            elevation = 16.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                CircularProgressIndicator(color = colorResource(id = R.color.payment_sdk_progress_loader_color))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = messageText, color = Color.Gray)
            }
        }
    }
}

enum class LoadingMessage {
    AUTH, PAYMENT, LOADING_ORDER, THREE_DS
}

@Preview(name = "PIXEL_4", device = Devices.PIXEL_4_XL)
@Composable
fun CircularProgressDialogPreview() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressDialog(message = LoadingMessage.PAYMENT)
    }
}
