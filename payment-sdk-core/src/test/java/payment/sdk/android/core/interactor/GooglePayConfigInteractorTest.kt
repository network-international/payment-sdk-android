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
class GooglePayConfigInteractorTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var sut: GooglePayConfigInteractor

    val configUrl = "https://example.com/google-pay/config"
    val accessToken = "testAccessToken"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = GooglePayConfigInteractor(httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test success returns valid config`() = runTest {
        val responseJson = """
            {
                "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                "allowedPaymentMethods": ["CARD"],
                "environment": "TEST",
                "gatewayName": "exampleGateway",
                "merchantInfo": {
                    "name": "Merchant Name",
                    "reference": "Merchant Reference"
                }
            }
        """.trimIndent()

        coEvery {
            httpClient.get(configUrl, mapOf("Authorization" to "Bearer $accessToken"), any())
        } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = responseJson
        )

        val response = sut.getConfig(configUrl, accessToken)

        coVerify(exactly = 1) {
            httpClient.get(configUrl, mapOf("Authorization" to "Bearer $accessToken"), any())
        }

        assertEquals(listOf("PAN_ONLY", "CRYPTOGRAM_3DS"), response?.allowedAuthMethods)
        assertEquals(listOf("CARD"), response?.allowedPaymentMethods)
        assertEquals("TEST", response?.environment)
        assertEquals("exampleGateway", response?.gatewayName)

        assertEquals("Merchant Name", response?.merchantInfo?.name)
        assertEquals("Merchant Reference", response?.merchantInfo?.reference)
    }

    @Test
    fun `test return null when response is failed`() = runTest {
        coEvery {
            httpClient.get(configUrl, mapOf("Authorization" to "Bearer $accessToken"), any())
        } returns SDKHttpResponse.Failed(Exception())

        val response = sut.getConfig(configUrl, accessToken)

        coVerify(exactly = 1) {
            httpClient.get(configUrl, mapOf("Authorization" to "Bearer $accessToken"), any())
        }

        assertEquals(null, response)
    }

    @Test
    fun `test return null when body is empty`() = runTest {
        coEvery {
            httpClient.get(configUrl, mapOf("Authorization" to "Bearer $accessToken"), any())
        } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = ""
        )

        val response = sut.getConfig(configUrl, accessToken)

        coVerify(exactly = 1) {
            httpClient.get(configUrl, mapOf("Authorization" to "Bearer $accessToken"), any())
        }

        assertEquals(null, response)
    }
}