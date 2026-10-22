package payment.sdk.android.core.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class OrderStatePollerTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private val slept = mutableListOf<Long>()
    private val sut = OrderStatePoller(httpClient) { slept += it }

    private val orderUrl = "https://api.sandbox.ngenius-payments.com/transactions/outlets/o1/orders/r1"
    private val cookie = "payment-token=abc"

    private fun orderWith(state: String) = """
        {
          "reference": "r1",
          "_embedded": { "payment": [ { "reference": "p1", "state": "$state" } ] }
        }
    """.trimIndent()

    private fun respondWith(vararg bodies: String) {
        val responses = bodies.map { SDKHttpResponse.Success(emptyMap(), it) }
        coEvery { httpClient.get(orderUrl, any(), any()) } returnsMany responses
    }

    @Test
    fun `returns terminal state on the first read without sleeping`() = runTest {
        respondWith(orderWith("PURCHASED"))

        val result = sut.awaitTerminalState(orderUrl, cookie)

        assertTrue(result is OrderStatePoller.Result.Terminal)
        assertEquals("PURCHASED", (result as OrderStatePoller.Result.Terminal).state)
        assertEquals(emptyList<Long>(), slept)
        coVerify(exactly = 1) { httpClient.get(orderUrl, any(), any()) }
    }

    @Test
    fun `keeps polling while the payment is still in flight`() = runTest {
        respondWith(orderWith("AWAIT_3DS"), orderWith("AWAIT_3DS"), orderWith("CAPTURED"))

        val result = sut.awaitTerminalState(orderUrl, cookie, maxAttempts = 5, intervalMs = 1_000)

        assertEquals("CAPTURED", (result as OrderStatePoller.Result.Terminal).state)
        assertEquals(listOf(1_000L, 1_000L), slept)
    }

    @Test
    fun `a payment that settles as failed is reported as failed`() = runTest {
        respondWith(orderWith("FAILED"))

        val result = sut.awaitTerminalState(orderUrl, cookie)

        assertEquals("FAILED", (result as OrderStatePoller.Result.Terminal).state)
    }

    @Test
    fun `lower-case states from the gateway are still recognised`() = runTest {
        respondWith(orderWith("purchased"))

        val result = sut.awaitTerminalState(orderUrl, cookie)

        assertEquals("PURCHASED", (result as OrderStatePoller.Result.Terminal).state)
    }

    @Test
    fun `reports the last state seen when the budget runs out`() = runTest {
        respondWith(orderWith("AWAIT_3DS"), orderWith("AWAIT_3DS"))

        val result = sut.awaitTerminalState(orderUrl, cookie, maxAttempts = 2, intervalMs = 500)

        assertEquals("AWAIT_3DS", (result as OrderStatePoller.Result.Inconclusive).lastState)
    }

    @Test
    fun `a failing read does not end the poll - a later read still decides`() = runTest {
        coEvery { httpClient.get(orderUrl, any(), any()) } returnsMany listOf(
            SDKHttpResponse.Failed(IllegalStateException("timeout")),
            SDKHttpResponse.Success(emptyMap(), orderWith("AUTHORISED"))
        )

        val result = sut.awaitTerminalState(orderUrl, cookie, maxAttempts = 3, intervalMs = 100)

        assertEquals("AUTHORISED", (result as OrderStatePoller.Result.Terminal).state)
    }

    @Test
    fun `reports no state when the order is never readable`() = runTest {
        coEvery { httpClient.get(orderUrl, any(), any()) } returns
                SDKHttpResponse.Failed(IllegalStateException("timeout"))

        val result = sut.awaitTerminalState(orderUrl, cookie, maxAttempts = 2, intervalMs = 100)

        assertNull((result as OrderStatePoller.Result.Inconclusive).lastState)
    }

    @Test
    fun `authenticates the read with the payment cookie`() = runTest {
        respondWith(orderWith("PURCHASED"))

        sut.awaitTerminalState(orderUrl, cookie)

        coVerify {
            httpClient.get(orderUrl, mapOf("Cookie" to cookie), any<Body.Empty>())
        }
    }

    @Test
    fun `partial auth approval ends the poll so the caller can take over`() = runTest {
        respondWith(orderWith("AWAITING_PARTIAL_AUTH_APPROVAL"))

        val result = sut.awaitTerminalState(orderUrl, cookie)

        assertEquals(
            "AWAITING_PARTIAL_AUTH_APPROVAL",
            (result as OrderStatePoller.Result.Terminal).state
        )
    }
}
