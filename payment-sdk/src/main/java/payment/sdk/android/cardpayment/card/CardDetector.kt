package payment.sdk.android.cardpayment.card

import payment.sdk.android.core.CardType

class CardDetector(private val supportedCards: Set<CardType>) {

    private val acceptedCardModels: List<CardModel> by lazy {
        CARD_MODELS.filter { supportedCards.contains(it.type) }
    }

    fun detect(bin: String): PaymentCard? {
        val firstSix = if (bin.length > LONGEST_AVAILABLE_BIN_DIGITS) {
            bin.substring(0 until LONGEST_AVAILABLE_BIN_DIGITS)
        } else {
            bin
        }
        return findMatchingCard(firstSix, acceptedCardModels)
    }

    companion object {

        private const val LONGEST_AVAILABLE_BIN_DIGITS = 6

        private fun findMatchingCard(pan: String, acceptedCards: List<CardModel>): PaymentCard? {
            val binValue = pan.toBigInteger()
            acceptedCards.forEach { card ->
                val matchedBinRange: BinRange? = card.binRanges.firstOrNull { range ->
                    (range.start..range.end).contains(binValue)
                }

                if (matchedBinRange != null) {
                    return@findMatchingCard PaymentCard(
                            card.type,
                            pan,
                            matchedBinRange,
                            card.cvv,
                            if (pan.length >= 6) MatchCertainty.MATCH else MatchCertainty.PROBABLE
                    )
                }
            }

            val shorterBin = pan.dropLast(1)

            if (shorterBin.isEmpty()) {
                return null
            }
            return findMatchingCard(shorterBin, acceptedCards)
        }


        private val CARD_MODELS = listOf(
                CardModel(
                        CardType.Visa,
                        Cvv(3, CardFace.BACK),
                        listOf(
                                BinRange(start = 4.toBigInteger(), end = 4.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4))

                        )),
                CardModel(
                        CardType.MasterCard,
                        Cvv(3, CardFace.BACK),
                        listOf(
                                BinRange(start = 51.toBigInteger(), end = 55.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 2221.toBigInteger(), end = 2720.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4))
                        )

                ),
                CardModel(
                        CardType.AmericanExpress,
                        Cvv(4, CardFace.FRONT),
                        listOf(
                                BinRange(start = 37.toBigInteger(), end = 37.toBigInteger(), length = BinLength(value = 15, pattern = SpacingPatterns.Pattern_4_6_5)),
                                BinRange(start = 34.toBigInteger(), end = 34.toBigInteger(), length = BinLength(value = 15, pattern = SpacingPatterns.Pattern_4_6_5))
                        )

                ),
                CardModel(
                        CardType.JCB,
                        Cvv(3, CardFace.BACK),
                        listOf(
                                BinRange(start = 3528.toBigInteger(), end = 3589.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4))
                        )

                ),
                CardModel(
                        CardType.DinersClubInternational,
                        Cvv(3, CardFace.BACK),
                        listOf(
                                BinRange(start = 36.toBigInteger(), end = 36.toBigInteger(), length = BinLength(value = 14, pattern = SpacingPatterns.Pattern_4_6_4))
                        )

                ),
                CardModel(
                        CardType.Discover,
                        Cvv(3, CardFace.BACK),
                        listOf(
                                BinRange(start = 6011.toBigInteger(), end = 6011.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 622126.toBigInteger(), end = 622925.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 644.toBigInteger(), end = 649.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 65.toBigInteger(), end = 65.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4))
                        )

                )
        )
    }


}
