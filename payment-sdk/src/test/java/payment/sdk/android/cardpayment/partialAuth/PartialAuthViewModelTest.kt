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
import payment.sdk.android.core.api.HttpClient
import payment.sdk.android.core.api.SDKHttpResponse
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
        val states: MutableList<PartialAuthVMState> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }
        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Success(mapOf(), "")

        sut.accept(url, "token")

        assertEquals(states[0].state, PartialAuthState.INIT)
        assertEquals(states[1].state, PartialAuthState.LOADING)
        assertEquals(states[2].state, PartialAuthState.SUCCESS)
    }

    @Test
    fun `test accept partial auth Error`() = runTest {
        val states: MutableList<PartialAuthVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Failed(Exception())

        sut.accept(url, "token")

        assertEquals(states[0].state, PartialAuthState.INIT)
        assertEquals(states[1].state, PartialAuthState.LOADING)
        assertEquals(states[2].state, PartialAuthState.ERROR)
    }

    @Test
    fun `test decline partial auth success`() = runTest {
        val states: MutableList<PartialAuthVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Success(mapOf(), "")

        sut.decline(url, "token")

        assertEquals(states[0].state, PartialAuthState.INIT)
        assertEquals(states[1].state, PartialAuthState.LOADING)
        assertEquals(states[2].state, PartialAuthState.DECLINED)
    }

    @Test
    fun `test decline partial auth Error`() = runTest {
        val states: MutableList<PartialAuthVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        coEvery {
            httpClient.put(any(), any(), any())
        } returns SDKHttpResponse.Failed(Exception())

        sut.decline(url, "token")

        assertEquals(states[0].state, PartialAuthState.INIT)
        assertEquals(states[1].state, PartialAuthState.LOADING)
        assertEquals(states[2].state, PartialAuthState.ERROR)
    }
}