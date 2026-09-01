package payment.sdk.android.core.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class GetOrderApiInteractorTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private val sut = GetOrderApiInteractor(httpClient)

    private val url = "https://api.sandbox.ngenius-payments.com/orders/o1"
    private val token = "accessToken"

    private val orderBody = """
        {
          "reference": "ref-1",
          "outletId": "outlet-1",
          "action": "PURCHASE",
          "amount": {"currencyCode": "AED", "value": 5000},
          "paymentMethods": {"card": ["VISA", "MASTERCARD"], "wallet": ["APPLE_PAY"]},
          "_links": {"payment": {"href": "https://paypage.example.com/?code=ABC"}}
        }
    """.trimIndent()

    @Test
    fun `returns the parsed order on success`() = runTest {
        coEvery { httpClient.get(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = orderBody
        )

        val order = sut.getOrder(url, token)

        assertNotNull(order)
        assertEquals("ref-1", order!!.reference)
        assertEquals("outlet-1", order.outletId)
        assertEquals("PURCHASE", order.action)
        assertEquals("AED", order.amount?.currencyCode)
        assertEquals(5000.0, order.amount?.value!!, 0.001)
    }

    @Test
    fun `card and wallet payment methods survive parsing`() = runTest {
        coEvery { httpClient.get(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = orderBody
        )

        val methods = sut.getOrder(url, token)!!.paymentMethods

        assertTrue(methods?.card?.contains("VISA") == true)
        assertTrue(methods?.wallet?.contains("APPLE_PAY") == true)
    }

    @Test
    fun `gets the given url with the bearer token and no body`() = runTest {
        val headers = slot<Map<String, String>>()
        val body = slot<Body>()
        coEvery { httpClient.get(url, capture(headers), capture(body)) } returns
                SDKHttpResponse.Success(headers = emptyMap(), body = orderBody)

        sut.getOrder(url, token)

        coVerify(exactly = 1) { httpClient.get(url, any(), any()) }
        assertEquals(
            "Bearer $token",
            headers.captured[TransactionServiceHttpAdapter.HEADER_AUTHORIZATION]
        )
        assertTrue(body.captured is Body.Empty)
    }

    @Test
    fun `a transport failure yields null rather than throwing`() = runTest {
        coEvery { httpClient.get(any(), any(), any()) } returns
                SDKHttpResponse.Failed(Exception("network down"))

        assertNull(sut.getOrder(url, token))
    }

    @Test
    fun `an order without optional sections still parses`() = runTest {
        coEvery { httpClient.get(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = """{"reference":"ref-1"}"""
        )

        val order = sut.getOrder(url, token)

        assertEquals("ref-1", order?.reference)
        assertNull(order?.paymentMethods)
        assertNull(order?.amount)
    }
}
