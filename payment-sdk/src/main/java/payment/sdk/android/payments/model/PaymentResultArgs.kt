package payment.sdk.android.payments.model

import payment.sdk.android.core.CardType

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
 * installment plan. All fields are pre-formatted in the payment flow so the result view
 * doesn't have to know about minor units / currency codes.
 */
data class SliceReceipt(
    val tenor: String,              // e.g. "4 Months"
    val interestRate: String,       // e.g. "0%"
    val fees: String,               // e.g. "AED 0.00"
    val installmentAmount: String,  // e.g. "AED 2,719.50"
    /**
     * `true` when the offer was Islamic (eligibility indicator `"I"`). Drives the
     * "Murabaha" vs "Interest rate" label on the result screen.
     */
    val isIslamic: Boolean = false,
)
