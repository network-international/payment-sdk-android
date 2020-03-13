package payment.sdk.android.core

import org.hamcrest.core.StringContains
import org.junit.Assert.assertThat
import org.junit.Test

class OrderAmountTest {
    @Test
    fun getFormattedCurrency() {
        val orderAmount = OrderAmount(2000.00, "AED")
        val formattedCurrency = orderAmount.formattedCurrencyString()
        assertThat(formattedCurrency, StringContains("20.0 AED"))
    }
}