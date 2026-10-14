package payment.sdk.android.payments.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.sdk.R
import payment.sdk.android.core.SliceOffer
import payment.sdk.android.payments.SliceCheckState
import payment.sdk.android.payments.model.SliceReceipt
import payment.sdk.android.payments.theme.PgColors
import payment.sdk.android.payments.theme.PgSize
import payment.sdk.android.payments.theme.PgType
import payment.sdk.android.payments.theme.Radius
import payment.sdk.android.payments.theme.Spacing

// Figma: Slice eligibility indicator (shown below card number field)
@Composable
fun SliceEligibilityRow(state: SliceCheckState) {
    when (state) {
        SliceCheckState.Checking -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = PgColors.spinnerPrimary,
                    backgroundColor = PgColors.spinnerTrack
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.slice_checking_eligibility),
                    style = PgType.bodyRowSubtitle,
                    color = PgColors.textEligibility
                )
            }
        }
        else -> {}
    }
}

// Figma: Slice installment picker — tab row + detail card.
// `pillBleedStart` / `pillBleedEnd` let the horizontally-scrolling pill row escape ancestor
// padding (so pills reach the screen edges) while banner & detail stay aligned with siblings.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SliceInstallmentSection(
    offers: List<SliceOffer>,
    onOfferSelected: (SliceOffer?) -> Unit,
    pillBleedStart: Dp = 0.dp,
    pillBleedEnd: Dp = 0.dp,
    showSelectionError: Boolean = false,
    isIslamic: Boolean = false,
) {
    // Pay in Full (index 0) is the default selection — the user can switch to an offer
    // via the tab row, but never has to start from a no-selection state.
    var selectedIndex by remember { mutableIntStateOf(0) }
    val detailRequester = remember { BringIntoViewRequester() }

    // Mirror the iOS behavior: after the detail card expands (offer selected, not Pay-in-Full),
    // scroll the parent so the card's bottom is visible instead of being clipped under the
    // pay button. The small delay lets the card finish layout before we request scroll.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex > 0) {
            delay(50)
            detailRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.rowGap)
    ) {
        SliceBanner()

        Spacer(Modifier.height(Spacing.rowGap))

        // Pill tab row — horizontally scrollable, bleeds past ancestor padding to screen edges.
        Row(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val startPx = pillBleedStart.roundToPx()
                    val endPx = pillBleedEnd.roundToPx()
                    val expandedWidth = constraints.maxWidth + startPx + endPx
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = expandedWidth, maxWidth = expandedWidth)
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.place(-startPx, 0)
                    }
                }
                .horizontalScroll(rememberScrollState())
                .padding(start = pillBleedStart, end = pillBleedEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SliceTab(
                label = stringResource(R.string.slice_pay_in_full),
                selected = selectedIndex == 0,
                onClick = {
                    selectedIndex = 0
                    onOfferSelected(null)
                }
            )
            offers.forEachIndexed { idx, offer ->
                SliceTab(
                    label = sliceMonths(R.plurals.slice_tab_months, offer.period),
                    selected = selectedIndex == idx + 1,
                    onClick = {
                        selectedIndex = idx + 1
                        onOfferSelected(offer)
                    }
                )
            }
        }

        if (showSelectionError) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.select_payment_option_validation),
                color = Color(0xFFD32F2F),
                style = PgType.bodyRowSubtitle,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        Spacer(Modifier.height(Spacing.rowGap))

        if (selectedIndex > 0) {
            SliceDetailCard(
                offer = offers[selectedIndex - 1],
                isIslamic = isIslamic,
                modifier = Modifier.bringIntoViewRequester(detailRequester),
            )
        }
    }
}

@Composable
private fun SliceTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (selected) PgColors.accentPrimary else PgColors.surfacePage)
            .border(
                width = 1.dp,
                color = if (selected) PgColors.accentPrimary else PgColors.borderTabUnselected,
                shape = RoundedCornerShape(Radius.pill)
            )
            .clickable { onClick() }
            .padding(horizontal = Spacing.tabPaddingH, vertical = Spacing.tabPaddingV),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (selected) PgType.pillTabSelected else PgType.pillTabUnselected,
            color = if (selected) PgColors.textOnTabSelected else PgColors.textSecondary
        )
    }
}

