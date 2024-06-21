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
import payment.sdk.android.demo.Result
import org.junit.Test
import payment.sdk.android.core.Order
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentType
import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.demo.model.PaymentOrderAmount

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOrderApiInteractorTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var sut: CreateOrderApiInteractor

    private val apiService: ApiService = mockk(relaxed = true)

    private val environment = Environment(
        type = EnvironmentType.DEV,
        id = "some",
        name = "test",
        apiKey = "key",
        outletReference = "ref",
        realm = "name"
    )

    private val orderRequest = OrderRequest(
        action = "SALE",
        amount = PaymentOrderAmount(
            value = 10.0,
            currencyCode = "AED"
        ),
        language = "en",
        merchantAttributes = emptyMap()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = CreateOrderApiInteractor(apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createOrder runs successfully`() = runTest {
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("order_response.json").readText(),
            Order::class.java
        )

        coEvery { apiService.getAccessToken(any(), any(), any()) } returns "accessToken"
        coEvery { apiService.createOrder(any(), any(), any()) } returns orderResponse

        val result = sut.createOrder(environment, orderRequest)

        assertEquals(result, Result.Success(orderResponse))
    }

    @Test
    fun `createOrder fails to get access token`() = runTest {
        coEvery { apiService.getAccessToken(any(), any(), any()) } returns null

        val result = sut.createOrder(environment, orderRequest)

        assertEquals(result, Result.Error<Any>("Failed to get access token"))
    }

    @Test
    fun `createOrder gets access token but fails to create order`() = runTest {
        coEvery { apiService.getAccessToken(any(), any(), any()) } returns "accessToken"
        coEvery { apiService.createOrder(any(), any(), any()) } returns null

        val result = sut.createOrder(environment, orderRequest)

        assertEquals(result, Result.Error<Any>("Failed to create order"))
    }
}