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

        var findMatchingCard = findMatchingCard(firstSix, acceptedCardModels)
        if (findMatchingCard?.type === CardType.Mada && !supportedCards.contains(CardType.Mada)) {
                findMatchingCard = PaymentCard(CardType.Visa, firstSix, findMatchingCard.binRange,
                        findMatchingCard.cvv, findMatchingCard.certainty)
        }
        return findMatchingCard
    }

    companion object {

        private const val LONGEST_AVAILABLE_BIN_DIGITS = 8

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
                            if (pan.length >= 8) MatchCertainty.MATCH else MatchCertainty.PROBABLE
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
                        CardType.Mada,
                        Cvv(3, CardFace.BACK),
                        listOf(
                                BinRange(start = 403024.toBigInteger(), end = 403024.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 406136.toBigInteger(), end = 406136.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 406996.toBigInteger(), end = 406996.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 407520.toBigInteger(), end = 407520.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 409201.toBigInteger(), end = 409201.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 410621.toBigInteger(), end = 410834.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 412565.toBigInteger(), end = 412565.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 417633.toBigInteger(), end = 417633.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 419593.toBigInteger(), end = 419593.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 420132.toBigInteger(), end = 420132.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 421141.toBigInteger(), end = 421141.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 422817.toBigInteger(), end = 422817.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 422818.toBigInteger(), end = 422818.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 422819.toBigInteger(), end = 422819.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 428331.toBigInteger(), end = 428331.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 428671.toBigInteger(), end = 428671.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 428672.toBigInteger(), end = 428672.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 428673.toBigInteger(), end = 428673.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 431361.toBigInteger(), end = 431361.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 432328.toBigInteger(), end = 432328.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 434107.toBigInteger(), end = 434107.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 439954.toBigInteger(), end = 439954.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 440533.toBigInteger(), end = 440533.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 440647.toBigInteger(), end = 440647.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 440795.toBigInteger(), end = 440795.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 442429.toBigInteger(), end = 442429.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 442463.toBigInteger(), end = 442463.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 445564.toBigInteger(), end = 445564.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 446393.toBigInteger(), end = 446393.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 446404.toBigInteger(), end = 446404.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 446672.toBigInteger(), end = 446672.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 455036.toBigInteger(), end = 455036.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 455708.toBigInteger(), end = 455708.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 457865.toBigInteger(), end = 457865.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 457997.toBigInteger(), end = 457997.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 458456.toBigInteger(), end = 458456.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 462220.toBigInteger(), end = 462220.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 468540.toBigInteger(), end = 468540.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 468541.toBigInteger(), end = 468541.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 468542.toBigInteger(), end = 468542.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 468543.toBigInteger(), end = 468543.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 474491.toBigInteger(), end = 474491.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 483010.toBigInteger(), end = 483012.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 484783.toBigInteger(), end = 484783.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 486094.toBigInteger(), end = 486094.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 486095.toBigInteger(), end = 486095.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 486096.toBigInteger(), end = 486096.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 489318.toBigInteger(), end = 489318.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 489319.toBigInteger(), end = 489319.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 492464.toBigInteger(), end = 492464.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 530060.toBigInteger(), end = 530060.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 531196.toBigInteger(), end = 531196.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 535825.toBigInteger(), end = 535825.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 535989.toBigInteger(), end = 535989.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 536023.toBigInteger(), end = 536023.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 537767.toBigInteger(), end = 537767.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 543085.toBigInteger(), end = 543085.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 543357.toBigInteger(), end = 543357.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 549760.toBigInteger(), end = 549760.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 554180.toBigInteger(), end = 554180.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 555610.toBigInteger(), end = 555610.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 558563.toBigInteger(), end = 558563.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 588845.toBigInteger(), end = 588850.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 604906.toBigInteger(), end = 604906.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 636120.toBigInteger(), end = 636120.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 968201.toBigInteger(), end = 968212.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 22337902.toBigInteger(), end = 22337986.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 22402030.toBigInteger(), end = 22402030.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 40177800.toBigInteger(), end = 40177800.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 40545400.toBigInteger(), end = 40545400.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 40719700.toBigInteger(), end = 40739500.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 42222200.toBigInteger(), end = 42222200.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 45488707.toBigInteger(), end = 45501701.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 45488713.toBigInteger(), end = 45488713.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 45501701.toBigInteger(), end = 45501701.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 49098000.toBigInteger(), end = 49098001.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 52166100.toBigInteger(), end = 52166100.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4)),
                                BinRange(start = 53973776.toBigInteger(), end = 53973776.toBigInteger(), length = BinLength(value = 16, pattern = SpacingPatterns.Pattern_4_4_4_4))
                        )),
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