@Composable
private fun SliceDetailCard(offer: SliceOffer, isIslamic: Boolean = false, modifier: Modifier = Modifier) {
    // Built through the receipt so the success screen restates exactly what is shown here.
    val plan = SliceReceipt.from(offer, isIslamic)
    val installmentDisplay = plan.installmentAmount
    val totalDisplay = formatSliceAmount(offer.totalAmount.value, offer.totalAmount.currencyCode)
    val isZeroInterest = offer.rate == "0" || offer.rate == "0.0" || offer.rate == "0.00"
    val isZeroFee = offer.isFeeFree
    val feeDisplay = plan.processingFees
    val installmentFeeDisplay = plan.installmentFees

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.row))
            .background(PgColors.surfaceSliceDetail)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                AedAmountText(
                    text = installmentDisplay,
                    style = PgType.amountRow,
                    color = PgColors.textPrimary
                )
                Text(
                    text = stringResource(R.string.slice_per_month),
                    style = PgType.bodyRowSubtitle,
                    color = PgColors.textSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isZeroInterest) {
                    SliceBadge(
                        label = stringResource(
                            if (isIslamic) R.string.slice_badge_zero_profit else R.string.slice_badge_zero_interest
                        )
                    )
                }
                if (isZeroFee) {
                    SliceBadge(label = stringResource(R.string.slice_badge_zero_fees))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Divider(color = PgColors.dividerSlice)
        Spacer(Modifier.height(8.dp))

        SliceDetailRow(label = "${stringResource(sliceRateLabel(isIslamic))}:", value = plan.rate, bold = true)
        Spacer(Modifier.height(8.dp))
        SliceDetailRow(label = "${stringResource(R.string.slice_processing_fees)}:", value = feeDisplay, bold = true)
        if (installmentFeeDisplay != null) {
            Spacer(Modifier.height(8.dp))
            SliceDetailRow(label = "${stringResource(R.string.slice_installment_fees)}:", value = installmentFeeDisplay, bold = true)
        }
        Spacer(Modifier.height(8.dp))
        SliceDetailRow(
            label = sliceMonths(R.plurals.slice_total_after_months, offer.period),
            value = totalDisplay,
            bold = true
        )
    }
}

@Composable
private fun SliceBadge(label: String) {
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 20.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(PgColors.badgeDarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = PgType.captionSlicePeriod.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = PgColors.badgeDarkText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SliceDetailRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = PgType.bodyRowSubtitle,
            color = PgColors.textSecondary
        )
        AedAmountText(
            text = value,
            style = PgType.bodyRowSubtitle,
            color = PgColors.textPrimary,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** Interest-based offers and Islamic (Murabaha) offers name the same figure differently. */
internal fun sliceRateLabel(isIslamic: Boolean): Int =
    if (isIslamic) R.string.slice_profit_rate else R.string.slice_interest_rate

/**
 * A month count from an offer's `period`, pluralised for the SDK language — Arabic alone has six
 * forms. The gateway sends `period` as a string; one that is not a number is shown as sent rather
 * than guessed at.
 */
@Composable
internal fun sliceMonths(pluralsRes: Int, period: String): String {
    val months = period.trim().toIntOrNull() ?: return period
    // The form is picked by the resource locale; the digits stay Western, like every amount.
    val format = LocalContext.current.resources.getQuantityString(pluralsRes, months)
    return String.format(java.util.Locale.US, format, months)
}

/**
 * Shared by the offer card and the success receipt, so the amounts the payer agrees to and the
 * amounts they are shown afterwards are formatted identically.
 */
internal fun formatSliceAmount(minorUnits: Int, currencyCode: String): String {
    val amount = minorUnits / 100.0
    return "$currencyCode ${String.format(java.util.Locale.US, "%,.2f", amount)}"
}

// Figma: brand banner — used both above the offer pills (eligible state) and standalone
// (Slice link present, current card not eligible).
@Composable
internal fun SliceBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.row))
            .background(PgColors.surfaceRow)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // The asset is a square with the wordmark across its middle; cropping to the
        // wordmark's own proportions trims the padding without touching the shared asset.
        Image(
            painter = painterResource(R.drawable.ic_logo_slice),
            contentDescription = "slice by network",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(64.dp)
                .height(28.dp)
        )
        Text(
            text = stringResource(R.string.slice_banner_title),
            style = PgType.bodyRowTitle,
            color = PgColors.textPrimary,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}
