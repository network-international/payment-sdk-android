package payment.sdk.android.core.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class BenefitApiInteractorTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private val sut = BenefitApiInteractor(httpClient)

    private val url = "https://api.sandbox.ngenius-payments.com/orders/o1/payments/p1/benefit"
    private val token = "accessToken"

    @Test
    fun `returns the hosted page url on success`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = """
                {"paymentId":"pay-1","paymentUrl":"https://benefit.example.com/pay/1","status":"Initiated"}
            """.trimIndent()
        )

        val result = sut.initBenefit(url, token)

        assertTrue(result is BenefitApiResponse.Success)
        with((result as BenefitApiResponse.Success).response) {
            assertEquals("pay-1", paymentId)
            assertEquals("https://benefit.example.com/pay/1", paymentUrl)
            assertTrue(isInitiated)
        }
    }

    @Test
    fun `a non-Initiated status is not treated as initiated`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = """{"status":"Failed","errorMessage":"card not enrolled"}"""
        )

        val response = (sut.initBenefit(url, token) as BenefitApiResponse.Success).response

        assertFalse(response.isInitiated)
        assertEquals("card not enrolled", response.errorMessage)
    }

    @Test
    fun `status comparison ignores case`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = """{"status":"INITIATED"}"""
        )

        assertTrue((sut.initBenefit(url, token) as BenefitApiResponse.Success).response.isInitiated)
    }

    @Test
    fun `a missing status is not initiated`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = "{}"
        )

        assertFalse((sut.initBenefit(url, token) as BenefitApiResponse.Success).response.isInitiated)
    }

    @Test
    fun `sends the v2 payment media type and the bearer token`() = runTest {
        val headers = slot<Map<String, String>>()
        coEvery { httpClient.post(url, capture(headers), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = "{}"
        )

        sut.initBenefit(url, token)

        coVerify(exactly = 1) { httpClient.post(url, any(), any()) }
        assertEquals("Bearer $token", headers.captured["Authorization"])
        assertEquals("application/vnd.ni-payment.v2+json", headers.captured["Accept"])
        assertEquals("application/vnd.ni-payment.v2+json", headers.captured["Content-Type"])
    }

    @Test
    fun `surfaces a transport failure as an error`() = runTest {
        val cause = Exception("network down")
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Failed(cause)

        val result = sut.initBenefit(url, token)

        assertTrue(result is BenefitApiResponse.Error)
        assertEquals(cause, (result as BenefitApiResponse.Error).error)
    }

    @Test
    fun `an undecodable body becomes an error rather than a crash`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = "<html>502</html>"
        )

        val result = sut.initBenefit(url, token)

        assertTrue(result is BenefitApiResponse.Error)
        assertEquals(
            "Failed to decode Benefit init response",
            (result as BenefitApiResponse.Error).error.message
        )
    }
}
