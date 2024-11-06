package payment.sdk.android.payments.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import payment.sdk.android.SDKConfig
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.cardpayment.card.CardDetector
import payment.sdk.android.cardpayment.card.CardValidator
import payment.sdk.android.cardpayment.card.PaymentCard
import payment.sdk.android.savedCard.view.CreditCardBack
import payment.sdk.android.savedCard.view.CreditCardView
import payment.sdk.android.cardpayment.theme.SDKTextFieldColors
import payment.sdk.android.core.CardType
import payment.sdk.android.googlepay.GooglePayButton
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.sdk.R

@Composable
fun PaymentsScreen(
    modifier: Modifier = Modifier,
    supportedCards: Set<CardType>,
    showWallets: Boolean,
    googlePayUiConfig: GooglePayUiConfig?,
    formattedAmount: String,
    aaniConfig: AaniPayLauncher.Config?,
    onMakePayment: (cardNumber: String, expiry: String, cvv: String, cardholderName: String) -> Unit,
    onGooglePay: () -> Unit,
    onClickAaniPay: (AaniPayLauncher.Config) -> Unit
) {
    val cardDetector = remember { CardDetector(supportedCards) }
    var pan by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf(TextFieldValue("")) }

    val expiryFocus = remember { FocusRequester() }
    val cvvFocus = remember { FocusRequester() }
    val cardHolderFocus = remember { FocusRequester() }

    var cardholderName by remember { mutableStateOf("") }
    var paymentCard by remember { mutableStateOf<PaymentCard?>(null) }

    var isFormValid by remember { mutableStateOf(false) }

    var isCvvFocused by remember { mutableStateOf(false) } // Track the focus state of CVV
    val rotationAngle by animateFloatAsState(targetValue = if (isCvvFocused) 180f else 0f) // Animate between 0 and 180 degrees


    LaunchedEffect(pan, cvv, expiry.text, cardholderName) {
        isFormValid = CardValidator.isValid(
            paymentCard = paymentCard,
            pan = pan,
            cvv = cvv,
            expiry = expiry.text,
            cardholderName = cardholderName
        )
    }

    Column(modifier.background(Color(0xFFF1F1F1))) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .aspectRatio(16 / 9f),
                contentAlignment = Alignment.Center
            ) {
                if (isCvvFocused) {
                    CreditCardBack(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = rotationAngle
                                cameraDistance = 12f * density
                            }
                    )

                } else {
                    CreditCardView(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = rotationAngle
                                cameraDistance = 12f * density
                            },
                        cardNumber = pan,
                        cardholderName = cardholderName,
                        expiry = expiry.text,
                        cardScheme = paymentCard?.type
                    )
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 8.dp,
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                            TextField(
                                label = { Text(stringResource(R.string.card_cvv_label_title)) },
                                value = cvv,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(cvvFocus)
                                    .onFocusChanged {
                                        isCvvFocused = it.isFocused
                                    },
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
                                colors = SDKTextFieldColors(),
                            )
                        }

                        TextField(
                            label = { Text(stringResource(R.string.card_cardholder_label_title)) },
                            value = cardholderName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(cardHolderFocus),
                            onValueChange = { text ->
                                cardholderName = text
                            },
                            colors = SDKTextFieldColors(),
                        )

                        val animated = animateColorAsState(
                            if (isFormValid) colorResource(id = R.color.payment_sdk_pay_button_background_color) else Color.Gray,
                            label = ""
                        )
                        TextButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .height(46.dp)
                                .background(
                                    color = animated.value,
                                    shape = RoundedCornerShape(percent = 15)
                                ),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White,
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
                            shape = RoundedCornerShape(percent = 15),
                        ) {
                            val title = if (SDKConfig.showOrderAmount) stringResource(
                                R.string.pay_button_title,
                                formattedAmount
                            ) else stringResource(R.string.make_payment)
                            Text(
                                text = title,
                                color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
                            )
                        }
                    }
                }
            }

            if (showWallets) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 8.dp,
                    color = Color.White
                ) {
                    Column {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            text = stringResource(R.string.payments_wallets_title),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.subtitle2
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        googlePayUiConfig?.let {
                            GooglePayButton(
                                onClick = onGooglePay,
                                radius = 8.dp,
                                allowedPaymentMethods = it.allowedPaymentMethods,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        aaniConfig?.let { config ->
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .height(46.dp),
                                onClick = { onClickAaniPay(config) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.White,
                                ),
                                border = BorderStroke(width = 1.dp, Color.Gray),
                                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                                shape = RoundedCornerShape(percent = 15),
                            ) {
                                Image(painter = painterResource(R.drawable.aani_logo), "")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth(0.5f),
                painter = painterResource(id = R.drawable.network_international_logo),
                contentDescription = ""
            )
        }
    }
}

@Preview
@Composable
fun Preview() {
    Box {
        PaymentsScreen(
            supportedCards = setOf(
                CardType.Visa,
                CardType.MasterCard,
                CardType.AmericanExpress,
                CardType.JCB,
                CardType.DinersClubInternational,
                CardType.Discover
            ),
            showWallets = false,
            formattedAmount = "100 AED",
            googlePayUiConfig = null,
            onMakePayment = { _, _, _, _ -> },
            onGooglePay = {},
            aaniConfig = null,
            onClickAaniPay = {}
        )
    }
}