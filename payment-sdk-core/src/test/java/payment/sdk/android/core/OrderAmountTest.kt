package payment.sdk.android.core

import org.hamcrest.core.StringContains
import org.junit.Assert.assertThat
import org.junit.Test

class OrderAmountTest {
    @Test
    fun getFormattedCurrency() {
        val orderAmount = OrderAmount(2000.00, "AED")
        val formattedCurrencyLTR = orderAmount.formattedCurrencyString(true)
        assertThat(formattedCurrencyLTR, StringContains("20.0 AED"))

        val formattedCurrencyRTL = orderAmount.formattedCurrencyString(false)
        assertThat(formattedCurrencyRTL, StringContains("AED 20.0"))
    }
}