package payment.sdk.android.core.interactor

import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import payment.sdk.android.core.TransactionServiceHttpAdapter
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class AuthApiInteractorTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var sut: AuthApiInteractor

    private val authUrl = "authurl"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = AuthApiInteractor(httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `returns success response and contains expected values`() = runTest {

        val paymentCookie = "${AuthResponse.PAYMENT_TOKEN}=somepaytoken;secure;Httponly"
        val accessToken = "randomToken"
        val accessTokenCookie = "${AuthResponse.ACCESS_TOKEN}=$accessToken;secure;Httponly"

        coEvery {
            httpClient.post(authUrl, any(), any())
        } returns SDKHttpResponse.Success(
            headers = mapOf(
                TransactionServiceHttpAdapter.HEADER_SET_COOKIE to listOf(
                    paymentCookie,
                    accessTokenCookie
                ),
            ),
            body = successJson.trimIndent()
        )

        val authResponse = sut.authenticate(authUrl, "paymentUrl")


        // Verify that the AuthResponse is a Success
        assertTrue(authResponse is AuthResponse.Success)

        // Verify that the AuthResponse contains the expected values
        assertEquals(
            paymentCookie,
            (authResponse as AuthResponse.Success).getPaymentCookie()
        )
        assertEquals(accessToken, authResponse.getAccessToken())
    }

    @Test
    fun `return error when no cookies found in response`() = runTest {
        coEvery {
            httpClient.post(authUrl, any(), any())
        } returns SDKHttpResponse.Success(
            headers = emptyMap(),
            body = "{}"
        )

        val authResponse = sut.authenticate(authUrl, "paymentUrl")

        assertTrue(authResponse is AuthResponse.Error)

        assertTrue((authResponse as AuthResponse.Error).error is IllegalArgumentException)

        assertEquals(authResponse.error.message, AuthApiInteractor.AUTH_ERROR_COOKIE)
    }

    @Test
    fun `return error when order url not found in response`() = runTest {
        coEvery {
            httpClient.post(authUrl, any(), any())
        } returns SDKHttpResponse.Success(
            headers = mapOf(
                TransactionServiceHttpAdapter.HEADER_SET_COOKIE to listOf(
                    "paymentCookie",
                    "accessTokenCookie"
                )
            ),
            body = "{}"
        )

        val authResponse = sut.authenticate(authUrl, "paymentUrl")

        assertTrue(authResponse is AuthResponse.Error)

        assertTrue((authResponse as AuthResponse.Error).error is IllegalArgumentException)

        assertEquals(authResponse.error.message, AuthApiInteractor.AUTH_ERROR_ORDER_URL)
    }

    @Test
    fun `return error when http respose fails`() = runTest {

        coEvery {
            httpClient.post(authUrl, headers = any(), body = any())
        } returns SDKHttpResponse.Failed(error = Exception("Network error"))

        val authResponse = sut.authenticate("authUrl", "authCode")

        assertTrue(authResponse is AuthResponse.Error)
    }

    companion object {
        const val successJson = """
        {
            "_links": {
                "cnp:order": {
                    "href": "orderUrl"
                }
            }
        }
    """
    }
}