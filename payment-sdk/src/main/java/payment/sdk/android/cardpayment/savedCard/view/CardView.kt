package payment.sdk.android.cardpayment.savedCard.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import payment.sdk.android.cardpayment.savedCard.SavedCardDto
import payment.sdk.android.cardpayment.widget.PreviewTextView
import payment.sdk.android.sdk.R

@Composable
fun CardView(savedCard: SavedCardDto) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .aspectRatio(16 / 9f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(size = 16.dp),
        elevation = 16.dp
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        "#43474A".color,
                        "#232527".color,
                        "#020202".color,
                    )
                )
            )
        ) {
            Image(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .height(48.dp),
                painter = getCardImage(scheme = savedCard.scheme),
                contentDescription = ""
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomStart)
            ) {
                AndroidView(factory = { context ->
                    PreviewTextView(context).apply {
                        text = savedCard.maskedPan.replace("....".toRegex(), "$0 ")
                    }
                })

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "VALID\nTHRU", fontSize = 6.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    AndroidView(factory = { context ->
                        PreviewTextView(context).apply {
                            text = savedCard.expiry
                        }
                    })
                }

                Spacer(modifier = Modifier.height(16.dp))

                AndroidView(factory = { context ->
                    PreviewTextView(context).apply {
                        text = savedCard.cardholderName.uppercase()
                    }
                })
            }
        }
    }
}

@Composable
private fun getCardImage(scheme: String): Painter {
    return painterResource(
        id = when (scheme) {
            "MASTERCARD" -> R.drawable.ic_logo_mastercard
            "VISA" -> R.drawable.ic_logo_visa
            "AMERICAN_EXPRESS" -> R.drawable.ic_logo_amex
            "DINERS_CLUB_INTERNATIONAL" -> R.drawable.ic_logo_dinners_clup
            "JCB" -> R.drawable.ic_logo_jcb
            else -> R.drawable.ic_card_back_chip
        }
    )
}