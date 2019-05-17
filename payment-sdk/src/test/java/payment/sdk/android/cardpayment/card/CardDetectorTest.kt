package payment.sdk.android.cardpayment.card

import payment.sdk.android.core.CardType
import payment.sdk.android.core.CardType.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardDetectorTest {

    private val detector = CardDetector(
            setOf(Visa, AmericanExpress, MasterCard, JCB, DinersClubInternational, Discover)
    )

    @Test
    fun `detect short pans`() {
        assertEquals(detector.detect("4")?.type, Visa)
        assertEquals(detector.detect("41")?.type, Visa)
        assertEquals(detector.detect("51")?.type, MasterCard)
        assertEquals(detector.detect("52")?.type, MasterCard)
        assertEquals(detector.detect("53")?.type, MasterCard)
        assertEquals(detector.detect("54")?.type, MasterCard)
        assertEquals(detector.detect("55")?.type, MasterCard)
        assertEquals(detector.detect("2720")?.type, MasterCard)
        assertEquals(detector.detect("2719")?.type, MasterCard)
        assertEquals(detector.detect("2521")?.type, MasterCard)
        assertEquals(detector.detect("2221")?.type, MasterCard)
        assertEquals(detector.detect("34")?.type, AmericanExpress)
        assertEquals(detector.detect("37")?.type, AmericanExpress)
    }

    @Test
    fun `detect long pans`() {
        assertEquals(detector.detect("4111111111111111")?.type, Visa)
        assertEquals(detector.detect("5105105105105100")?.type, MasterCard)
        assertEquals(detector.detect("5555555555554444")?.type, MasterCard)
        assertEquals(detector.detect("371449635398431")?.type, AmericanExpress)
        assertEquals(detector.detect("378282246310005")?.type, AmericanExpress)
        assertEquals(detector.detect("3566002020360505")?.type, CardType.JCB)
        assertEquals(detector.detect("3530111333300000")?.type, CardType.JCB)
        assertEquals(detector.detect("36700102000000")?.type, CardType.DinersClubInternational)
        assertEquals(detector.detect("36148900647913")?.type, CardType.DinersClubInternational)
        assertEquals(detector.detect("6011111111111117")?.type, CardType.Discover)
        assertEquals(detector.detect("6011000990139424")?.type, CardType.Discover)
    }

    @Test
    fun `detect invalid pans`() {
        assertNull(detector.detect("1111111111111111"))
        assertNull(detector.detect("0000000000000000"))
        assertNull(detector.detect("35"))
        assertNull(detector.detect("39"))
        assertNull(detector.detect("1"))
    }

}