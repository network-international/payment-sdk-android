package payment.sdk.android.core

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BenefitOrderTest {

    private fun order(
        action: String = "PURCHASE",
        currency: String = "BHD",
        cards: String = """["VISA", "MASTERCARD", "BENEFIT"]""",
        paymentSelfHref: String? = PAYMENT_SELF
    ): Order {
        val selfLink = paymentSelfHref?.let { """"self": { "href": "$it" },""" }.orEmpty()
        return Gson().fromJson(
            """
            {
              "action": "$action",
              "amount": { "currencyCode": "$currency", "value": 1000 },
              "paymentMethods": { "card": $cards },
              "_embedded": {
                "payment": [
                  {
                    "_links": { $selfLink "payment:card": { "href": "$PAYMENT_SELF/card" } },
                    "reference": "0d66edc2-4b58-49cf-9d98-bf1260be416d",
                    "state": "STARTED"
                  }
                ]
              }
            }
            """.trimIndent(),
            Order::class.java
        )
    }

    @Test
    fun `benefit url is derived from the payment self link`() {
        assertEquals("$PAYMENT_SELF/benefit", order().getBenefitUrl())
    }

    @Test
    fun `benefit url tolerates a trailing slash on the self link`() {
        assertEquals("$PAYMENT_SELF/benefit", order(paymentSelfHref = "$PAYMENT_SELF/").getBenefitUrl())
    }

    @Test
    fun `benefit url is null when the order has no payment self link`() {
        assertNull(order(paymentSelfHref = null).getBenefitUrl())
    }

    @Test
    fun `benefit is supported for a BHD purchase listing BENEFIT`() {
        assertTrue(order().isBenefitSupported())
    }

    @Test
    fun `benefit is not supported when the currency is not BHD`() {
        assertFalse(order(currency = "AED").isBenefitSupported())
    }

    @Test
    fun `benefit is not supported when the order is not a purchase`() {
        assertFalse(order(action = "AUTH").isBenefitSupported())
    }

    @Test
    fun `benefit is not supported when the outlet does not list BENEFIT`() {
        assertFalse(order(cards = """["VISA", "MASTERCARD"]""").isBenefitSupported())
    }

    companion object {
        private const val PAYMENT_SELF =
            "https://api-gateway.sandbox.ngenius-payments.com/transactions/outlets/" +
                    "736afec5-5b5d-467f-88d0-2ce128653143/orders/926143c1-e7b9-419e-9aa8-c03e76fd23c5" +
                    "/payments/0d66edc2-4b58-49cf-9d98-bf1260be416d"
    }
}
