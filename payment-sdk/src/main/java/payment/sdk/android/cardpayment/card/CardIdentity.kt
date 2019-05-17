package payment.sdk.android.cardpayment.card

import payment.sdk.android.core.CardType

data class PaymentCard(
        val type: CardType,
        val pan: String,
        val binRange: BinRange,
        val cvv: Cvv,
        val certainty: MatchCertainty
)

enum class MatchCertainty {
    PROBABLE, MATCH
}