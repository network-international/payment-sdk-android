package payment.sdk.android.payments.view

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import payment.sdk.android.core.SliceOffer
import payment.sdk.android.payments.SliceCheckState
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
                    text = "Checking Slice eligibility...",
                    style = PgType.bodyRowSubtitle,
                    color = PgColors.textEligibility
                )
            }
        }
        else -> {}
    }
}

// Figma: Slice installment picker — tab row + detail card
@Composable
fun SliceInstallmentSection(
    offers: List<SliceOffer>,
    onOfferSelected: (SliceOffer?) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.rowGap)
    ) {
        // Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PgColors.borderSection, RoundedCornerShape(Radius.row))
                .clip(RoundedCornerShape(Radius.row))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "slice »",
                    style = PgType.bodyRowTitle,
                    color = PgColors.accentPrimary
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Zero fees. Zero interest.",
                        style = PgType.bodyRowTitle,
                        color = PgColors.textPrimary
                    )
                    Text(
                        text = "Split your purchases into easy installments",
                        style = PgType.bodyRowSubtitle,
                        color = PgColors.textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.rowGap))

        // Pill tab row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SliceTab(
                label = "Pay in Full",
                selected = selectedIndex == 0,
                onClick = {
                    selectedIndex = 0
                    onOfferSelected(null)
                }
            )
            offers.forEachIndexed { idx, offer ->
                SliceTab(
                    label = "${offer.period} months",
                    selected = selectedIndex == idx + 1,
                    onClick = {
                        selectedIndex = idx + 1
                        onOfferSelected(offer)
                    }
                )
            }
        }

        Spacer(Modifier.height(Spacing.rowGap))

        if (selectedIndex > 0) {
            SliceDetailCard(offer = offers[selectedIndex - 1])
        }
    }
}

@Composable
private fun SliceTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (selected) PgColors.accentPrimary else PgColors.surfaceRow)
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
            color = if (selected) PgColors.textOnTabSelected else PgColors.textPrimary
        )
    }
}

@Composable
private fun SliceDetailCard(offer: SliceOffer) {
    val installmentDisplay = formatAmount(offer.installmentAmount.value, offer.installmentAmount.currencyCode)
    val totalDisplay = formatAmount(offer.totalAmount.value, offer.totalAmount.currencyCode)
    val isZeroInterest = offer.rate == "0" || offer.rate == "0.0" || offer.rate == "0.00"
    val isZeroFee = offer.fee == "0" || offer.fee == "0.0" || offer.fee == "0.00"

    val feeDisplay = if (offer.feeType == "P") {
        "${offer.fee}%"
    } else {
        formatAmount((offer.fee.toDoubleOrNull()?.times(100))?.toInt() ?: 0, offer.installmentAmount.currencyCode)
    }

    Column(
        modifier = Modifier
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
            Text(
                text = "$installmentDisplay / month",
                style = PgType.amountRow,
                color = PgColors.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isZeroInterest) {
                    SliceBadge(label = "Zero interest")
                }
                if (isZeroFee) {
                    SliceBadge(label = "Zero fees")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Divider(color = PgColors.dividerSlice)
        Spacer(Modifier.height(8.dp))

        SliceDetailRow(label = "Interest rate", value = "${offer.rate}%")
        Spacer(Modifier.height(4.dp))
        SliceDetailRow(label = "Processing fees", value = feeDisplay)
        Spacer(Modifier.height(4.dp))
        SliceDetailRow(
            label = "Total after ${offer.period} months",
            value = totalDisplay,
            bold = true
        )
    }
}

@Composable
private fun SliceBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.badge))
            .background(PgColors.badgeDarkBg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = PgType.captionSlicePeriod,
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
        Text(
            text = value,
            style = PgType.bodyRowSubtitle,
            color = PgColors.textPrimary,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun formatAmount(minorUnits: Int, currencyCode: String): String {
    val amount = minorUnits / 100.0
    return "$currencyCode ${String.format("%.2f", amount)}"
}
