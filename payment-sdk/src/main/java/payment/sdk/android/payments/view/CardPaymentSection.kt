package payment.sdk.android.payments.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.SDKConfig
import payment.sdk.android.cardpayment.card.CardDetector
import payment.sdk.android.cardpayment.card.CardValidator
import payment.sdk.android.cardpayment.card.PaymentCard
import payment.sdk.android.cardpayment.theme.SDKOutlinedTextFieldColors
import payment.sdk.android.core.CardType
import payment.sdk.android.cardpayment.theme.sdkColor
import payment.sdk.android.savedCard.view.getCardImage
import payment.sdk.android.sdk.R

@Composable
fun CardPaymentSection(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    supportedCards: Set<CardType>,
    formattedAmount: String,
    onToggle: () -> Unit,
    onMakePayment: (cardNumber: String, expiry: String, cvv: String, cardholderName: String) -> Unit
) {
    val cardDetector = remember { CardDetector(supportedCards) }
    var pan by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf(TextFieldValue("")) }
    var cardholderName by remember { mutableStateOf("") }
    var paymentCard by remember { mutableStateOf<PaymentCard?>(null) }
    var isFormValid by remember { mutableStateOf(false) }

    val expiryFocus = remember { FocusRequester() }
    val cvvFocus = remember { FocusRequester() }
    val cardHolderFocus = remember { FocusRequester() }

    LaunchedEffect(pan, cvv, expiry.text, cardholderName) {
        isFormValid = CardValidator.isValid(
            paymentCard = paymentCard,
            pan = pan,
            cvv = cvv,
            expiry = expiry.text,
            cardholderName = cardholderName
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        // Title: "Use Credit Or Debit Card"
        Text(
            text = stringResource(R.string.use_credit_or_debit_card),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF070707),
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        )

        // Card brand logos row
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            supportedCards.forEach { card ->
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = getCardImage(card, isWhiteBackground = true),
                    contentDescription = card.name,
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Radio button: "Pay by card"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(start = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaymentRadioButton(selected = isExpanded)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.pay_by_card),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF070707)
            )
        }

        // Collapsible card form
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
        ) {
            Column(modifier = Modifier.clip(RectangleShape)) {
                // Card number
                CardNumberTextField(
                    pan = pan,
                    paymentCard = paymentCard,
                    supportedCards = supportedCards,
                ) { text ->
                    val maxLength = paymentCard?.binRange?.length?.value ?: 16
                    if (text.length <= maxLength) {
                        pan = text.filter { it.isDigit() }
                        if (pan.length == maxLength) {
                            expiryFocus.requestFocus()
                        }
                        paymentCard = takeIf { pan.isNotEmpty() }?.let {
                            cardDetector.detect(pan)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Expiration date + Security code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExpiryDateTextField(
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(expiryFocus),
                        text = expiry,
                        onValueChange = { newValue ->
                            expiry = newValue
                        },
                        focusCvv = {
                            cvvFocus.requestFocus()
                        }
                    )

                    OutlinedTextField(
                        label = { Text(stringResource(R.string.security_code_label)) },
                        value = cvv,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(cvvFocus),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        onValueChange = { text ->
                            val maxLength = paymentCard?.cvv?.length ?: 3
                            if (text.length <= maxLength) {
                                cvv = text
                                if (text.length == maxLength) {
                                    cardHolderFocus.requestFocus()
                                }
                            }
                        },
                        colors = SDKOutlinedTextFieldColors(),
                    )
                }

                // "What's CVV?" + tooltip
                var showCvvTooltip by remember { mutableStateOf(false) }

                Text(
                    text = stringResource(R.string.whats_cvv),
                    fontSize = 12.sp,
                    color = Color(0xFF8F8F8F),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCvvTooltip = !showCvvTooltip }
                        .padding(top = 4.dp),
                    textAlign = TextAlign.End
                )

                AnimatedVisibility(visible = showCvvTooltip) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Arrow pointing at "What's CVV?"
                        Box(
                            modifier = Modifier
                                .padding(end = 24.dp)
                                .size(width = 16.dp, height = 8.dp)
                                .clip(
                                    GenericShape { size, _ ->
                                        moveTo(0f, size.height)
                                        lineTo(size.width / 2f, 0f)
                                        lineTo(size.width, size.height)
                                        close()
                                    }
                                )
                                .background(Color(0xFF333333))
                        )
                        // Bubble
                        Text(
                            text = stringResource(R.string.cvv_tooltip),
                            fontSize = 13.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFF333333),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Name on card
                OutlinedTextField(
                    label = { Text(stringResource(R.string.name_on_card_label)) },
                    value = cardholderName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(cardHolderFocus),
                    onValueChange = { text ->
                        cardholderName = text
                    },
                    colors = SDKOutlinedTextFieldColors(),
                )

                Spacer(Modifier.height(20.dp))

                // Pay button
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            color = if (isFormValid) sdkColor(R.color.payment_sdk_pay_button_background_color) else sdkColor(R.color.payment_sdk_button_disabled_background_color),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    onClick = {
                        onMakePayment(
                            pan,
                            expiry.text.filter { it.isDigit() },
                            cvv,
                            cardholderName
                        )
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    val title = if (SDKConfig.showOrderAmount) stringResource(
                        R.string.pay_button_title,
                        formattedAmount
                    ) else stringResource(R.string.pay_button)
                    Text(
                        text = title,
                        color = if (isFormValid) sdkColor(R.color.payment_sdk_pay_button_text_color) else sdkColor(R.color.payment_sdk_button_disabled_text_color),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                // Terms agreement text
                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.terms_agreement_text),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    color = Color(0xFF8F8F8F),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
