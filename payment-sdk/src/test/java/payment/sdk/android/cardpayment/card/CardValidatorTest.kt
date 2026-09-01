package payment.sdk.android.cardpayment.card

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import payment.sdk.android.core.CardType

@RunWith(Parameterized::class)
class CardValidatorTest(
    private val paymentCard: PaymentCard?,
    private val pan: String,
    private val expiry: String,
    private val cvv: String,
    private val cardholderName: String,
    private val expected: Boolean
) {
    companion object {
        private val cards = CardType.entries.toSet()

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "12/30",
                    "123",
                    "John Doe",
                    true
                ),
                arrayOf(null, "4539578763621486", "12/30", "123", "John Doe", false),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "invalidPan",
                    "12/30",
                    "123",
                    "John Doe",
                    false
                ),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "13/30",
                    "123",
                    "John Doe",
                    true
                ),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "12/30",
                    "123",
                    "",
                    false
                ),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "12/30",
                    "123",
                    "   ",
                    false
                ),

                // Every case above uses a 3-digit CVV on a Visa, so the scheme's CVV length was
                // never actually exercised — deleting that check from CardValidator kept the suite
                // green. These pin it.
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "12/30",
                    "12",           // one short for a Visa
                    "John Doe",
                    false
                ),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "12/30",
                    "1234",         // one long for a Visa
                    "John Doe",
                    false
                ),
                arrayOf(
                    CardDetector(cards).detect("378282246310005"),
                    "378282246310005",
                    "12/30",
                    "1234",         // Amex genuinely wants four
                    "John Doe",
                    true
                ),
                arrayOf(
                    CardDetector(cards).detect("378282246310005"),
                    "378282246310005",
                    "12/30",
                    "123",          // three is short for Amex
                    "John Doe",
                    false
                ),

                // Likewise no case had an expiry whose invalidity changed the answer, so the
                // expiry check could be deleted unnoticed.
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "12/20",        // in the past
                    "123",
                    "John Doe",
                    false
                )
            )
        }
    }

    @Test
    fun `test CardValidator isValid`() {
        assertEquals(expected, CardValidator.isValid(paymentCard, pan, expiry, cvv, cardholderName))
    }
}