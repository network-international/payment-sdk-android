package payment.sdk.android.cardpayment.partialAuth

import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
import payment.sdk.android.partialAuth.PartialAuthViewModel
import java.lang.Exception

@OptIn(ExperimentalCoroutinesApi::class)
class PartialAuthViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val httpClient: HttpClient = mockk(relaxed = true)

    private lateinit var sut: PartialAuthViewModel

    private val url = "url"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = PartialAuthViewModel(
            httpClient,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test accept partial auth success`() = runTest {
        val states: MutableList<CardPaymentData> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        val body = """
            { "state": "CAPTURED" }
        """.trimIndent()
        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Success(mapOf(), body)

        sut.submitRequest(url, "token")

        assertEquals(states.last().code, CardPaymentData.STATUS_PAYMENT_CAPTURED)
    }

    @Test
    fun `test accept partial auth Error`() = runTest {
        val states: MutableList<CardPaymentData> = mutableListOf()

        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Failed(Exception())

        sut.submitRequest(url, "token")

        assertEquals(states[0].code, CardPaymentData.STATUS_PAYMENT_FAILED)
    }

    @Test
    fun `test partial auth declined`() = runTest {
        val states: MutableList<CardPaymentData> = mutableListOf()

        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }
        val body = """
            { "state": "PARTIAL_AUTH_DECLINED" }
        """.trimIndent()
        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Success(mapOf(), body)

        sut.submitRequest(url, "token")

        assertEquals(states[0].code, CardPaymentData.STATUS_PARTIAL_AUTH_DECLINED)
    }

    @Test
    fun `test partial auth declined failed`() = runTest {
        val states: MutableList<CardPaymentData> = mutableListOf()

        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }
        val body = """
            { "state": "PARTIAL_AUTH_DECLINE_FAILED" }
        """.trimIndent()

        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Success(mapOf(),body)

        sut.submitRequest(url, "token")

        assertEquals(states.last().code, CardPaymentData.STATUS_PARTIAL_AUTH_DECLINE_FAILED)
    }

    @Test
    fun `test auth partially authorised`() = runTest {
        val states: MutableList<CardPaymentData> = mutableListOf()

        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }
        val body = """
            { "state": "PARTIALLY_AUTHORISED" }
        """.trimIndent()

        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Success(mapOf(),body)

        sut.submitRequest(url, "token")

        assertEquals(states[0].code, CardPaymentData.STATUS_PARTIALLY_AUTHORISED)
    }
}