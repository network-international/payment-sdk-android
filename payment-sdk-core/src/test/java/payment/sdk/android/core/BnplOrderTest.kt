package payment.sdk.android.core

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BnplOrderTest {

    /**
     * Mirrors what the gateway actually returns for a BNPL-enabled outlet: the providers under
     * `paymentMethods.apm`, each with its own `payment:{provider}` rel.
     */
    private fun order(
        apm: String = """["TAMARA", "TABBY"]""",
        rels: List<String> = listOf("tamara", "tabby"),
        paymentSelfHref: String? = PAYMENT_SELF
    ): Order {
        val selfLink = paymentSelfHref?.let { """"self": { "href": "$it" },""" }.orEmpty()
        val relLinks = rels.joinToString("") { """"payment:$it": { "href": "$PAYMENT_SELF/$it" }, """ }
        return Gson().fromJson(
            """
            {
              "action": "PURCHASE",
              "amount": { "currencyCode": "AED", "value": 50000 },
              "paymentMethods": { "card": ["VISA"], "apm": $apm },
              "_embedded": {
                "payment": [
                  {
                    "_links": { $selfLink $relLinks "payment:card": { "href": "$PAYMENT_SELF/card" } },
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
    fun `each provider is offered when the order lists it`() {
        assertEquals(listOf(BnplProvider.TAMARA), order(apm = """["TAMARA"]""").supportedBnplProviders())
        assertEquals(listOf(BnplProvider.TABBY), order(apm = """["TABBY"]""").supportedBnplProviders())
    }

    /** The live DEV order carries all three APMs together; both BNPL providers must survive that. */
    @Test
    fun `both providers are offered alongside other apms`() {
        assertEquals(
            listOf(BnplProvider.TAMARA, BnplProvider.TABBY),
            order(apm = """["TABBY", "AANI", "TAMARA"]""").supportedBnplProviders()
        )
    }

    @Test
    fun `apm comparison is case insensitive`() {
        assertEquals(
            listOf(BnplProvider.TAMARA, BnplProvider.TABBY),
            order(apm = """["tamara", "tabby"]""").supportedBnplProviders()
        )
    }

    @Test
    fun `nothing is offered when the order lists no bnpl apms`() {
        assertTrue(order(apm = """["AANI"]""").supportedBnplProviders().isEmpty())
    }

    /** An unknown APM must not stop the known ones being read. */
    @Test
    fun `unknown apms are ignored rather than failing the order`() {
        assertEquals(
            listOf(BnplProvider.TAMARA),
            order(apm = """["SOMETHING_NEW", "TAMARA"]""").supportedBnplProviders()
        )
    }

    @Test
    fun `checkout url uses the advertised rel`() {
        assertEquals("$PAYMENT_SELF/tamara", order().getBnplUrl(BnplProvider.TAMARA))
        assertEquals("$PAYMENT_SELF/tabby", order().getBnplUrl(BnplProvider.TABBY))
    }

    /**
     * An outlet that lists the APM without the rel still gets the option, with the endpoint derived
     * from the payment's own self link.
     */
    @Test
    fun `checkout url is derived when the rel is absent`() {
        val order = order(rels = emptyList())
        assertEquals("$PAYMENT_SELF/tabby", order.getBnplUrl(BnplProvider.TABBY))
        assertEquals(listOf(BnplProvider.TAMARA, BnplProvider.TABBY), order.supportedBnplProviders())
    }

    @Test
    fun `checkout url tolerates a trailing slash on the self link`() {
        assertEquals(
            "$PAYMENT_SELF/tamara",
            order(rels = emptyList(), paymentSelfHref = "$PAYMENT_SELF/").getBnplUrl(BnplProvider.TAMARA)
        )
    }

    @Test
    fun `checkout url is null when the order has no payment self link`() {
        assertNull(order(rels = emptyList(), paymentSelfHref = null).getBnplUrl(BnplProvider.TAMARA))
    }

    /**
     * The gateway named these itself and they follow no shared convention, so a swap between them
     * would fail every accept call.
     */
    @Test
    fun `each provider keeps its own accept field name`() {
        assertEquals("tamaraOrderId", BnplProvider.TAMARA.acceptIdField)
        assertEquals("tabbyPaymentId", BnplProvider.TABBY.acceptIdField)
    }

    @Test
    fun `checkout type is the only value the apm endpoint accepts`() {
        assertEquals("INSTALLMENTS", BnplProvider.CHECKOUT_TYPE)
    }

    companion object {
        private const val PAYMENT_SELF =
            "https://api-gateway.sandbox.ngenius-payments.com/transactions/outlets/o1/orders/o2/payments/p1"
    }
}
