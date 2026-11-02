package payment.sdk.android.core

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** KNET is offered exactly when the gateway gives the payment a `payment:knet` link. */
class KnetOrderTest {

    private fun order(knetHref: String?, cards: String = """["KNET", "VISA", "MASTERCARD"]"""): Order {
        val knetLink = knetHref?.let { """"payment:knet": { "href": "$it" },""" }.orEmpty()
        return Gson().fromJson(
            """
            {
              "action": "PURCHASE",
              "amount": { "currencyCode": "KWD", "value": 1000 },
              "paymentMethods": { "card": $cards },
              "_embedded": {
                "payment": [
                  {
                    "_links": { $knetLink "self": { "href": "$PAYMENT_SELF" } },
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
    fun `KNET is offered when the payment carries a payment knet link`() {
        val order = order("$PAYMENT_SELF/knet")
        assertTrue(order.isKnetSupported())
        assertEquals("$PAYMENT_SELF/knet", order.getKnetUrl())
    }

    /** Before the backend added the link KNET appeared only as a card scheme — nothing to start from. */
    @Test
    fun `KNET is hidden when only the card scheme is listed`() {
        val order = order(knetHref = null)
        assertFalse(order.isKnetSupported())
        assertNull(order.getKnetUrl())
    }

    @Test
    fun `a blank link counts as absent`() {
        assertFalse(order("").isKnetSupported())
    }

    /** Verbatim from UAT while the outlet's KNET encryption was broken (2026-11-02, HTTP 502). */
    @Test
    fun `reads the gateway's reason out of a refused initiation`() {
        val raw = "HTTP: 502 - {\"message\":\"Bad Gateway\",\"code\":502,\"errors\":[{\"message\":" +
            "\"Failed to encrypt KNET trandata\",\"localizedMessage\":\"{error.processing.invalidKnetPayment}\"," +
            "\"errorCode\":\"invalidKnetPayment\",\"domain\":\"processing\"}]}"
        assertEquals("Failed to encrypt KNET trandata (invalidKnetPayment)", KnetInitErrorBody.reasonFrom(raw))
    }

    @Test
    fun `reads the hosted page from a successful initiation`() {
        val body = Gson().fromJson(
            """{"paymentId":"100630610000011036","paymentUrl":"https://kpaytest.com.kw/kpg/paymentpage.htm?PaymentID=100630610000011036","status":"Initiated"}""",
            KnetInitResponse::class.java
        )
        assertTrue(body.isInitiated)
        assertEquals("kpaytest.com.kw", java.net.URI(body.paymentUrl).host)
    }

    private companion object {
        const val PAYMENT_SELF =
            "https://api-gateway.sandbox.ngenius-payments.com/transactions/outlets/o1/orders/abc/payments/pay1"
    }
}
