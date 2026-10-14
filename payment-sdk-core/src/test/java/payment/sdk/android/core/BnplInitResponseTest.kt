package payment.sdk.android.core

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BnplInitResponseTest {

    private fun parse(json: String): BnplInitResponse =
        Gson().fromJson(json, BnplInitResponse::class.java)

    /** Verbatim from the gateway on the DEV outlet with Tamara live. */
    @Test
    fun `decodes the live tamara response`() {
        val response = parse(
            """
            {
              "webUrl": "https://checkout-sandbox.tamara.co/checkout/ee26f7e6?orderId=761d053b",
              "tamaraOrderId": "761d053b-8bf4-4767-8bfb-0bcd2af004d8"
            }
            """.trimIndent()
        )
        assertEquals(
            "https://checkout-sandbox.tamara.co/checkout/ee26f7e6?orderId=761d053b",
            response.hostedCheckoutUrl
        )
        assertEquals("761d053b-8bf4-4767-8bfb-0bcd2af004d8", response.providerReference)
    }

    @Test
    fun `reads tabbys payment id`() {
        val response = parse("""{ "webUrl": "https://checkout.tabby.ai/abc", "tabbyPaymentId": "tabby-1" }""")
        assertEquals("tabby-1", response.providerReference)
    }

    /**
     * The sibling APMs each spell the URL field differently, so every spelling is accepted rather
     * than leaving the payer on a blank screen if this one follows Benefit or QPay instead.
     */
    @Test
    fun `accepts the other spellings the apm responses use`() {
        listOf("redirectUrl", "paymentUrl", "checkoutUrl").forEach { field ->
            val response = parse("""{ "$field": "https://checkout.tamara.co/abc" }""")
            assertEquals("$field must be read", "https://checkout.tamara.co/abc", response.hostedCheckoutUrl)
        }
    }

    @Test
    fun `empty urls are ignored in favour of a populated one`() {
        val response = parse("""{ "webUrl": "", "paymentUrl": "https://checkout.tamara.co/abc" }""")
        assertEquals("https://checkout.tamara.co/abc", response.hostedCheckoutUrl)
    }

    @Test
    fun `cancelled and error message are read`() {
        val response = parse("""{ "cancelled": true, "errorMessage": "order amount too low" }""")
        assertEquals(true, response.cancelled)
        assertEquals("order amount too low", response.errorMessage)
        assertNull(response.hostedCheckoutUrl)
    }

    /**
     * An order id and a payment id are different things, so the provider-specific name wins over the
     * generic one rather than whichever the parser happened to read first.
     */
    @Test
    fun `provider specific reference wins over the generic one`() {
        val response = parse("""{ "tamaraOrderId": "tam-1", "orderId": "generic-1" }""")
        assertEquals("tam-1", response.providerReference)
    }
}
