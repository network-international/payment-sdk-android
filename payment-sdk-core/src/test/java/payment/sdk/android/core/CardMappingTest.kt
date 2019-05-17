package payment.sdk.android.core

import org.hamcrest.Matchers
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.collection.IsIterableContainingInOrder.contains
import org.junit.Assert.*
import org.junit.Test

class CardMappingTest {

    @Test
    fun mapVisa() {
        val mapping = CardMapping.mapSupportedCards(listOf("VISA"))
        assertThat(mapping, hasSize(1))
        assertThat(mapping, contains(CardType.Visa))
    }

    @Test
    fun mapMasterCard() {
        val mapping = CardMapping.mapSupportedCards(listOf("MASTERCARD"))
        assertThat(mapping, hasSize(1))
        assertThat(mapping, contains(CardType.MasterCard))
    }

    @Test
    fun mapMasterAmex() {
        val mapping = CardMapping.mapSupportedCards(listOf("AMERICAN_EXPRESS"))
        assertThat(mapping, hasSize(1))
        assertThat(mapping, contains(CardType.AmericanExpress))
    }

    @Test
    fun mapMasterJCB() {
        val mapping = CardMapping.mapSupportedCards(listOf("JCB"))
        assertThat(mapping, hasSize(1))
        assertThat(mapping, contains(CardType.JCB))
    }


    @Test
    fun mapMasterDinersClub() {
        val mapping = CardMapping.mapSupportedCards(listOf("DINERS_CLUB_INTERNATIONAL"))
        assertThat(mapping, hasSize(1))
        assertThat(mapping, contains(CardType.DinersClubInternational))
    }

    @Test
    fun mapVisaMasterCardAmex() {
        val mapping = CardMapping.mapSupportedCards(listOf("VISA", "MASTERCARD", "AMERICAN_EXPRESS"))
        assertThat(mapping, hasSize(3))
        assertThat(mapping, contains(CardType.Visa, CardType.MasterCard, CardType.AmericanExpress))
    }


    @Test
    fun mapVisaMasterJCBDinersClub() {
        val mapping = CardMapping.mapSupportedCards(listOf("VISA", "MASTERCARD", "JCB", "DINERS_CLUB_INTERNATIONAL"))
        assertThat(mapping, hasSize(4))
        assertThat(mapping, contains(CardType.Visa, CardType.MasterCard, CardType.JCB, CardType.DinersClubInternational))
    }

    @Test
    fun mapNonExistingCards() {
        val mapping = CardMapping.mapSupportedCards(listOf("UNKNOWN_CARD_1", "UNKNOWN_CARD_2"))
        assertThat(mapping, Matchers.empty())
    }

    @Test
    fun mapEmptyStrings() {
        val mapping = CardMapping.mapSupportedCards(listOf("", "", "", ""))
        assertThat(mapping, Matchers.empty())
    }
}