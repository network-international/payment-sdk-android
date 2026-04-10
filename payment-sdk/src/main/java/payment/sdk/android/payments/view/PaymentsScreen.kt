package payment.sdk.android.payments.view

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.SDKConfig
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.core.CardType
import payment.sdk.android.googlepay.GooglePayButton
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.sdk.R

@Composable
fun UnifiedPaymentPageScreen(
    modifier: Modifier = Modifier,
    supportedCards: Set<CardType>,
    showWallets: Boolean,
    googlePayUiConfig: GooglePayUiConfig?,
    isSamsungPayAvailable: Boolean,
    formattedAmount: String,
    aaniConfig: AaniPayLauncher.Config?,
    clickToPayConfig: ClickToPayLauncher.Config?,
    onMakePayment: (cardNumber: String, expiry: String, cvv: String, cardholderName: String) -> Unit,
    isProcessing: Boolean,
    onGooglePay: () -> Unit,
    onSamsungPay: () -> Unit,
    onClickAaniPay: (AaniPayLauncher.Config) -> Unit,
    onClickToPay: (ClickToPayLauncher.Config) -> Unit,
    onClose: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<PaymentOption?>(null) }

    // Determine which wallet gets the top banner: Google Pay priority > Samsung Pay
    val primaryWallet: PaymentOption? = when {
        showWallets && googlePayUiConfig != null -> PaymentOption.GOOGLE_PAY
        showWallets && isSamsungPayAvailable -> PaymentOption.SAMSUNG_PAY
        else -> null
    }

    // Other options exclude the primary wallet
    val showGooglePayInOtherOptions = showWallets && googlePayUiConfig != null && primaryWallet != PaymentOption.GOOGLE_PAY
    val showSamsungPayInOtherOptions = showWallets && isSamsungPayAvailable && primaryWallet != PaymentOption.SAMSUNG_PAY
    val showAaniInOtherOptions = showWallets && aaniConfig != null

    val hasOtherOptions = showGooglePayInOtherOptions || showSamsungPayInOtherOptions || showAaniInOtherOptions || clickToPayConfig != null

    val logoResId = if (SDKConfig.merchantLogoResId != 0) {
        SDKConfig.merchantLogoResId
    } else {
        R.drawable.network_international_logo
    }

    Column(modifier.background(Color.White).testTag("sdk_paymentpage_container_main")) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides if (SDKConfig.getLanguage() == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                // X close button at top right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 4.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.align(Alignment.TopEnd).testTag("sdk_paymentpage_button_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Logo (merchant logo or default NI logo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Image(
                        painter = painterResource(id = logoResId),
                        contentDescription = "Logo",
                        modifier = Modifier.height(40.dp).testTag("sdk_paymentpage_image_logo"),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Total amount
                if (SDKConfig.showOrderAmount) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.order_summary),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W400,
                            color = Color.Gray
                        )
                        Text(
                            text = formattedAmount,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.testTag("sdk_paymentpage_label_amount")
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Primary wallet banner
                if (primaryWallet == PaymentOption.GOOGLE_PAY && googlePayUiConfig != null) {
                    GooglePayButton(
                        enabled = !isProcessing,
                        onClick = {
                            if (!isProcessing) {
                                onGooglePay()
                            }
                        },
                        radius = 8.dp,
                        allowedPaymentMethods = googlePayUiConfig.allowedPaymentMethods,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp)
                            .testTag("sdk_paymentpage_button_googlePay")
                    )
                    Spacer(Modifier.height(8.dp))
                    // Terms text below banner
                    TermsAgreementText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                } else if (primaryWallet == PaymentOption.SAMSUNG_PAY) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp)
                            .testTag("sdk_paymentpage_button_samsungPay"),
                        onClick = {
                            if (!isProcessing) {
                                onSamsungPay()
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Black,
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.samsung_pay_logo),
                            contentDescription = stringResource(R.string.samsung_pay_button),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Terms text below banner
                    TermsAgreementText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // "Or select your payment options" separator
                if (primaryWallet != null) {
                    Text(
                        text = stringResource(R.string.or_select_payment_options),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF8F8F8F),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Card payment section (collapsible)
                CardPaymentSection(
                    supportedCards = supportedCards,
                    formattedAmount = formattedAmount,
                    isExpanded = selectedOption == PaymentOption.CARD,
                    onToggle = {
                        selectedOption = if (selectedOption == PaymentOption.CARD) null else PaymentOption.CARD
                    },
                    onMakePayment = onMakePayment
                )

                // Other payment options (excluding primary wallet)
                if (hasOtherOptions) {
                    Spacer(Modifier.height(24.dp))

                    OtherPaymentOptionsSection(
                        selectedOption = selectedOption,
                        googlePayUiConfig = if (showGooglePayInOtherOptions) googlePayUiConfig else null,
                        isSamsungPayAvailable = showSamsungPayInOtherOptions,
                        aaniConfig = if (showAaniInOtherOptions) aaniConfig else null,
                        clickToPayConfig = clickToPayConfig,
                        onGooglePay = onGooglePay,
                        onSamsungPay = onSamsungPay,
                        onClickAaniPay = onClickAaniPay,
                        onClickToPay = onClickToPay,
                        onOptionSelected = { option ->
                            selectedOption = option
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                PaymentFooterView(supportedCards = supportedCards)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun UnifiedPaymentPageScreenPreview() {
    Box {
        UnifiedPaymentPageScreen(
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
            isSamsungPayAvailable = false,
            onMakePayment = { _, _, _, _ -> },
            onGooglePay = {},
            onSamsungPay = {},
            aaniConfig = null,
            clickToPayConfig = null,
            isProcessing = false,
            onClickAaniPay = {},
            onClickToPay = {},
            onClose = {}
        )
    }
}

@Composable
private fun TermsAgreementText(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    val context = LocalContext.current
    val fullText = stringResource(R.string.terms_agreement_text)
    val termsLinkText = stringResource(R.string.terms_and_conditions)
    val annotated = remember(fullText, termsLinkText) {
        buildAnnotatedString {
            val start = fullText.indexOf(termsLinkText, ignoreCase = true)
            if (start >= 0) {
                append(fullText.substring(0, start))
                pushStringAnnotation(tag = "URL", annotation = "https://www.network.ae/en/terms-and-conditions")
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(fullText.substring(start, start + termsLinkText.length))
                }
                pop()
                append(fullText.substring(start + termsLinkText.length))
            } else {
                append(fullText)
            }
        }
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            textAlign = textAlign,
            color = Color(0xFF8F8F8F),
            fontSize = 11.sp,
            lineHeight = 16.sp
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                }
        }
    )
}
