package payment.sdk.android.payments.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import payment.sdk.android.payments.CreditCardVisualTransformation
import payment.sdk.android.cardpayment.card.CardDetector
import payment.sdk.android.cardpayment.card.PaymentCard
import payment.sdk.android.cardpayment.card.SpacingPatterns
import payment.sdk.android.savedCard.view.getCardImage
import payment.sdk.android.cardpayment.theme.SDKTextFieldColors
import payment.sdk.android.cardpayment.validation.Luhn
import payment.sdk.android.core.CardType
import payment.sdk.android.sdk.R

@Composable
fun CardNumberTextField(
    modifier: Modifier = Modifier,
    pan: String,
    paymentCard: PaymentCard?,
    supportedCards: Set<CardType>,
    onValueChanged: (String) -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Box {
            TextField(
                label = { Text(stringResource(R.string.card_number_label_title)) },
                value = pan,
                isError = pan.length > 5 && Luhn.isValidPan(pan) && paymentCard == null,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onValueChanged,
                visualTransformation = CreditCardVisualTransformation(
                    paymentCard?.binRange?.length?.pattern ?: SpacingPatterns.Default
                ),
                textStyle = MaterialTheme.typography.subtitle1,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                ),
                colors = SDKTextFieldColors(),
                placeholder = { Text("0000 0000 0000 0000") },
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = paymentCard != null,
                modifier = Modifier
                    .height(8.dp)
                    .padding(horizontal = 2.dp)
                    .align(Alignment.CenterEnd),
                enter = scaleIn(animationSpec = tween(500)),  // Expand from center
                exit = scaleOut(animationSpec = tween(500))   // Shrink towards center
            ) {
                Image(
                    painter = getCardImage(paymentCard?.type, isWhiteBackground = true),
                    contentDescription = "Image",
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(
            visible = paymentCard == null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + expandVertically(animationSpec = tween(durationMillis = 500)), // Expand from top to bottom
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ) + shrinkVertically(animationSpec = tween(durationMillis = 500))
        ) {
            Row {
                supportedCards.forEach { card ->
                    Image(
                        modifier = Modifier
                            .height(10.dp)
                            .padding(end = 2.dp),
                        painter = getCardImage(card, isWhiteBackground = true),
                        contentDescription = "Image",
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CardNumberTextFieldPreview() {
    Box(Modifier.background(Color.White)) {
        CardNumberTextField(
            Modifier.fillMaxWidth(),
            "",
            CardDetector(setOf(CardType.Visa, CardType.MasterCard)).detect("41111"),
            setOf(CardType.Visa, CardType.MasterCard)
        ) {

        }
    }
}