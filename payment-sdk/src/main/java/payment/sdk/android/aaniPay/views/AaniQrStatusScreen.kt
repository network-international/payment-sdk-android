package payment.sdk.android.aaniPay.views

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.cardpayment.theme.sdkColor
import payment.sdk.android.sdk.R

internal enum class QrStatusType {
    EXPIRED, FAILED, TIMEOUT
}

@Composable
internal fun AaniQrStatusScreen(
    statusType: QrStatusType,
    onAction: () -> Unit,
    onCancel: () -> Unit
) {
    val goldColor = sdkColor(R.color.payment_sdk_pay_button_gold)
    val goldTextColor = sdkColor(R.color.payment_sdk_pay_button_gold_text)
    val pinkBorder = Color(0xFFF2D6D6)

    val iconEmoji: String
    val borderColor: Color
    val title: String
    val subtitle: String
    val buttonText: String

    when (statusType) {
        QrStatusType.EXPIRED -> {
            iconEmoji = "\u26A0\uFE0F"
            borderColor = goldColor
            title = stringResource(R.string.aani_qr_expired)
            subtitle = stringResource(R.string.aani_qr_expired_message)
            buttonText = stringResource(R.string.aani_generate_new_qr)
        }
        QrStatusType.FAILED -> {
            iconEmoji = "\u274C"
            borderColor = pinkBorder
            title = stringResource(R.string.aani_qr_failed)
            subtitle = stringResource(R.string.aani_qr_failed_message)
            buttonText = stringResource(R.string.aani_try_again)
        }
        QrStatusType.TIMEOUT -> {
            iconEmoji = "\u23F0"
            borderColor = pinkBorder
            title = stringResource(R.string.aani_payment_timeout)
            subtitle = stringResource(R.string.aani_payment_timeout_message)
            buttonText = stringResource(R.string.aani_try_again)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Bordered status container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 3.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = iconEmoji,
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.testTag("sdk_aanistatus_label_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = TextStyle(
                    fontWeight = FontWeight.W400,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action button (gold background)
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = goldColor
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(0.dp),
            modifier = Modifier.fillMaxWidth().testTag("sdk_aanistatus_button_action")
        ) {
            Text(
                text = buttonText,
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = goldTextColor
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cancel button
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.LightGray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(0.dp),
            modifier = Modifier.fillMaxWidth().testTag("sdk_aanistatus_button_cancel")
        ) {
            Text(
                text = stringResource(R.string.cancel_button),
                style = TextStyle(
                    fontWeight = FontWeight.W500,
                    fontSize = 16.sp
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
