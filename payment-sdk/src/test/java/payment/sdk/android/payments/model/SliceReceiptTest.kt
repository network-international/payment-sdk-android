package payment.sdk.android.payments.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.core.SliceAmount
import payment.sdk.android.core.SliceOffer

/**
 * The receipt restates the plan the payer agreed to on the offer card, and the card builds its
 * figures through [SliceReceipt.from] — so these pin what both screens show.
 */
class SliceReceiptTest {

    private fun offer(
        period: String = "4",
        rate: String = "0.00",
        fee: String = "0",
        feeType: String = "F",
        commission: String? = null,
        installment: Int = 271950,
        total: Int = 1087800,
    ) = SliceOffer(
        period = period,
        rate = rate,
        fee = fee,
        feeType = feeType,
        commission = commission,
        installmentAmount = SliceAmount("AED", installment),
        totalAmount = SliceAmount("AED", total),
    )

    @Test
    fun `carries the period as sent so the view can pluralise it`() {
        assertEquals("4", SliceReceipt.from(offer(period = "4"), isIslamic = false).period)
    }

    @Test
    fun `formats the installment amount with grouping and two decimals`() {
        assertEquals("AED 2,719.50", SliceReceipt.from(offer(), isIslamic = false).installmentAmount)
    }

    @Test
    fun `shows the rate as a percentage`() {
        assertEquals("1.5%", SliceReceipt.from(offer(rate = "1.5"), isIslamic = false).rate)
    }

    @Test
    fun `a percentage fee type is shown as a percentage, not an amount`() {
        assertEquals("2%", SliceReceipt.from(offer(fee = "2", feeType = "P"), isIslamic = false).processingFees)
    }

    @Test
    fun `a fixed fee is shown as an amount in the offer currency`() {
        assertEquals("AED 25.00", SliceReceipt.from(offer(fee = "25"), isIslamic = false).processingFees)
    }

    /** `0.29 * 100` is `28.999…`; truncating it quoted the payer AED 0.28. */
    @Test
    fun `a fixed fee is rounded to the nearest minor unit, not truncated`() {
        assertEquals("AED 0.29", SliceReceipt.from(offer(fee = "0.29"), isIslamic = false).processingFees)
    }

    @Test
    fun `an unparsable fixed fee is shown as zero`() {
        assertEquals("AED 0.00", SliceReceipt.from(offer(fee = ""), isIslamic = false).processingFees)
    }

    @Test
    fun `a commission becomes the installment fee row`() {
        assertEquals("AED 7.00", SliceReceipt.from(offer(commission = "7.00"), isIslamic = false).installmentFees)
    }

    @Test
    fun `the installment fee is rounded, not truncated`() {
        assertEquals("AED 0.29", SliceReceipt.from(offer(commission = "0.29"), isIslamic = false).installmentFees)
    }

    /** The view hides the row purely on null-ness, as the offer card does. */
    @Test
    fun `no commission means no installment fee row`() {
        assertNull(SliceReceipt.from(offer(commission = null), isIslamic = false).installmentFees)
        assertNull(SliceReceipt.from(offer(commission = "0.00"), isIslamic = false).installmentFees)
    }

    @Test
    fun `carries the Islamic flag that picks the profit-rate label`() {
        assertTrue(SliceReceipt.from(offer(), isIslamic = true).isIslamic)
        assertFalse(SliceReceipt.from(offer(), isIslamic = false).isIslamic)
    }
}
