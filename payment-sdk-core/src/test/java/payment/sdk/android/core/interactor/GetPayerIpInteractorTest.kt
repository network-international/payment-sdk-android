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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class GetPayerIpInteractorTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var sut: GetPayerIpInteractor

    val payPageUrl = "https://paypage.sandbox.ngenius-payments.com/?code=1234Das"
    val ipUrl = "https://paypage.sandbox.ngenius-payments.com/api/requester-ip"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = GetPayerIpInteractor(httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSuccess() = runTest {
        coEvery {
            httpClient.get(ipUrl, any(), any())
        } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = "{\"requesterIp\": \"103.255.181.1\"}"
        )

        val response = sut.getPayerIp(payPageUrl = payPageUrl)

        coVerify(exactly = 1) {
            httpClient.get(ipUrl, any(), any())
        }

        assertEquals(response, "103.255.181.1")
    }

    @Test
    fun `test return null when response is failed`() = runTest {
        coEvery {
            httpClient.get(ipUrl, any(), any())
        } returns SDKHttpResponse.Failed(Exception())

        val response = sut.getPayerIp(payPageUrl = payPageUrl)

        coVerify(exactly = 1) {
            httpClient.get(ipUrl, any(), any())
        }

        assertEquals(response, null)
    }

    @Test
    fun `test return null when body is empty`() = runTest {
        coEvery {
            httpClient.get(any(), any(), any())
        } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = ""
        )

        val response = sut.getPayerIp(payPageUrl = payPageUrl)

        coVerify(exactly = 1) {
            httpClient.get(ipUrl, any(), any())
        }

        assertEquals(response, null)
    }
}