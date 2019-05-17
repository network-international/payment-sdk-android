package payment.sdk.android.cardpayment.card

import payment.sdk.android.core.CardType
import java.math.BigInteger

data class CardModel(
        val type: CardType,
        val cvv: Cvv,
        val binRanges: List<BinRange>
)

data class BinRange(
        val start: BigInteger,
        val end: BigInteger,
        val length: BinLength
)

data class BinLength(
        val value: Int,
        val pattern: String
)

data class Cvv(
        val length: Int,
        val face: CardFace
)

enum class CardFace {
    FRONT, BACK
}