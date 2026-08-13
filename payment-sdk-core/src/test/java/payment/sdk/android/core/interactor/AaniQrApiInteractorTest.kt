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
class AaniQrApiInteractorTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private val sut = AaniQrApiInteractor(httpClient)

    private val url = "https://api.sandbox.ngenius-payments.com/orders/o1/payments/p1/aani-qr"
    private val token = "accessToken"

    private val createBody = """
        {
          "_id": "urn:payment:p1",
          "state": "AWAITING_QR_SCAN",
          "amount": {"currencyCode": "AED", "value": 10000},
          "aani": {
            "qrCodeId": "qr-1",
            "qrCodeTransactionId": "qrtx-1",
            "emvQrData": "00020101021226",
            "deepLinkUrl": "https://aani.example.com/pay"
          },
          "_links": {
            "self": {"href": "$url"},
            "cnp:aani-status": {"href": "$url/status"}
          }
        }
    """.trimIndent()

    // region createQr

    @Test
    fun `createQr returns the qr payload on success`() = runTest {
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = createBody
        )

        val result = sut.createQr(url, token)

        assertTrue(result is AaniQrCreateResponse.Success)
        with((result as AaniQrCreateResponse.Success).aaniPayResponse) {
            assertEquals("qr-1", aani?.qrCodeId)
            assertEquals("qrtx-1", aani?.qrCodeTransactionId)
            assertEquals("00020101021226", aani?.emvQrData)
            assertEquals(10000.0, amount.value!!, 0.001)
        }
    }

    @Test
    fun `createQr posts an empty json object with the bearer token`() = runTest {
        val headers = slot<Map<String, String>>()
        val body = slot<Body>()
        coEvery { httpClient.post(url, capture(headers), capture(body)) } returns
                SDKHttpResponse.Success(headers = emptyMap(), body = createBody)

        sut.createQr(url, token)

        coVerify(exactly = 1) { httpClient.post(url, any(), any()) }
        assertEquals("Bearer $token", headers.captured["Authorization"])
        assertEquals("{}", body.captured.encode())
    }

    @Test
    fun `createQr surfaces a transport failure as an error`() = runTest {
        val cause = Exception("network down")
        coEvery { httpClient.post(any(), any(), any()) } returns SDKHttpResponse.Failed(cause)

        val result = sut.createQr(url, token)

        assertTrue(result is AaniQrCreateResponse.Error)
        assertEquals(cause, (result as AaniQrCreateResponse.Error).error)
    }

    // endregion

    // region pollQrStatus

    @Test
    fun `pollQrStatus returns the state the gateway reports`() = runTest {
        coEvery { httpClient.get(any(), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = """{"state":"CAPTURED"}"""
        )

        assertEquals("CAPTURED", sut.pollQrStatus(url, token, "qr-1", "qrtx-1"))
    }

    @Test
    fun `pollQrStatus asks the status endpoint with both qr identifiers`() = runTest {
        val requested = slot<String>()
        coEvery { httpClient.get(capture(requested), any(), any()) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = """{"state":"PENDING"}"""
        )

        sut.pollQrStatus(url, token, "qr-1", "qrtx-1")

        assertEquals("$url/status?qrCodeId=qr-1&qrTransactionId=qrtx-1", requested.captured)
    }

    @Test
    fun `pollQrStatus sends an empty body`() = runTest {
        val body = slot<Body>()
        coEvery { httpClient.get(any(), any(), capture(body)) } returns SDKHttpResponse.Success(
            headers = emptyMap(), body = """{"state":"PENDING"}"""
        )

        sut.pollQrStatus(url, token, "qr-1", "qrtx-1")

        assertTrue(body.captured is Body.Empty)
    }

    @Test
    fun `a failed poll reads as FAILED so the caller stops polling`() = runTest {
        coEvery { httpClient.get(any(), any(), any()) } returns
                SDKHttpResponse.Failed(Exception("network down"))

        assertEquals("FAILED", sut.pollQrStatus(url, token, "qr-1", "qrtx-1"))
    }

    // endregion

    // region cancelQr

    @Test
    fun `cancelQr deletes the qr with both identifiers`() = runTest {
        val requested = slot<String>()
        val headers = slot<Map<String, String>>()
        coEvery { httpClient.delete(capture(requested), capture(headers)) } returns
                SDKHttpResponse.Success(headers = emptyMap(), body = "")

        sut.cancelQr(url, token, "qr-1", "qrtx-1")

        assertEquals("$url?qrCodeId=qr-1&qrTransactionId=qrtx-1", requested.captured)
        assertEquals("Bearer $token", headers.captured["Authorization"])
    }

    @Test
    fun `cancelQr ignores a failure because there is nothing left to do`() = runTest {
        coEvery { httpClient.delete(any(), any()) } returns
                SDKHttpResponse.Failed(Exception("network down"))

        sut.cancelQr(url, token, "qr-1", "qrtx-1")

        coVerify(exactly = 1) { httpClient.delete(any(), any()) }
    }

    // endregion
}
