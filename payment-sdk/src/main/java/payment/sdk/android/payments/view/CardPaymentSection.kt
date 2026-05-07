package payment.sdk.android.payments.view

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import payment.sdk.android.core.SliceOffer
import payment.sdk.android.payments.SliceCheckState
import payment.sdk.android.payments.VisCheckState
import payment.sdk.android.visaInstalments.model.InstallmentPlan
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import payment.sdk.android.cardpayment.card.CardDetector
import payment.sdk.android.cardpayment.card.PaymentCard
import payment.sdk.android.core.CardType
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.payments.theme.PgSize
import payment.sdk.android.payments.theme.PgType
import payment.sdk.android.payments.theme.Radius
import payment.sdk.android.payments.theme.Spacing
import payment.sdk.android.savedCard.view.getCardImage
import payment.sdk.android.sdk.R
import payment.sdk.android.core.testId
import payment.sdk.android.core.SavedCard

// Figma: Card payment section — bordered section card with expand/collapse form
@Composable
fun CardPaymentSection(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    supportedCards: Set<CardType>,
    savedCards: List<SavedCard> = emptyList(),
    selectedSavedCard: SavedCard? = null,
    savedCardCvv: String = "",
    onSavedCardSelected: (SavedCard) -> Unit = {},
    onSavedCardCvvChanged: (String) -> Unit = {},
    pan: String,
    cvv: String,
    expiry: TextFieldValue,
    cardholderName: String,
    paymentCard: PaymentCard?,
    selectedSliceOffer: SliceOffer?,
    sliceCheckState: SliceCheckState = SliceCheckState.Idle,
    visCheckState: VisCheckState = VisCheckState.Idle,
    visSelectedPlan: InstallmentPlan? = null,
    visTermsAccepted: Boolean = false,
    visOrderValue: Double = 0.0,
    visCurrencyCode: String = "",
    onVisPlanSelected: (InstallmentPlan?) -> Unit = {},
    onVisTermsToggled: (Boolean) -> Unit = {},
    onToggle: () -> Unit,
    onPanChanged: (pan: String, card: PaymentCard?) -> Unit,
    onCvvChanged: (String) -> Unit,
    onExpiryChanged: (TextFieldValue) -> Unit,
    onCardholderNameChanged: (String) -> Unit,
    onSliceOfferSelected: (SliceOffer?) -> Unit,
    onCheckSliceEligibility: (pan: String, expiryRaw: String) -> Unit = { _, _ -> },
    onResetSliceCheck: () -> Unit = {}
) {
    val cardDetector = remember(supportedCards) { CardDetector(supportedCards) }

    val expiryFocus = remember { FocusRequester() }
    val cvvFocus = remember { FocusRequester() }
    val cardHolderFocus = remember { FocusRequester() }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section heading
        Text(
            text = stringResource(R.string.use_credit_or_debit_card),
            style = PgType.headingSection,
            color = PgColors.textPrimary,
            modifier = Modifier.padding(
                start = Spacing.pageH, end = Spacing.pageH,
                top = Spacing.rowPaddingV, bottom = Spacing.rowPaddingV
            )
        )

        // Accepted card brand strip
        Row(
            modifier = Modifier.padding(
                start = Spacing.pageH, end = Spacing.pageH,
                bottom = Spacing.rowGap
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            supportedCards.forEach { card ->
                Image(
                    modifier = Modifier
                        .height(18.dp)
                        .widthIn(max = 32.dp),
                    painter = getCardImage(card, isWhiteBackground = true),
                    contentDescription = card.name,
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Saved cards — between brand icons and "Pay by card" toggle (no horizontal padding so
        // the selection background goes edge-to-edge within the card section)
        if (savedCards.isNotEmpty()) {
            savedCards.forEach { card ->
                SavedCardRow(
                    card = card,
                    isSelected = selectedSavedCard?.cardToken == card.cardToken,
                    savedCardCvv = if (selectedSavedCard?.cardToken == card.cardToken) savedCardCvv else "",
                    onSelect = { onSavedCardSelected(card) },
                    onCvvChanged = onSavedCardCvvChanged
                )
            }
        }

        // Radio toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = Spacing.pageH, vertical = Spacing.rowPaddingV),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(Spacing.rowPaddingH))
            PaymentRadioButton(selected = isExpanded)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.pay_by_card),
                style = PgType.bodyRowTitle,
                color = PgColors.textPrimary
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
        ) {
            Column(modifier = Modifier
                .clip(RectangleShape)
                .padding(
                    start = Spacing.pageH + PgSize.radioOuter + 12.dp,
                    end = Spacing.pageH
                )) {
                CardNumberTextField(
                    pan = pan,
                    paymentCard = paymentCard,
                    sliceCheckState = sliceCheckState,
                    onValueChanged = { text ->
                        val maxLength = paymentCard?.binRange?.length?.value ?: 16
                        if (text.length <= maxLength) {
                            val newPan = text.filter { it.isDigit() }
                            val newCard = if (newPan.isNotEmpty()) cardDetector.detect(newPan) else null
                            onPanChanged(newPan, newCard)
                            if (newPan.length == maxLength) {
                                expiryFocus.requestFocus()
                            }
                        }
                    }
                )

                Spacer(Modifier.height(Spacing.fieldsStackGap))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.fieldRowGap)
                ) {
                    ExpiryDateTextField(
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(expiryFocus),
                        text = expiry,
                        onValueChange = { onExpiryChanged(it) },
                        focusCvv = { cvvFocus.requestFocus() }
                    )

                    PgTextField(
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(cvvFocus),
                        value = cvv,
                        onValueChange = { text ->
                            val maxLength = paymentCard?.cvv?.length ?: 3
                            if (text.length <= maxLength) {
                                onCvvChanged(text)
                                if (text.length == maxLength) {
                                    cardHolderFocus.requestFocus()
                                }
                            }
                        },
                        label = stringResource(R.string.security_code_label),
                        placeholder = "CVV",
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        testTag = "sdk_card_field_cvv"
                    )
                }

                var showCvvTooltip by remember { mutableStateOf(false) }

                Text(
                    text = stringResource(R.string.whats_cvv),
                    style = PgType.bodyRowSubtitle,
                    color = PgColors.textMuted,
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
                        Text(
                            text = stringResource(R.string.cvv_tooltip),
                            style = PgType.bodyRowSubtitle,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFF333333),
                                    shape = RoundedCornerShape(Radius.row)
                                )
                                .padding(horizontal = Spacing.rowPaddingH, vertical = Spacing.rowPaddingV)
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.rowGap))

                PgTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(cardHolderFocus),
                    value = cardholderName,
                    onValueChange = onCardholderNameChanged,
                    label = stringResource(R.string.name_on_card_label),
                    placeholder = stringResource(R.string.name_on_card_placeholder),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = PgColors.textMuted
                        )
                    },
                    testTag = "sdk_card_field_cardholderName"
                )

                if (sliceCheckState is SliceCheckState.Available) {
                    SliceInstallmentSection(
                        offers = sliceCheckState.offers,
                        onOfferSelected = { onSliceOfferSelected(it) }
                    )
                }

                Spacer(Modifier.height(Spacing.sectionGap))
            }
        }

        // Visa installment section is rendered at section level (outside the manual-form
        // AnimatedVisibility) so it appears for both manual entry AND saved-card selection.
        if (visCheckState is VisCheckState.Available) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.pageH)
            ) {
                VisaInstallmentSection(
                    plans = visCheckState.plans,
                    orderAmount = payment.sdk.android.core.OrderAmount(visOrderValue, visCurrencyCode),
                    selectedPlan = visSelectedPlan,
                    termsAccepted = visTermsAccepted,
                    onPlanSelected = onVisPlanSelected,
                    onTermsToggled = onVisTermsToggled
                )
                Spacer(Modifier.height(Spacing.sectionGap))
            }
        }
    }
}
