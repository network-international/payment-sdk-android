package payment.sdk.android.core.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
class SliceEligibilityInteractorTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private val sut = SliceEligibilityInteractor(httpClient)

    private val url = "https://api.sandbox.ngenius-payments.com/orders/o1/payments/p1/slice"
    private val token = "cookie-token"

    private val eligibleBody = """
        {
          "transactionAmount": {"currencyCode": "AED", "value": 50000},
          "indicator": "Y",
          "offers": [
            {"period":"3","rate":"0.00","fee":"0","feeType":"FIXED",
             "installmentAmount":{"currencyCode":"AED","value":16667},
             "totalAmount":{"currencyCode":"AED","value":50000}},
            {"period":"6","rate":"1.50","fee":"500","feeType":"FIXED",
             "installmentAmount":{"currencyCode":"AED","value":8417},
             "totalAmount":{"currencyCode":"AED","value":50500}}
          ]
        }
    """.trimIndent()

    @Test
    fun `returns the offers on success`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = eligibleBody
        )

        val result = sut.checkEligibility(url, token, "4111111111111111", "2030-12")

        assertTrue(result is SliceEligibilityResult.Success)
        with((result as SliceEligibilityResult.Success).response) {
            assertEquals(50000, transactionAmount.value)
            assertEquals("Y", indicator)
            assertEquals(2, offers.size)
            assertEquals("6", offers[1].period)
            assertEquals(50500, offers[1].totalAmount.value)
        }
    }

    @Test
    fun `an absent indicator decodes to null`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = """{"transactionAmount":{"currencyCode":"AED","value":100},"offers":[]}"""
        )

        val response = (sut.checkEligibility(url, token, "4111111111111111", "2030-12")
                as SliceEligibilityResult.Success).response

        assertNull(response.indicator)
        assertTrue(response.offers.isEmpty())
    }

    @Test
    fun `manual entry sends the card number under pan`() = runTest {
        val body = slot<Body>()
        coEvery { httpClient.post(any(), any(), capture(body)) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = eligibleBody
        )

        sut.checkEligibility(url, token, "4111111111111111", "2030-12", isSavedCardToken = false)

        val encoded = body.captured.encode()
        assertTrue(encoded.contains("\"pan\":\"4111111111111111\""))
        assertTrue(encoded.contains("\"expiry\":\"2030-12\""))
        assertTrue(!encoded.contains("cardToken"))
    }

    @Test
    fun `a saved card sends its token under cardToken because pan rejects tokens`() = runTest {
        val body = slot<Body>()
        coEvery { httpClient.post(any(), any(), capture(body)) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = eligibleBody
        )

        sut.checkEligibility(url, token, "tok-abc", "2030-12", isSavedCardToken = true)

        val encoded = body.captured.encode()
        assertTrue(encoded.contains("\"cardToken\":\"tok-abc\""))
        assertTrue(!encoded.contains("\"pan\""))
    }

    @Test
    fun `sends the session cookie and the v2 payment media type`() = runTest {
        val headers = slot<Map<String, String>>()
        coEvery { httpClient.post(url, capture(headers), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = eligibleBody
        )

        sut.checkEligibility(url, token, "4111111111111111", "2030-12")

        coVerify(exactly = 1) { httpClient.post(url, any(), any()) }
        assertEquals(token, headers.captured[SliceEligibilityInteractor.HEADER_COOKIE])
        assertEquals(
            "application/vnd.ni-payment.v2+json",
            headers.captured[SliceEligibilityInteractor.HEADER_ACCEPT]
        )
        assertEquals(
            "application/vnd.ni-payment.v2+json",
            headers.captured[SliceEligibilityInteractor.HEADER_CONTENT_TYPE]
        )
    }

    @Test
    fun `surfaces a transport failure as an error`() = runTest {
        val cause = Exception("network down")
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Failed(cause)

        val result = sut.checkEligibility(url, token, "4111111111111111", "2030-12")

        assertTrue(result is SliceEligibilityResult.Error)
        assertEquals(cause, (result as SliceEligibilityResult.Error).error)
    }
}
