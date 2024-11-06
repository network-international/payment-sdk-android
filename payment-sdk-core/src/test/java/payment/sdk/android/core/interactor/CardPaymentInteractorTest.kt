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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse

@OptIn(ExperimentalCoroutinesApi::class)
class CardPaymentInteractorTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val httpClient: HttpClient = mockk(relaxed = true)

    private lateinit var sut: CardPaymentInteractor

    private val makeCardPaymentRequest = MakeCardPaymentRequest(
        "",
        "",
        pan = "4301099393939939",
        cvv = "123",
        cardHolder = "test",
        expiry = "",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = CardPaymentInteractor(httpClient)
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
            body = SavedCardPaymentApiInteractorTest.paymentResponse.trimIndent()
        )

        val response = sut.makeCardPayment(makeCardPaymentRequest)

        Assert.assertTrue(response is CardPaymentResponse.Success)
    }

    @Test
    fun `return failed when http response fails`() = runTest {
        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Failed(Exception("Network Error"))

        val response = sut.makeCardPayment(makeCardPaymentRequest)

        Assert.assertTrue(response is CardPaymentResponse.Error)
    }
}