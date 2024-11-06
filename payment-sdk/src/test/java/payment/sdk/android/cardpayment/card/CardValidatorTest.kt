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
                    "12/25",
                    "123",
                    "John Doe",
                    true
                ),
                arrayOf(null, "4539578763621486", "12/25", "123", "John Doe", false),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "invalidPan",
                    "12/25",
                    "123",
                    "John Doe",
                    false
                ),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "13/25",
                    "123",
                    "John Doe",
                    true
                ),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "12/25",
                    "123",
                    "",
                    false
                ),
                arrayOf(
                    CardDetector(cards).detect("4539578763621486"),
                    "4539578763621486",
                    "12/25",
                    "123",
                    "   ",
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