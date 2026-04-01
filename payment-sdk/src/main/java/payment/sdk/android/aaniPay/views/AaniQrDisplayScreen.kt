package payment.sdk.android.aaniPay.views

import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.TextUtilsCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.delay
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.sdk.R
import java.util.Locale

@Composable
internal fun AaniQrDisplayScreen(
    amount: Double,
    currencyCode: String,
    qrContent: String,
    onExpired: () -> Unit,
    onCancel: () -> Unit
) {
    var remainingTime by remember { mutableIntStateOf(5 * 60) }
    val minutes = remainingTime / 60
    val seconds = remainingTime % 60
    val isLtr =
        TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR
    val formattedAmount = OrderAmount(amount, currencyCode).formattedCurrencyString2Decimal(isLtr)

    val qrBitmap = remember(qrContent) {
        generateAaniQrBitmap(qrContent, 512)
    }

    LaunchedEffect(Unit) {
        while (remainingTime > 0) {
            delay(1000L)
            remainingTime -= 1
        }
        onExpired()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.aani_scan_qr_to_pay),
            style = TextStyle(
                fontWeight = FontWeight.W500,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // QR container with white exclusion zone
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(16.dp)
                        .testTag("sdk_aani_image_qrCode")
                )
            }

            // Aani logo overlay in center
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.aani_qr_logo),
                    contentDescription = "Aani",
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Timer with clock icon
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u23F0",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.testTag("sdk_aani_label_timer")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.aani_note_do_not_close),
            style = TextStyle(
                fontWeight = FontWeight.W400,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Amount row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.aani_paying_amount, ""),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Text(
                text = formattedAmount,
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cancel button
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.LightGray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(0.dp),
            modifier = Modifier.fillMaxWidth().testTag("sdk_aani_button_cancel")
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
    }
}

/**
 * Generates a QR bitmap with "H" error correction level to support the center logo overlay.
 * Creates a 1:1 pixel-per-module bitmap then scales up with nearest-neighbor
 * to ensure uniform module widths (matching iOS CIFilter behavior).
 */
private fun generateAaniQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 0
        )
        // Generate at native QR size (1 pixel per module)
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 1, 1, hints)
        val qrSize = bitMatrix.width
        val bitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
        for (x in 0 until qrSize) {
            for (y in 0 until qrSize) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        // Scale up with nearest-neighbor (no filtering) for uniform module widths
        Bitmap.createScaledBitmap(bitmap, size, size, false)
    } catch (e: Exception) {
        null
    }
}
