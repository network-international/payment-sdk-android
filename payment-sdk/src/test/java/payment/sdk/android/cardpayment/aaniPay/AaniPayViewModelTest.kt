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
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.aaniPay.AaniPayViewModel
import payment.sdk.android.aaniPay.model.AaniIDType
import payment.sdk.android.aaniPay.model.AaniPayVMState
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

    private val aaniPayApiInterator: AaniPayApiInterator = mockk(relaxed = true)
    private val aaniPoolingApiInteractor: AaniPoolingApiInteractor = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = AaniPayViewModel(
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
    fun `onSubmit updates state to Loading and then Error on payment failure`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val accessToken = "accessToken"
        val alias = AaniIDType.MOBILE_NUMBER
        val value = "1234567890"

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniPayApiInterator.makePayment(any(), any(), any())
        } returns AaniPayApiResponse.Error(Exception("Payment failed"))

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = alias,
            value = value,
            payerIp = "1.1.1.1"
        )

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
            aaniPayApiInterator.makePayment(any(), any(), any())
        } returns AaniPayApiResponse.Success(aaniResponse)

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = alias,
            value = value,
            payerIp = "1.1.1.1"
        )

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
            aaniPayApiInterator.makePayment(any(), any(), any())
        } returns AaniPayApiResponse.Success(aaniResponse)

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = alias,
            value = value,
            payerIp = "1.1.1.1"
        )

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
            AaniPayLauncher.Config(100.0, "link", "AED", "anniPaymentLink", "")
        private const val accessToken = "randomToken"
    }
}