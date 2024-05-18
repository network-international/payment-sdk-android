package payment.sdk.android.demo.http

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import payment.sdk.android.core.Order
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.demo.model.PaymentOrderAmount

@OptIn(ExperimentalCoroutinesApi::class)
class ApiServiceAdapterTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var sut: ApiService
    private val httpClient: HttpClient = mockk(relaxed = true)
    private val orderRequest = OrderRequest(
        action = "SALE",
        amount = PaymentOrderAmount(
            value = 10.0,
            currencyCode = "AED"
        ),
        language = "en",
        merchantAttributes = mapOf("one" to "one"),
        savedCard = SavedCard(
            cardholderName = "",
            scheme = "",
            expiry = "",
            cardToken = "",
            recaptureCsc = true,
            maskedPan = ""
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = ApiServiceAdapter(httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `http get order success`() = runTest {
        val response = ClassLoader.getSystemResource("order_response.json").readText()
        coEvery {
            httpClient.get(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Success(body = response, headers = mapOf())

        val result = sut.getOrder(any(), any(), any())

        assertEquals(result?.reference, Gson().fromJson(response, Order::class.java).reference)
    }

    @Test
    fun `http get order fails`() = runTest {
        coEvery {
            httpClient.get(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Failed(Exception())

        val result = sut.getOrder(any(), any(), any())

        assertEquals(result, null)
    }

    @Test
    fun `http create order success`() = runTest {
        val response = ClassLoader.getSystemResource("order_response.json").readText()
        coEvery {
            httpClient.post(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Success(body = response, headers = mapOf())

        val result = sut.createOrder(any(), any(), orderRequest)

        assertEquals(result?.reference, Gson().fromJson(response, Order::class.java).reference)
    }

    @Test
    fun `http create order fails`() = runTest {
        coEvery {
            httpClient.post(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Failed(Exception())

        val result = sut.createOrder(any(), any(), orderRequest)

        assertEquals(result, null)
    }

    @Test
    fun `http get access token success`() = runTest {
        val response = ClassLoader.getSystemResource("auth_response.json").readText()
        coEvery {
            httpClient.post(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Success(body = response, headers = mapOf())

        val result = sut.getAccessToken(any(), any(), any())

        assertEquals("access_token", result)
    }

    @Test
    fun `http get access token fails`() = runTest {
        coEvery {
            httpClient.post(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Failed(Exception())

        val result = sut.getAccessToken(any(), any(), any())

        assertEquals(result, null)
    }
}