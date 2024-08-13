package payment.sdk.android.cardpayment.aaniPay

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import payment.sdk.android.cardpayment.aaniPay.model.AaniIDType
import payment.sdk.android.cardpayment.aaniPay.model.AaniPayActivityArgs
import payment.sdk.android.cardpayment.aaniPay.model.AaniPayVMState
import payment.sdk.android.core.AaniPayResponse
import payment.sdk.android.core.interactor.AaniPayApiInterator
import payment.sdk.android.core.interactor.AaniPayApiResponse
import payment.sdk.android.core.interactor.AaniPoolingApiInteractor
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.GetPayerIpInteractor

@OptIn(ExperimentalCoroutinesApi::class)
class AaniPayViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var sut: AaniPayViewModel

    private val authApiInteractor: AuthApiInteractor = mockk(relaxed = true)
    private val getPayerIpInteractor: GetPayerIpInteractor = mockk(relaxed = true)
    private val aaniPayApiInterator: AaniPayApiInterator = mockk(relaxed = true)
    private val aaniPoolingApiInteractor: AaniPoolingApiInteractor = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = AaniPayViewModel(
            authApiInteractor = authApiInteractor,
            getPayerIpInteractor = getPayerIpInteractor,
            dispatcher = testDispatcher,
            aaniPayApiInterator = aaniPayApiInterator,
            aaniPoolingApiInteractor = aaniPoolingApiInteractor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `authorize state is Error when auth code in payment url not found`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        sut.authorize(authUrl = "blank", paymentUrl = "")

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Error)
    }

    @Test
    fun `authorize state is error when AuthApiInteractor return failed`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Error(Exception("error"))

        sut.authorize(authUrl = authUrl, paymentUrl = paymentUrl)

        coVerify(exactly = 1) { authApiInteractor.authenticate(authUrl, authCode) }

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Loading)
        assertTrue(states[2] is AaniPayVMState.Error)
    }

    @Test
    fun `authorize state is Authorized when AuthApiInteractor return success`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(paymentCookie, accessTokenCookie), "orderUrl")

        sut.authorize(authUrl = authUrl, paymentUrl = paymentUrl)

        coVerify(exactly = 1) {
            authApiInteractor.authenticate(authUrl, authCode)
        }

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Loading)
        assertTrue(states[2] is AaniPayVMState.Authorized)
    }

    @Test
    fun `onSubmit updates state to Loading and then Error on payment failure`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val accessToken = "accessToken"
        val alias = AaniIDType.MOBILE_NUMBER
        val value = "1234567890"

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            getPayerIpInteractor.getPayerIp(any())
        } returns "payerIp"

        coEvery {
            aaniPayApiInterator.makePayment(any(), any(), any())
        } returns AaniPayApiResponse.Error(Exception("Payment failed"))

        sut.onSubmit(args, accessToken, alias, value)

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Loading)
        assertTrue(states[2] is AaniPayVMState.Error)
    }

    @Test
    fun `onSubmit updates state to Loading and then Pooling on payment success`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val alias = AaniIDType.MOBILE_NUMBER
        val value = "1234567890"
        val aaniResponse = Gson().fromJson(
            ClassLoader.getSystemResource("aaniPaymentResponse.json").readText(),
            AaniPayResponse::class.java
        )
        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            getPayerIpInteractor.getPayerIp(any())
        } returns "payerIp"

        coEvery {
            aaniPayApiInterator.makePayment(any(), any(), any())
        } returns AaniPayApiResponse.Success(aaniResponse)

        sut.onSubmit(args, accessToken, alias, value)

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Loading)
        assertTrue(states[2] is AaniPayVMState.Pooling)

        advanceTimeBy(6000)

        coEvery {
            aaniPoolingApiInteractor.startPooling(any(), any())
        } returns "CAPTURED"
        assertTrue(states.last() is AaniPayVMState.Pooling)
    }

    @Test
    fun `onSubmit calls startPooling and status returns FAILED`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()

        val alias = AaniIDType.MOBILE_NUMBER
        val value = "1234567890"
        val aaniResponse = Gson().fromJson(
            ClassLoader.getSystemResource("aaniPaymentResponse.json").readText(),
            AaniPayResponse::class.java
        )
        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        coEvery {
            getPayerIpInteractor.getPayerIp(any())
        } returns "payerIp"

        coEvery {
            aaniPayApiInterator.makePayment(any(), any(), any())
        } returns AaniPayApiResponse.Success(aaniResponse)

        sut.onSubmit(args, accessToken, alias, value)

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Loading)
        assertTrue(states[2] is AaniPayVMState.Pooling)

        coEvery {
            aaniPoolingApiInteractor.startPooling(any(), any())
        } returns "FAILED"

        advanceTimeBy(6100)

        assertTrue(states.last() is AaniPayVMState.Error)
    }

    companion object {
        private val args =
            AaniPayActivityArgs(100.0, "link", "AED", "anniPaymentLink", "")
        private const val authUrl = "authUrl"
        private const val paymentUrl = "https://test.com/?code=authCode"
        private const val authCode = "authCode"
        private const val accessToken = "randomToken"
        private const val accessTokenCookie =
            "${AuthResponse.ACCESS_TOKEN}=$accessToken;secure;Httponly"
        private const val paymentCookie =
            "${AuthResponse.PAYMENT_TOKEN}=somepaytoken;secure;Httponly"
    }
}