package payment.sdk.android.core

import org.hamcrest.core.StringContains
import org.junit.Assert.assertThat
import org.junit.Test

class OrderAmountTest {
    @Test
    fun getFormattedCurrency() {
        val orderAmount = OrderAmount(2000.00, "AED")
        // The code leads with the currency and pads to the currency's minor units in both
        // directions, so both forms read "AED 20.00".
        val formattedCurrencyLTR = orderAmount.formattedCurrencyString(true)
        assertThat(formattedCurrencyLTR, StringContains("AED 20.00"))

        val formattedCurrencyRTL = orderAmount.formattedCurrencyString(false)
        assertThat(formattedCurrencyRTL, StringContains("AED 20.00"))
    }
}