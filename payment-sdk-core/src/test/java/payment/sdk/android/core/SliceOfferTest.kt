package payment.sdk.android.core

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SliceOfferTest {

    private fun offer(commission: String?, fee: String = "0"): SliceOffer {
        val commissionField = commission?.let { """"commission": "$it",""" }.orEmpty()
        return Gson().fromJson(
            """
            {
              "period": "3",
              "rate": "0.00",
              "fee": "$fee",
              "feeType": "FIXED",
              $commissionField
              "installmentAmount": { "currencyCode": "AED", "value": 20733 },
              "totalAmount": { "currencyCode": "AED", "value": 62200 }
            }
            """.trimIndent(),
            SliceOffer::class.java
        )
    }

    /**
     * The value behind the "Installment fee" row: the planned commission the backend quotes for
     * the selected plan, in major units.
     */
    @Test
    fun `a positive commission is the installment fee`() {
        assertEquals(7.00, offer("7.00").installmentFeeAmount!!, 0.001)
    }

    /**
     * The row is hidden on a zero fee, so the parsed value must be null rather than 0 — the view
     * decides purely on null-ness.
     */
    @Test
    fun `a zero commission carries no installment fee`() {
        assertNull(offer("0").installmentFeeAmount)
        assertNull(offer("0.00").installmentFeeAmount)
    }

    @Test
    fun `an absent commission carries no installment fee`() {
        assertNull(offer(null).installmentFeeAmount)
    }

    /**
     * A plan the backend quotes without a usable number must hide the row rather than render
     * something meaningless next to an amount the payer is about to be charged.
     */
    @Test
    fun `an unparsable commission carries no installment fee`() {
        assertNull(offer("").installmentFeeAmount)
        assertNull(offer("n/a").installmentFeeAmount)
        assertNull(offer("-7.00").installmentFeeAmount)
    }

    /**
     * The breakdown's "Total after N months" reads the backend total directly, which the design has
     * covering the order amount plus the installment fee (615.00 + 7.00).
     */
    @Test
    fun `the offer carries the backend total`() {
        assertEquals(62200, offer("7.00").totalAmount.value)
    }

    /** The "Zero fees" tag needs both charges to be zero — processing fee and installment fee. */
    @Test
    fun `zero fee and no commission is fee free`() {
        assertTrue(offer(null).isFeeFree)
        assertTrue(offer("0").isFeeFree)
        assertTrue(offer("0.00", fee = "0.00").isFeeFree)
    }

    @Test
    fun `an installment fee alone rules out the zero fees tag`() {
        assertFalse(offer("18.00").isFeeFree)
    }

    @Test
    fun `a processing fee alone rules out the zero fees tag`() {
        assertFalse(offer(null, fee = "8.00").isFeeFree)
        assertFalse(offer("0", fee = "8").isFeeFree)
    }

    @Test
    fun `an unparsable processing fee is not shown as zero`() {
        assertFalse(offer(null, fee = "").isFeeFree)
    }
}
