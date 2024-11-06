package payment.sdk.android.core.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GooglePayAcceptInteractorTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var sut: GooglePayAcceptInteractor

    val acceptUrl = "https://example.com/google-pay/accept"
    val accessToken = "testAccessToken"
    val paymentData = "samplePaymentData"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = GooglePayAcceptInteractor(httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test success returns SDKHttpResponse_Success`() = runTest {
        val responseBody = "{\"status\":\"accepted\"}"

        coEvery {
            httpClient.post(any(), any(), any())
        } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = responseBody
        )

        val response = sut.accept(acceptUrl, accessToken, paymentData)

        coVerify(exactly = 1) { httpClient.post(any(), any(), any()) }

        assertTrue(response is SDKHttpResponse.Success)
        assertEquals(responseBody, (response as SDKHttpResponse.Success).body)
    }

    @Test
    fun `test failure returns SDKHttpResponse_Failed`() = runTest {
        val exception = Exception("Error occurred")

        coEvery {
            httpClient.post(any(), any(), any())
        } returns SDKHttpResponse.Failed(exception)

        val response = sut.accept(acceptUrl, accessToken, paymentData)

        coVerify(exactly = 1) { httpClient.post(any(), any(), any()) }

        assertTrue(response is SDKHttpResponse.Failed)
        assertEquals(exception, (response as SDKHttpResponse.Failed).error)
    }
}