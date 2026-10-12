package payment.sdk.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BnplReturnTest {

    private val returnUrls = listOf(
        "https://paypage-dev.platform.network.ae/v2?code=ZZZ&payment_method=tabby&ni_sdk_result=success",
        "https://paypage-dev.platform.network.ae/v2?code=ZZZ&ni_sdk_result=cancel",
        "https://paypage-dev.platform.network.ae/v2?code=ZZZ&payment_method=tabby&ni_sdk_result=failure"
    )

    /** The bug this exists to prevent: the gateway leaks our marker into the checkout's own URL. */
    @Test
    fun `the provider's checkout url is not a return leg even carrying our marker`() {
        val checkout = "https://paypage-dev.ngenius-payments.com/fake/tabby/checkout/payment" +
                "?orderRef=83f34d63&paymentRef=76642a77" +
                "&merchantUrl=https://paypage-dev.platform.network.ae/v2?code=ZZZ" +
                "&payment_method=tabby&ni_sdk_result=success"
        assertNull(BnplReturn.markerOf(checkout, returnUrls))
        assertFalse(BnplReturn.isReturnTarget(checkout, returnUrls))
    }

    @Test
    fun `a real return leg is recognised`() {
        val url = "https://paypage-dev.platform.network.ae/v2?code=ZZZ&ni_sdk_result=success"
        assertEquals("success", BnplReturn.markerOf(url, returnUrls))
    }

    @Test
    fun `the provider's own appended parameters do not break the match`() {
        val url = "https://paypage-dev.platform.network.ae/v2" +
                "?code=ZZZ&payment_method=tabby&ni_sdk_result=cancel&tabbyPaymentId=abc&foo=bar"
        assertEquals("cancel", BnplReturn.markerOf(url, returnUrls))
    }

    @Test
    fun `our address without the marker is not a leg`() {
        // The stub's GET form rebuilds the query from its own inputs and drops our marker.
        val url = "https://paypage-dev.platform.network.ae/v2?code=ZZZ&payment_method=tabby&payment_id=authorise_x"
        assertTrue(BnplReturn.isReturnTarget(url, returnUrls))
        assertNull(BnplReturn.markerOf(url, returnUrls))
    }

    @Test
    fun `a different host carrying the marker is not a leg`() {
        val url = "https://evil.example.com/v2?ni_sdk_result=success"
        assertNull(BnplReturn.markerOf(url, returnUrls))
    }

    @Test
    fun `a different path on our host is not a leg`() {
        val url = "https://paypage-dev.platform.network.ae/other?ni_sdk_result=success"
        assertNull(BnplReturn.markerOf(url, returnUrls))
    }

    @Test
    fun `scheme and host compare case-insensitively and a trailing slash is ignored`() {
        val url = "HTTPS://PayPage-Dev.Platform.Network.AE/v2/?code=ZZZ&ni_sdk_result=failure"
        assertEquals("failure", BnplReturn.markerOf(url, returnUrls))
    }

    @Test
    fun `null and unparseable urls are not legs`() {
        assertNull(BnplReturn.markerOf(null, returnUrls))
        assertNull(BnplReturn.originAndPath(""))
        assertNull(BnplReturn.markerOf("about:blank", returnUrls))
    }
}
