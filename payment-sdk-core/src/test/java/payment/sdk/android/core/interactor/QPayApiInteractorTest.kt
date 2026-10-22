package payment.sdk.android.core.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class QPayApiInteractorTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private val sut = QPayApiInteractor(httpClient)

    private val url = "https://api.sandbox.ngenius-payments.com/orders/o1/payments/p1/qpay"
    private val token = "accessToken"

    @Test
    fun `returns the decoded form fields on success`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = """{"redirectUri":"https://qcb.example.com/pay","Amount":500,"PUN":"PUN1"}"""
        )

        val result = sut.initQPay(url, token)

        assertTrue(result is QPayApiResponse.Success)
        with((result as QPayApiResponse.Success).response) {
            assertEquals("https://qcb.example.com/pay", redirectUri)
            assertEquals("500", amount)
            assertEquals("PUN1", pun)
        }
    }

    @Test
    fun `posts to the given url with the bearer token`() = runTest {
        val headers = slot<Map<String, String>>()
        coEvery { httpClient.post(url, capture(headers), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = "{}"
        )

        sut.initQPay(url, token)

        coVerify(exactly = 1) { httpClient.post(url, any(), any()) }
        assertEquals("Bearer $token", headers.captured["Authorization"])
        assertEquals("application/vnd.ni-payment.v2+json", headers.captured["Content-Type"])
    }

    @Test
    fun `always sends a json body because the backend rejects a missing one`() = runTest {
        val body = slot<Body>()
        coEvery { httpClient.post(any(), any(), capture(body)) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = "{}"
        )

        sut.initQPay(url, token)

        assertTrue(body.captured is Body.Json)
        assertEquals("{}", body.captured.encode())
        assertTrue(body.captured.isNotEmpty())
    }

    @Test
    fun `surfaces a transport failure as an error`() = runTest {
        val cause = Exception("network down")
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Failed(cause)

        val result = sut.initQPay(url, token)

        assertTrue(result is QPayApiResponse.Error)
        assertEquals(cause, (result as QPayApiResponse.Error).error)
    }

    @Test
    fun `an undecodable body becomes an error rather than a crash`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = "<html>gateway error</html>"
        )

        val result = sut.initQPay(url, token)

        assertTrue(result is QPayApiResponse.Error)
        assertEquals(
            "Failed to decode QPay init response",
            (result as QPayApiResponse.Error).error.message
        )
    }
}
