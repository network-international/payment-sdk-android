package payment.sdk.android.cardpayment.payments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import payment.sdk.android.payments.PaymentsRequest

class PaymentsRequestTest {

    @Test
    fun `test successful build`() {
        val request = PaymentsRequest.builder()
            .gatewayAuthorizationUrl("https://example.com/gateway")
            .payPageUrl("https://example.com/pay")
            .build()

        assertEquals("https://example.com/gateway", request.authorizationUrl)
        assertEquals("https://example.com/pay", request.paymentUrl)
    }

    @Test
    fun `test missing gatewayUrl`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PaymentsRequest.builder()
                .payPageUrl("https://example.com/pay")
                .build()
        }
        assertEquals("Gateway url should not be null", exception.message)
    }

    @Test
    fun `test missing payPageUrl`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PaymentsRequest.builder()
                .gatewayAuthorizationUrl("https://example.com/gateway")
                .build()
        }
        assertEquals("Pay page url should not be null", exception.message)
    }
}