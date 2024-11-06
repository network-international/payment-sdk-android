package payment.sdk.android.savedCard.view

import android.util.Log
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import payment.sdk.android.payments.theme.SDKTheme
import payment.sdk.android.cardpayment.widget.PreviewTextView
import payment.sdk.android.core.CardType
import payment.sdk.android.sdk.R

@Composable
internal fun CreditCardView(
    modifier: Modifier = Modifier,
    cardNumber: String,
    cardScheme: CardType?,
    cardholderName: String,
    expiry: String
) {
    Surface(
        modifier = modifier
            .aspectRatio(16 / 9f),
        shape = RoundedCornerShape(8.dp),
        elevation = 8.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        colorResource(id = R.color.payment_sdk_card_start_color),
                        colorResource(id = R.color.payment_sdk_card_center_color),
                        colorResource(id = R.color.payment_sdk_card_end_color),
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .height(48.dp)
            ) {
                AnimatedVisibility(
                    visible = cardScheme != null,
                    enter = scaleIn(animationSpec = tween(500)),  // Expand from center
                    exit = scaleOut(animationSpec = tween(0))   // Shrink towards center
                ) {
                    Image(
                        painter = getCardImage(type = cardScheme),
                        contentDescription = "",
                        contentScale = ContentScale.FillHeight
                    )
                }

                if (cardScheme == null) {
                    Image(
                        painter = painterResource(R.drawable.ic_card_back_chip),
                        contentDescription = "",
                        contentScale = ContentScale.FillHeight
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 32.dp, bottom = 16.dp)
                    .align(Alignment.BottomStart)
            ) {
                val cardNumberText = cardNumber.replace("....".toRegex(), "$0 ").ifBlank {
                    stringResource(R.string.placeholder_card_number)
                }
                AndroidView(factory = { context ->
                    PreviewTextView(context).apply {
                        text = cardNumberText
                        layoutDirection = View.LAYOUT_DIRECTION_LTR
                    }
                }, update = {
                    it.text = cardNumberText
                })

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(start = 96.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "EXPIRES\nEND", fontSize = 6.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    val expiryText = expiry.ifBlank {
                        stringResource(R.string.placeholder_expire_date)
                    }
                    AndroidView(factory = { context ->
                        PreviewTextView(context).apply {
                            text = expiryText
                            layoutDirection = View.LAYOUT_DIRECTION_LTR
                        }
                    }, update = {
                        it.text = expiryText
                    })
                }

                Spacer(modifier = Modifier.height(16.dp))
                Log.d("CardNumberTextField", "cardholderName: $cardholderName")
                val cardholderNameText = cardholderName.ifBlank {
                    stringResource(R.string.cardholder_field_hint)
                }
                Log.d("CardNumberTextField", "cardholderNameText: $cardholderNameText")
                AndroidView(factory = { context ->
                    PreviewTextView(context).apply {
                        text = cardholderNameText
                        layoutDirection = View.LAYOUT_DIRECTION_LTR
                    }
                }, update = {
                    it.text = cardholderNameText.uppercase()
                })
            }
        }
    }
}

@Composable
fun getCardImage(type: CardType?, isWhiteBackground: Boolean = false): Painter {
    return painterResource(
        id = when (type) {
            CardType.MasterCard -> R.drawable.ic_logo_mastercard
            CardType.Visa -> R.drawable.ic_logo_visa
            CardType.AmericanExpress -> R.drawable.ic_logo_amex
            CardType.DinersClubInternational -> if (isWhiteBackground) {
                R.drawable.ic_logo_dinners_club
            } else {
                R.drawable.ic_logo_dinners_club_white
            }

            CardType.JCB -> R.drawable.ic_logo_jcb
            CardType.Discover -> R.drawable.ic_logo_discover
            else -> R.drawable.ic_card_back_chip
        }
    )
}

@Preview
@Composable
fun PreviewCardView() {
    SDKTheme {
        CreditCardView(
            modifier = Modifier,
            cardholderName = "Cardholder name",
            expiry = "2025-08",
            cardNumber = "230377******0275",
            cardScheme = CardType.MasterCard
        )
    }
}