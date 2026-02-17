package payment.sdk.android.payments.model

import payment.sdk.android.core.CardType

data class PaymentResultArgs(
    val isSuccess: Boolean,
    val formattedAmount: String?,
    val transactionId: String,
    val dateTime: String,
    val supportedCards: Set<CardType>
)
