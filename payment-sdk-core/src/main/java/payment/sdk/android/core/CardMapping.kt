package payment.sdk.android.core

/**
 * Maps cards string representation to actual enum classes
 */
class CardMapping {

    companion object {
        fun mapSupportedCards(cards: List<String>): Set<CardType> =
                mutableSetOf<CardType>().apply {
                    cards.forEach { card ->
                        SUPPORTED_CARD_MAPPING[card]?.let { cardType ->
                            add(cardType)
                        }
                    }
                }

        private val SUPPORTED_CARD_MAPPING = mapOf(
                "VISA" to CardType.Visa,
                "MASTERCARD" to CardType.MasterCard,
                "AMERICAN_EXPRESS" to CardType.AmericanExpress,
                "DISCOVER" to CardType.Discover,
                "JCB" to CardType.JCB,
                "DINERS_CLUB_INTERNATIONAL" to CardType.DinersClubInternational
        )
    }
}