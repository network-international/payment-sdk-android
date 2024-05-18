package payment.sdk.android.demo.http

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase
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
import payment.sdk.android.demo.Result
import payment.sdk.android.core.Order
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentType
import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.demo.model.PaymentOrderAmount

@OptIn(ExperimentalCoroutinesApi::class)
class GetOrderApiInteractorTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var sut: GetOrderApiInteractor

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
        sut = GetOrderApiInteractor(apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getOrder runs successfully`() = runTest {
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("order_response.json").readText(),
            Order::class.java
        )

        coEvery { apiService.getAccessToken(any(), any(), any()) } returns "accessToken"
        coEvery { apiService.getOrder(any(), any(), any()) } returns orderResponse

        val result = sut.getOrder(environment, "orderReference")

        TestCase.assertEquals(result, Result.Success(orderResponse))
    }

    @Test
    fun `getOrder fails to get access token`() = runTest {
        coEvery { apiService.getAccessToken(any(), any(), any()) } returns null

        val result = sut.getOrder(environment, "orderRequest")

        TestCase.assertEquals(result, Result.Error<Any>("Failed to get access token"))
    }

    @Test
    fun `getOrder gets access token but fails to create order`() = runTest {
        coEvery { apiService.getAccessToken(any(), any(), any()) } returns "accessToken"
        coEvery { apiService.getOrder(any(), any(), any()) } returns null

        val result = sut.getOrder(environment, "orderRequest")

        TestCase.assertEquals(result, Result.Error<Any>("Order not found"))
    }

    @Test
    fun `getOrder error if order reference is empty`() = runTest {
        val result = sut.getOrder(environment, null)

        TestCase.assertEquals(result, Result.Error<Any>("Order reference is null"))
    }
}