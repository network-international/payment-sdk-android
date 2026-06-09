package payment.sdk.android.payments.view

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import payment.sdk.android.SDKConfig
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.cardpayment.card.CardValidator
import payment.sdk.android.cardpayment.validation.Luhn
import payment.sdk.android.cardpayment.widget.ExpireDateEditText
import payment.sdk.android.cardpayment.card.PaymentCard
import payment.sdk.android.cardpayment.theme.sdkColor
import payment.sdk.android.clicktopay.ClickToPayLauncher
import payment.sdk.android.qpay.QPayLauncher
import payment.sdk.android.core.CardMapping
import payment.sdk.android.core.CardType
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.SliceOffer
import payment.sdk.android.googlepay.GooglePayButton
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.payments.SliceCheckState
import payment.sdk.android.payments.model.OrderItem
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.payments.theme.PgSize
import payment.sdk.android.payments.theme.PgType
import payment.sdk.android.payments.theme.Radius
import payment.sdk.android.payments.theme.Spacing
import payment.sdk.android.payments.VisCheckState
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import payment.sdk.android.savedCard.view.getCardImage
import payment.sdk.android.sdk.R
import payment.sdk.android.core.testId

// ─────────────────────────────────────────────────────────────────────────────
// Public entry-point composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UnifiedPaymentPageScreen(
    modifier: Modifier = Modifier,
    supportedCards: Set<CardType>,
    showWallets: Boolean,
    googlePayUiConfig: GooglePayUiConfig?,
    isSamsungPayAvailable: Boolean,
    formattedAmount: String,
    orderValue: Double = 0.0,
    currencyCode: String = "",
    aaniConfig: AaniPayLauncher.Config?,
    clickToPayConfig: ClickToPayLauncher.Config?,
    qpayConfig: QPayLauncher.Config? = null,
    savedCards: List<SavedCard> = emptyList(),
    orderItems: List<OrderItem> = emptyList(),
    sliceCheckState: SliceCheckState = SliceCheckState.Idle,
    visCheckState: VisCheckState = VisCheckState.Idle,
    onCheckSliceEligibility: (pan: String, expiryRaw: String, cardScheme: String?) -> Unit = { _, _, _ -> },
    onCheckSavedCardEligibility: (savedCard: SavedCard) -> Unit = {},
    onResetSliceCheck: () -> Unit = {},
    onMakePayment: (cardNumber: String, expiry: String, cvv: String, cardholderName: String, sliceOffer: SliceOffer?, visaPlan: InstallmentPlan?) -> Unit,
    onMakeSavedCardPayment: (savedCard: SavedCard, cvv: String?) -> Unit = { _, _ -> },
    isProcessing: Boolean,
    onGooglePay: () -> Unit,
    onSamsungPay: () -> Unit,
    onClickAaniPay: (AaniPayLauncher.Config) -> Unit,
    onClickToPay: (ClickToPayLauncher.Config) -> Unit,
    onClickQPay: (QPayLauncher.Config) -> Unit = {},
    onClose: () -> Unit
) {
    // ── Selection state ──────────────────────────────────────────────────────
    // Pay-by-Card is preselected on every fresh page load. `remember` reseeds
    // this on each entry into composition, so navigating away and back gets a
    // clean default rather than a leaked prior selection.
    var selectedOption by remember { mutableStateOf<PaymentOption?>(PaymentOption.CARD) }
    var selectedSavedCard by remember { mutableStateOf<SavedCard?>(null) }
    var savedCardCvv by remember { mutableStateOf("") }
    var isOrderSummaryExpanded by remember { mutableStateOf(true) }

    // ── Lifted card-form state ───────────────────────────────────────────────
    var cardPan by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf(TextFieldValue("")) }
    var cardholderName by remember { mutableStateOf("") }
    var cardPaymentCard by remember { mutableStateOf<PaymentCard?>(null) }
    var isCardFormValid by remember { mutableStateOf(false) }
    var cardSelectedSliceOffer by remember { mutableStateOf<SliceOffer?>(null) }
    var cardSliceSelectionMade by remember { mutableStateOf(false) }
    var lastSliceKey by remember { mutableStateOf("") }
    var cardSelectedVisaPlan by remember { mutableStateOf<InstallmentPlan?>(null) }
    var visTermsAccepted by remember { mutableStateOf(false) }
    var paymentOptionError by remember { mutableStateOf(false) }

    val visAvailablePlans = (visCheckState as? VisCheckState.Available)?.plans
    LaunchedEffect(visAvailablePlans) {
        // Any change in the available plans wipes prior selection. Never preselect: a stale
        // selection from a previous card would silently bypass the "pick a plan" error gate.
        cardSelectedVisaPlan = null
        visTermsAccepted = false
        paymentOptionError = false
    }

    // Reset slice selection state when offers change (new eligibility result). Pay in Full
    // is preselected inside the slice section, so the user has a valid default selection
    // the moment offers appear — flip the validation gate to "made" as soon as we have a
    // non-empty offer list.
    val sliceAvailableOffers = (sliceCheckState as? SliceCheckState.Available)?.offers
    LaunchedEffect(sliceAvailableOffers) {
        cardSliceSelectionMade = !sliceAvailableOffers.isNullOrEmpty()
        paymentOptionError = false
    }

    LaunchedEffect(cardPan, cardCvv, cardExpiry.text, cardholderName) {
        isCardFormValid = CardValidator.isValid(
            paymentCard = cardPaymentCard,
            pan = cardPan,
            cvv = cardCvv,
            expiry = cardExpiry.text,
            cardholderName = cardholderName
        )
    }

    // Re-run when the Card option is (re-)selected too, not only on PAN/expiry edits — the
    // user can leave Pay-by-Card with valid data and come back; without this trigger the
    // slice/visa state would stay cleared until the user retyped the card details.
    // Slice / VIS offers are hidden the moment either field becomes invalid (incomplete length,
    // failed Luhn, malformed expiry) and only reappear when BOTH PAN and expiry are valid.
    LaunchedEffect(cardPan, cardExpiry.text, selectedOption) {
        if (selectedOption != PaymentOption.CARD) return@LaunchedEffect
        val maxLength = cardPaymentCard?.binRange?.length?.value ?: 16
        val panValid = cardPan.length == maxLength && Luhn.isValidPan(cardPan)
        val expiryValid = cardExpiry.text.length == 5 &&
                ExpireDateEditText.isValidExpire(cardExpiry.text)
        if (panValid && expiryValid) {
            val key = "$cardPan|${cardExpiry.text}"
            if (key != lastSliceKey) {
                lastSliceKey = key
                cardSelectedSliceOffer = null
                onCheckSliceEligibility(cardPan, cardExpiry.text.filter { it.isDigit() }, cardPaymentCard?.type?.name)
            }
        } else {
            // Hide any visible Slice offers / VIS plans the moment PAN or expiry stops being
            // valid. The brand banner (driven by the order, not the card) stays.
            if (lastSliceKey.isNotEmpty()) lastSliceKey = ""
            cardSelectedSliceOffer = null
            onResetSliceCheck()
        }
    }

    // ── Visibility flags ─────────────────────────────────────────────────────
    val showGooglePay = showWallets && googlePayUiConfig != null
    val showSamsungPay = showWallets && isSamsungPayAvailable
    val showAani = showWallets && aaniConfig != null
    val hasOtherOptions = showGooglePay || showSamsungPay || showAani || clickToPayConfig != null || qpayConfig != null

    val logoResId = if (SDKConfig.merchantLogoResId != 0) SDKConfig.merchantLogoResId
    else R.drawable.network_international_logo

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testId("sdk_paymentpage_container_main")
    ) {
        // ── Scrollable body ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides
                        if (SDKConfig.getLanguage() == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                // Header band — close button + merchant logo on F5F9FC, joins seamlessly into the order summary below
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PgColors.surfaceRow)
                        .statusBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 4.dp)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .testId("sdk_paymentpage_button_cancel")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF333333),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Image(
                            painter = painterResource(id = logoResId),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .height(40.dp)
                                .testId("sdk_paymentpage_image_merchantLogo"),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                PaymentSectionsContent(
                    supportedCards = supportedCards,
                    hasOtherOptions = hasOtherOptions,
                    showGooglePay = showGooglePay,
                    showSamsungPay = showSamsungPay,
                    showAani = showAani,
                    googlePayUiConfig = googlePayUiConfig,
                    formattedAmount = formattedAmount,
                    aaniConfig = aaniConfig,
                    clickToPayConfig = clickToPayConfig,
                    qpayConfig = qpayConfig,
                    savedCards = savedCards.takeLast(3),
                    orderItems = orderItems,
                    sliceCheckState = sliceCheckState,
                    selectedOption = selectedOption,
                    selectedSavedCard = selectedSavedCard,
                    savedCardCvv = savedCardCvv,
                    isOrderSummaryExpanded = isOrderSummaryExpanded,
                    cardPan = cardPan,
                    cardCvv = cardCvv,
                    cardExpiry = cardExpiry,
                    cardholderName = cardholderName,
                    cardPaymentCard = cardPaymentCard,
                    cardSelectedSliceOffer = cardSelectedSliceOffer,
                    visCheckState = visCheckState,
                    visSelectedPlan = cardSelectedVisaPlan,
                    visTermsAccepted = visTermsAccepted,
                    visOrderValue = orderValue,
                    visCurrencyCode = currencyCode,
                    showPaymentOptionError = paymentOptionError,
                    onVisPlanSelected = {
                        cardSelectedVisaPlan = it
                        paymentOptionError = false
                    },
                    onVisTermsToggled = { visTermsAccepted = it },
                    onToggleOrderSummary = { isOrderSummaryExpanded = !isOrderSummaryExpanded },
                    onOptionSelected = { option ->
                        selectedOption = option
                        if (option != PaymentOption.SAVED_CARD) {
                            selectedSavedCard = null
                            savedCardCvv = ""
                        }
                        // Drop any stale installment selectors when switching options.
                        cardSelectedVisaPlan = null
                        visTermsAccepted = false
                        onResetSliceCheck()
                        // Reset the dedupe key so re-selecting Pay-by-Card with valid PAN/expiry
                        // already populated re-fires eligibility (LaunchedEffect only re-runs on
                        // pan/expiry changes; without this the slice options would never come
                        // back after the user navigates away and returns).
                        lastSliceKey = ""
                    },
                    onSavedCardSelected = { card ->
                        // Avoid re-firing eligibility (and clearing the user's CVV input) when
                        // the same saved card is already selected — the row's clickable area
                        // includes the inline CVV field, so taps inside it bubble up here.
                        val wasAlreadySelected =
                            selectedSavedCard?.cardToken == card.cardToken &&
                            selectedOption == PaymentOption.SAVED_CARD
                        selectedSavedCard = card
                        selectedOption = PaymentOption.SAVED_CARD
                        if (!wasAlreadySelected) {
                            savedCardCvv = ""
                            // Drop any stale Slice/Vis state from prior selection.
                            cardSelectedSliceOffer = null
                            cardSelectedVisaPlan = null
                            visTermsAccepted = false
                            // Fire Slice + Vis eligibility once, when the card is first selected.
                            onCheckSavedCardEligibility(card)
                        }
                    },
                    onSavedCardCvvChanged = { savedCardCvv = it },
                    onPanChanged = { pan, card ->
                        cardPan = pan
                        cardPaymentCard = card
                    },
                    onCvvChanged = { cardCvv = it },
                    onExpiryChanged = { cardExpiry = it },
                    onCardholderNameChanged = { cardholderName = it },
                    onSliceOfferSelected = {
                        cardSelectedSliceOffer = it
                        cardSliceSelectionMade = true
                        paymentOptionError = false
                    },
                    onGooglePay = onGooglePay,
                    onSamsungPay = onSamsungPay,
                    onClickAaniPay = onClickAaniPay,
                    onClickToPay = onClickToPay,
                    onClickQPay = onClickQPay
                )
            }
        }

        // ── Pinned bottom bar ────────────────────────────────────────────────
        Divider(color = PgColors.borderRow, thickness = 0.5.dp)
        BottomPayBar(
            selectedOption = selectedOption,
            selectedSavedCard = selectedSavedCard,
            savedCardCvv = savedCardCvv,
            isCardFormValid = isCardFormValid,
            isProcessing = isProcessing,
            googlePayUiConfig = googlePayUiConfig,
            formattedAmount = formattedAmount,
            aaniConfig = aaniConfig,
            clickToPayConfig = clickToPayConfig,
            qpayConfig = qpayConfig,
            cardPan = cardPan,
            cardCvv = cardCvv,
            cardExpiry = cardExpiry,
            cardholderName = cardholderName,
            cardSelectedSliceOffer = cardSelectedSliceOffer,
            cardSelectedVisaPlan = cardSelectedVisaPlan,
            visTermsAccepted = visTermsAccepted,
            sliceCheckState = sliceCheckState,
            visCheckState = visCheckState,
            cardSliceSelectionMade = cardSliceSelectionMade,
            paymentOptionError = paymentOptionError,
            onPaymentOptionErrorChange = { paymentOptionError = it },
            onGooglePay = onGooglePay,
            onSamsungPay = onSamsungPay,
            onClickAaniPay = onClickAaniPay,
            onClickToPay = onClickToPay,
            onClickQPay = onClickQPay,
            onMakePayment = onMakePayment,
            onMakeSavedCardPayment = onMakeSavedCardPayment
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scrollable payment sections
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaymentSectionsContent(
    supportedCards: Set<CardType>,
    hasOtherOptions: Boolean,
    showGooglePay: Boolean,
    showSamsungPay: Boolean,
    showAani: Boolean,
    googlePayUiConfig: GooglePayUiConfig?,
    formattedAmount: String,
    aaniConfig: AaniPayLauncher.Config?,
    clickToPayConfig: ClickToPayLauncher.Config?,
    qpayConfig: QPayLauncher.Config? = null,
    savedCards: List<SavedCard>,
    orderItems: List<OrderItem>,
    sliceCheckState: SliceCheckState,
    selectedOption: PaymentOption?,
    selectedSavedCard: SavedCard?,
    savedCardCvv: String,
    isOrderSummaryExpanded: Boolean,
    cardPan: String,
    cardCvv: String,
    cardExpiry: TextFieldValue,
    cardholderName: String,
    cardPaymentCard: PaymentCard?,
    cardSelectedSliceOffer: SliceOffer?,
    visCheckState: VisCheckState,
    visSelectedPlan: InstallmentPlan?,
    visTermsAccepted: Boolean,
    visOrderValue: Double,
    visCurrencyCode: String,
    showPaymentOptionError: Boolean = false,
    onVisPlanSelected: (InstallmentPlan?) -> Unit,
    onVisTermsToggled: (Boolean) -> Unit,
    onToggleOrderSummary: () -> Unit,
    onOptionSelected: (PaymentOption?) -> Unit,
    onSavedCardSelected: (SavedCard) -> Unit,
    onSavedCardCvvChanged: (String) -> Unit,
    onPanChanged: (pan: String, card: PaymentCard?) -> Unit,
    onCvvChanged: (String) -> Unit,
    onExpiryChanged: (TextFieldValue) -> Unit,
    onCardholderNameChanged: (String) -> Unit,
    onSliceOfferSelected: (SliceOffer?) -> Unit,
    onGooglePay: () -> Unit,
    onSamsungPay: () -> Unit,
    onClickAaniPay: (AaniPayLauncher.Config) -> Unit,
    onClickToPay: (ClickToPayLauncher.Config) -> Unit,
    onClickQPay: (QPayLauncher.Config) -> Unit = {}
) {
    // ── Order summary ────────────────────────────────────────────────────────
    if (SDKConfig.showOrderAmount) {
        OrderSummarySection(
            formattedAmount = formattedAmount,
            orderItems = orderItems,
            isExpanded = isOrderSummaryExpanded,
            onToggle = onToggleOrderSummary
        )
        Spacer(Modifier.height(Spacing.rowGap))
    }

    // ── Google Pay — first after order summary ───────────────────────────────
    if (showGooglePay) {
        Spacer(Modifier.height(Spacing.sectionGap))
        OtherPaymentOptionsSection(
            title = stringResource(R.string.pay_with_google_pay),
            selectedOption = selectedOption,
            googlePayUiConfig = googlePayUiConfig,
            isSamsungPayAvailable = false,
            aaniConfig = null,
            clickToPayConfig = null,
            onGooglePay = onGooglePay,
            onSamsungPay = onSamsungPay,
            onClickAaniPay = onClickAaniPay,
            onClickToPay = onClickToPay,
            onOptionSelected = { onOptionSelected(it) }
        )
    }

    // ── Card payment section (with inline saved cards above "Pay by card") ─────
    Spacer(Modifier.height(Spacing.sectionGap))
    CardPaymentSection(
        supportedCards = supportedCards,
        isExpanded = selectedOption == PaymentOption.CARD,
        savedCards = savedCards,
        selectedSavedCard = selectedSavedCard,
        savedCardCvv = savedCardCvv,
        onSavedCardSelected = onSavedCardSelected,
        onSavedCardCvvChanged = onSavedCardCvvChanged,
        pan = cardPan,
        cvv = cardCvv,
        expiry = cardExpiry,
        cardholderName = cardholderName,
        paymentCard = cardPaymentCard,
        selectedSliceOffer = cardSelectedSliceOffer,
        sliceCheckState = sliceCheckState,
        visCheckState = visCheckState,
        visSelectedPlan = visSelectedPlan,
        visTermsAccepted = visTermsAccepted,
        visOrderValue = visOrderValue,
        visCurrencyCode = visCurrencyCode,
        showPaymentOptionError = showPaymentOptionError,
        onVisPlanSelected = onVisPlanSelected,
        onVisTermsToggled = onVisTermsToggled,
        onToggle = {
            onOptionSelected(if (selectedOption == PaymentOption.CARD) null else PaymentOption.CARD)
        },
        onPanChanged = onPanChanged,
        onCvvChanged = onCvvChanged,
        onExpiryChanged = onExpiryChanged,
        onCardholderNameChanged = onCardholderNameChanged,
        onSliceOfferSelected = onSliceOfferSelected
    )

    // ── Other payment options (Samsung Pay, Aani, Click to Pay) ─────────────
    val hasRemainingOptions = showSamsungPay || showAani || clickToPayConfig != null || qpayConfig != null
    if (hasRemainingOptions) {
        Spacer(Modifier.height(Spacing.sectionGap))
        OtherPaymentOptionsSection(
            selectedOption = selectedOption,
            googlePayUiConfig = null,
            isSamsungPayAvailable = showSamsungPay,
            aaniConfig = if (showAani) aaniConfig else null,
            clickToPayConfig = clickToPayConfig,
            qpayConfig = qpayConfig,
            onGooglePay = onGooglePay,
            onSamsungPay = onSamsungPay,
            onClickAaniPay = onClickAaniPay,
            onClickToPay = onClickToPay,
            onClickQPay = onClickQPay,
            onOptionSelected = { onOptionSelected(it) }
        )
    }

    Spacer(Modifier.height(Spacing.sectionGap))
}

// ─────────────────────────────────────────────────────────────────────────────
// Order summary collapsible
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OrderSummarySection(
    formattedAmount: String,
    orderItems: List<OrderItem>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PgColors.surfaceRow)
            .padding(horizontal = Spacing.pageH)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (orderItems.isNotEmpty()) Modifier.clickable { onToggle() } else Modifier)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.order_summary),
                    style = PgType.bodyRowSubtitle,
                    color = PgColors.textMuted
                )
                if (orderItems.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = PgColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            AedAmountText(
                text = formattedAmount,
                style = PgType.amountSummary,
                color = PgColors.textPrimary,
                modifier = Modifier.testId("sdk_paymentpage_label_amount")
            )
        }

        AnimatedVisibility(
            visible = isExpanded && orderItems.isNotEmpty(),
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.order_items_header),
                        style = PgType.bodyRowSubtitle,
                        color = PgColors.textMuted
                    )
                    Text(
                        text = stringResource(R.string.order_amount_header),
                        style = PgType.bodyRowSubtitle,
                        color = PgColors.textMuted
                    )
                }
                orderItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.name,
                            style = PgType.bodyRowSubtitle,
                            color = PgColors.textSecondary
                        )
                        AedAmountText(
                            text = item.amount,
                            style = PgType.amountRow,
                            color = PgColors.textPrimary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun SavedCardRow(
    card: SavedCard,
    isSelected: Boolean,
    savedCardCvv: String,
    onSelect: () -> Unit,
    onCvvChanged: (String) -> Unit
) {
    val cardType = remember(card.scheme) {
        CardMapping.mapSupportedCards(listOf(card.scheme)).firstOrNull()
    }
    val last4 = remember(card.maskedPan) { card.maskedPan.takeLast(4) }
    val expiry = remember(card.expiry) { formatSavedCardExpiry(card.expiry) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.pageH)
            .clip(RoundedCornerShape(Radius.row))
            .background(if (isSelected) PgColors.surfaceRow else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = Spacing.rowPaddingH, vertical = Spacing.rowPaddingV)
            .testId("sdk_paymentpage_savedcard_${card.maskedPan.takeLast(4)}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PaymentRadioButton(selected = isSelected)
        Spacer(Modifier.width(8.dp))

        // Card logo in a small bordered box
        Box(
            modifier = Modifier
                .border(1.dp, PgColors.borderRow, RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = getCardImage(cardType, isWhiteBackground = true),
                contentDescription = card.scheme,
                modifier = Modifier.size(width = 32.dp, height = 20.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.width(10.dp))

        // Two-line info column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ending in $last4",
                style = PgType.bodyRowTitle,
                color = PgColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.cardholderName,
                    style = PgType.bodyRowSubtitle,
                    color = PgColors.textMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = expiry,
                    style = PgType.bodyRowSubtitle,
                    color = PgColors.textMuted
                )
            }
        }

        // Inline CVV field — only when this card is selected and requires CVV recapture
        if (isSelected && card.recaptureCsc) {
            Spacer(Modifier.width(8.dp))
            InlineCvvField(
                value = savedCardCvv,
                onValueChange = { if (it.length <= 4) onCvvChanged(it) },
                modifier = Modifier
                    .width(84.dp)
                    .testId("sdk_paymentpage_field_savedCardCvv")
            )
        }
    }
}

