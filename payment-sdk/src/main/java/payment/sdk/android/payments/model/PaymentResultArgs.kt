package payment.sdk.android.payments.model

import payment.sdk.android.core.CardType
import payment.sdk.android.core.SliceOffer
import payment.sdk.android.payments.view.formatSliceAmount

data class PaymentResultArgs(
    val isSuccess: Boolean,
    val formattedAmount: String?,
    val transactionId: String,
    val dateTime: String,
    val supportedCards: Set<CardType>,
    val orderItems: List<OrderItem> = emptyList(),
    val sliceReceipt: SliceReceipt? = null,
)

/**
 * Display-ready slice details surfaced on the success screen when the user paid with a Slice
 * installment plan. Amounts are pre-formatted in the payment flow so the result view doesn't
 * have to know about minor units / currency codes; the labels and the month count are left to
 * the view, which localises them.
 *
 * The labels and formatting deliberately mirror the offer card on the payment page — the
 * receipt restates the plan the payer just agreed to, so any wording that differs between the
 * two reads as a different plan. The card builds its figures through [from] for that reason.
 */
data class SliceReceipt(
    /** The offer's `period` as the gateway sent it, e.g. "4"; the view pluralises it. */
    val period: String,
    val rate: String,               // e.g. "0%"
    val processingFees: String,     // e.g. "AED 0.00", or "2%" for a percentage fee type
    /**
     * The issuer's per-installment fee, already added to the amount charged. `null` when the
     * offer carried no `commission`, matching the card, which hides the row entirely.
     */
    val installmentFees: String? = null,
    val installmentAmount: String,  // e.g. "AED 2,719.50"
    /**
     * `true` when the offer was Islamic (eligibility indicator `"I"`). Drives the
     * "Profit rate" vs "Interest rate" label, as on the offer card.
     */
    val isIslamic: Boolean = false,
) {
    companion object {
        fun from(offer: SliceOffer, isIslamic: Boolean): SliceReceipt {
            val currency = offer.installmentAmount.currencyCode
            val processingFees = if (offer.feeType == "P") {
                "${offer.fee}%"
            } else {
                formatSliceAmount(majorToMinor(offer.fee.toDoubleOrNull() ?: 0.0), currency)
            }
            return SliceReceipt(
                period = offer.period,
                rate = "${offer.rate}%",
                processingFees = processingFees,
                installmentFees = offer.installmentFeeAmount?.let {
                    formatSliceAmount(majorToMinor(it), currency)
                },
                installmentAmount = formatSliceAmount(offer.installmentAmount.value, currency),
                isIslamic = isIslamic,
            )
        }

        /**
         * Rounded, not truncated: `0.29 * 100` is `28.999…` in floating point, so truncating
         * would quote the payer one fil less than the plan they agreed to.
         */
        private fun majorToMinor(major: Double): Int = Math.round(major * 100).toInt()
    }
}
