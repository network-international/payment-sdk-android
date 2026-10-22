package payment.sdk.android.webview

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoggableUrlTest {

    @Test
    fun `the paypage auth code never reaches the log`() {
        val redacted = redactUrlForLog(
            "https://paypage.sandbox.ngenius-payments.com/v2?code=abc123secret&ni_sdk_result=success"
        )
        assertEquals(
            "https://paypage.sandbox.ngenius-payments.com/v2?ni_sdk_result=success (+1 redacted)",
            redacted
        )
    }

    @Test
    fun `our own markers survive so a return leg is still readable`() {
        val redacted = redactUrlForLog(
            "https://paypage.example.com/v2?code=live&payment_method=tamara&ni_sdk_result=failure"
        )
        assertEquals(
            "https://paypage.example.com/v2?payment_method=tamara&ni_sdk_result=failure (+1 redacted)",
            redacted
        )
    }

    @Test
    fun `a provider reference the gateway appends is redacted, not allowlisted`() {
        val redacted = redactUrlForLog(
            "https://paypage.example.com/v2?tamaraOrderId=761d053b&token=xyz&code=c"
        )
        assertEquals("https://paypage.example.com/v2 (+3 redacted)", redacted)
    }

    @Test
    fun `the provider's own checkout page logs intact when it carries no query`() {
        assertEquals(
            "https://checkout-sandbox.tamara.co/checkout/761d053b",
            redactUrlForLog("https://checkout-sandbox.tamara.co/checkout/761d053b")
        )
    }

    @Test
    fun `port is kept so a local gateway is still identifiable`() {
        assertEquals(
            "http://localhost:8080/v2 (+1 redacted)",
            redactUrlForLog("http://localhost:8080/v2?code=abc")
        )
    }

    @Test
    fun `null and blank urls say so rather than printing nothing`() {
        assertEquals("<none>", redactUrlForLog(null))
        assertEquals("<none>", redactUrlForLog(""))
    }

    @Test
    fun `an opaque uri keeps its scheme and discards the rest`() {
        // The scheme-specific part can be an address or a token; there is no host to anchor on.
        assertEquals("mailto:<opaque>", redactUrlForLog("mailto:someone@example.com"))
        assertEquals("tamara:<opaque>", redactUrlForLog("tamara:checkout?token=secret"))
    }
}
