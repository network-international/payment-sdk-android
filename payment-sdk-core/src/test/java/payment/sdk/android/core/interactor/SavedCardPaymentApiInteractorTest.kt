package payment.sdk.android.core.interactor

import android.app.Application
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.SliceRequest
import payment.sdk.android.core.api.Body
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCardPaymentApiInteractorTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val httpClient: HttpClient = mockk(relaxed = true)

    private val app: Application = mockk(relaxed = true)

    private lateinit var sut: SavedCardPaymentApiInteractor

    private val savedCardPaymentRequest = SavedCardPaymentApiRequest(
        accessToken = "",
        paymentCookie = "",
        savedCardUrl = "",
        savedCard = SavedCard(
            cardholderName = "",
            expiry = "",
            maskedPan = "",
            scheme = "",
            cardToken = "",
            recaptureCsc = false
        ),
        payerIp = "1.1.1.1",
        cvv = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = SavedCardPaymentApiInteractor(httpClient, app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `return success response with correct data`() = runTest {
        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Success(
            headers = mapOf(),
            body = paymentResponse.trimIndent()
        )

        val response = sut.doSavedCardPayment(savedCardPaymentRequest)

        assertTrue(response is SavedCardResponse.Success)
    }

    @Test
    fun `sends the selected slice plan under the slice key`() = runTest {
        val body = slot<Body>()
        coEvery {
            httpClient.put(any(), any(), capture(body))
        } returns SDKHttpResponse.Success(headers = mapOf(), body = paymentResponse.trimIndent())

        sut.doSavedCardPayment(
            savedCardPaymentRequest.copy(
                sliceRequest = SliceRequest(period = "4", rate = "0", fee = "0")
            )
        )

        val sent = (body.captured as Body.StringBody).value
        assertTrue(sent.contains("\"slice\":{\"period\":\"4\",\"rate\":\"0\",\"fee\":\"0\"}"))
    }

    @Test
    fun `omits the slice key entirely when no plan was selected`() = runTest {
        val body = slot<Body>()
        coEvery {
            httpClient.put(any(), any(), capture(body))
        } returns SDKHttpResponse.Success(headers = mapOf(), body = paymentResponse.trimIndent())

        sut.doSavedCardPayment(savedCardPaymentRequest)

        val sent = (body.captured as Body.StringBody).value
        assertFalse(sent.contains("slice"))
        // `vis` still goes out as an explicit null - the gateway's saved-card route wants the key.
        assertTrue(sent.contains("\"vis\":null"))
    }

    @Test
    fun `return failed when http response fails`() = runTest {
        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Failed(Exception("Network Error"))

        val response = sut.doSavedCardPayment(savedCardPaymentRequest)

        assertTrue(response is SavedCardResponse.Error)
    }

    companion object {
        const val paymentResponse = """
            {
    "_id": "urn:payment",
    "_links": {
        "cnp:3ds2-challenge-response": {
            "href": ""
        },
        "self": {
            "href": ""
        },
        "cnp:3ds2-authentication": {
            "href": ""
        },
        "cnp:3ds": {
            "href": ""
        },
        "curies": [
            {
                "name": "cnp",
                "href": "",
                "templated": true
            }
        ]
    },
    "reference": "",
    "paymentMethod": {
        "expiry": "",
        "cardholderName": "test",
        "name": "MASTERCARD",
        "cardType": "DEBIT",
        "cardCategory": "PREPAID RELOADABLE",
        "issuingOrg": "MASTERCARD - OPERATIONS TEAM AND TECHNOLOGIES - BUSINESS OPERATIONS",
        "issuingCountry": "US",
        "issuingOrgWebsite": "HTTPS://WWW.MASTERCARD.US/",
        "issuingOrgPhoneNumber": "",
        "pan": "",
        "cvv": ""
    },
    "state": "AWAIT_3DS",
    "amount": {
        "currencyCode": "AED",
        "value": 140
    },
    "updateDateTime": "",
    "outletId": "",
    "orderReference": "",
    "originIp": "",
    "3ds2": {
        "messageVersion": "2.1.0",
        "threeDSMethodURL": "",
        "threeDSServerTransID": "",
        "directoryServerID": ""
    }
}
        """
    }
}