private fun formatSavedCardExpiry(expiry: String): String {
    // Convert YYYY-MM → MM/YY
    val parts = expiry.split("-")
    return if (parts.size == 2) "${parts[1]}/${parts[0].takeLast(2)}" else expiry
}

@Composable
private fun InlineCvvField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .border(1.dp, PgColors.borderInput, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp)),
        textStyle = PgType.bodyRowTitle.copy(color = PgColors.textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("•••", style = PgType.bodyRowTitle, color = PgColors.textMuted)
                    }
                    innerTextField()
                }
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom pay bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun BottomPayBar(
    selectedOption: PaymentOption?,
    selectedSavedCard: SavedCard?,
    savedCardCvv: String,
    isCardFormValid: Boolean,
    isProcessing: Boolean,
    googlePayUiConfig: GooglePayUiConfig?,
    formattedAmount: String,
    aaniConfig: AaniPayLauncher.Config?,
    clickToPayConfig: ClickToPayLauncher.Config?,
    qpayConfig: QPayLauncher.Config? = null,
    cardPan: String,
    cardCvv: String,
    cardExpiry: TextFieldValue,
    cardholderName: String,
    cardSelectedSliceOffer: SliceOffer?,
    cardSelectedVisaPlan: InstallmentPlan?,
    visTermsAccepted: Boolean,
    sliceCheckState: SliceCheckState = SliceCheckState.Idle,
    visCheckState: VisCheckState = VisCheckState.Idle,
    cardSliceSelectionMade: Boolean = false,
    paymentOptionError: Boolean = false,
    onPaymentOptionErrorChange: (Boolean) -> Unit = {},
    onGooglePay: () -> Unit,
    onSamsungPay: () -> Unit,
    onClickAaniPay: (AaniPayLauncher.Config) -> Unit,
    onClickToPay: (ClickToPayLauncher.Config) -> Unit,
    onClickQPay: (QPayLauncher.Config) -> Unit = {},
    onMakePayment: (cardNumber: String, expiry: String, cvv: String, cardholderName: String, sliceOffer: SliceOffer?, visaPlan: InstallmentPlan?) -> Unit,
    onMakeSavedCardPayment: (savedCard: SavedCard, cvv: String?) -> Unit
) {
    val isSavedCardReady = selectedSavedCard != null &&
            (!selectedSavedCard.recaptureCsc || savedCardCvv.isNotBlank())

    // A non-PayInFull Vis plan requires explicit T&C acceptance.
    val visPlanGate = cardSelectedVisaPlan == null ||
            cardSelectedVisaPlan.frequency == payment.sdk.android.visaInstalments.model.PlanFrequency.PayInFull ||
            visTermsAccepted

    val sliceRequiresPick = sliceCheckState is SliceCheckState.Available &&
            sliceCheckState.offers.isNotEmpty() &&
            !cardSliceSelectionMade
    val visRequiresPick = visCheckState is VisCheckState.Available &&
            visCheckState.plans.matchedPlans.isNotEmpty() &&
            cardSelectedVisaPlan == null

    val isStandardButtonEnabled = when (selectedOption) {
        PaymentOption.CARD -> isCardFormValid && !isProcessing && visPlanGate
        PaymentOption.SAVED_CARD -> isSavedCardReady && !isProcessing
        PaymentOption.AANI,
        PaymentOption.CLICK_TO_PAY,
        PaymentOption.QPAY -> !isProcessing
        PaymentOption.GOOGLE_PAY,
        PaymentOption.SAMSUNG_PAY,
        null -> false
    }

    Spacer(Modifier.height(Spacing.rowGap))

    when (selectedOption) {
        // ── Native Google Pay button ─────────────────────────────────────────
        PaymentOption.GOOGLE_PAY -> {
            if (googlePayUiConfig != null) {
                GooglePayButton(
                    onClick = onGooglePay,
                    allowedPaymentMethods = googlePayUiConfig.allowedPaymentMethods,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PgSize.buttonHeight)
                        .padding(horizontal = Spacing.appBarPadding)
                        .testId("sdk_paymentpage_button_pay")
                )
            }
        }

        // ── Samsung Pay button (black, branded) ──────────────────────────────
        PaymentOption.SAMSUNG_PAY -> {
            Button(
                onClick = onSamsungPay,
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PgSize.buttonHeight)
                    .padding(horizontal = Spacing.appBarPadding)
                    .testId("sdk_paymentpage_button_pay"),
                shape = RoundedCornerShape(Radius.button),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Black,
                    disabledBackgroundColor = sdkColor(R.color.payment_sdk_button_disabled_background_color)
                )
            ) {
                Image(
                    painter = painterResource(R.drawable.samsung_pay_logo),
                    contentDescription = "Samsung Pay",
                    modifier = Modifier.height(22.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // ── Standard pay button for all other options ────────────────────────
        else -> {
            val payLabel = if (SDKConfig.showOrderAmount)
                stringResource(R.string.pay_button_title, formattedAmount)
            else
                stringResource(R.string.pay_button)

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PgSize.buttonHeight)
                    .padding(horizontal = Spacing.appBarPadding)
                    .testId("sdk_paymentpage_button_pay")
                    .background(
                        color = if (isStandardButtonEnabled)
                            sdkColor(R.color.payment_sdk_pay_button_background_color)
                        else
                            sdkColor(R.color.payment_sdk_button_disabled_background_color),
                        shape = RoundedCornerShape(Radius.button)
                    ),
                onClick = {
                    val cardNeedsPick = selectedOption == PaymentOption.CARD && (sliceRequiresPick || visRequiresPick)
                    val savedCardNeedsPick = selectedOption == PaymentOption.SAVED_CARD && (sliceRequiresPick || visRequiresPick)
                    if (cardNeedsPick || savedCardNeedsPick) {
                        onPaymentOptionErrorChange(true)
                        return@TextButton
                    }
                    when (selectedOption) {
                        PaymentOption.CARD -> onMakePayment(
                            cardPan,
                            cardExpiry.text.filter { it.isDigit() },
                            cardCvv,
                            cardholderName,
                            cardSelectedSliceOffer,
                            cardSelectedVisaPlan
                        )
                        PaymentOption.SAVED_CARD -> selectedSavedCard?.let {
                            onMakeSavedCardPayment(it, savedCardCvv.takeIf { v -> v.isNotBlank() })
                        }
                        PaymentOption.AANI -> aaniConfig?.let { onClickAaniPay(it) }
                        PaymentOption.CLICK_TO_PAY -> clickToPayConfig?.let { onClickToPay(it) }
                        PaymentOption.QPAY -> qpayConfig?.let { onClickQPay(it) }
                        else -> {}
                    }
                },
                enabled = isStandardButtonEnabled,
                shape = RoundedCornerShape(Radius.button)
            ) {
                AedAmountText(
                    text = payLabel,
                    style = PgType.buttonPrimary,
                    color = if (isStandardButtonEnabled)
                        sdkColor(R.color.payment_sdk_pay_button_text_color)
                    else
                        sdkColor(R.color.payment_sdk_button_disabled_text_color)
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    TermsAgreementText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.pageH)
    )
    Spacer(
        Modifier
            .navigationBarsPadding()
            .height(Spacing.rowGap)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Terms text
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TermsAgreementText(modifier: Modifier = Modifier) {
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
        style = PgType.captionDisclaimer.copy(
            textAlign = TextAlign.Start,
            color = PgColors.textMuted
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                }
        }
    )
}
