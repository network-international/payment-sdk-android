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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import payment.sdk.android.core.interactor.AaniQrApiInteractor
import payment.sdk.android.core.interactor.AaniQrCreateResponse

@OptIn(ExperimentalCoroutinesApi::class)
class AaniPayViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var sut: AaniPayViewModel

    private val aaniPayApiInterator: AaniPayApiInterator = mockk(relaxed = true)
    private val aaniPoolingApiInteractor: AaniPoolingApiInteractor = mockk(relaxed = true)
    private val aaniQrApiInteractor: AaniQrApiInteractor = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = AaniPayViewModel(
            dispatcher = testDispatcher,
            aaniPayApiInterator = aaniPayApiInterator,
            aaniPoolingApiInteractor = aaniPoolingApiInteractor,
            aaniQrApiInteractor = aaniQrApiInteractor
        )
    }

    @After
    fun tearDown() {
        sut.cancelQr()
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

    @Test
    fun `onSubmit with QR_CODE updates state to Loading and then QrDisplay on success`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Loading)
        assertTrue(states[2] is AaniPayVMState.QrDisplay)
    }

    @Test
    fun `onSubmit with QR_CODE QrDisplay state contains correct amount and currencyCode`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        val qrDisplay = states[2] as AaniPayVMState.QrDisplay
        assertEquals(10000.0, qrDisplay.amount, 0.001)
        assertEquals("AED", qrDisplay.currencyCode)
        assertEquals("https://deeplink.example.com", qrDisplay.qrContent)
    }

    @Test
    fun `onSubmit with QR_CODE updates state to Loading and then Error on failure`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Error(Exception("QR creation failed"))

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Loading)
        assertTrue(states[2] is AaniPayVMState.Error)
    }

    @Test
    fun `onSubmit with QR_CODE polling transitions to Error on FAILED`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(
            ClassLoader.getSystemResource("aaniPaymentResponse.json").readText(),
            AaniPayResponse::class.java
        )

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "FAILED"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(6100)

        assertTrue(states.last() is AaniPayVMState.Error)
    }

    @Test
    fun `onSubmit with QR_CODE polling transitions to Success on CAPTURED`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(
            ClassLoader.getSystemResource("aaniPaymentResponse.json").readText(),
            AaniPayResponse::class.java
        )

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "CAPTURED"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(6100)

        assertTrue(states.last() is AaniPayVMState.Success)
    }

    @Test
    fun `onSubmit with QR_CODE polling transitions to Success on PURCHASED`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(
            ClassLoader.getSystemResource("aaniPaymentResponse.json").readText(),
            AaniPayResponse::class.java
        )

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PURCHASED"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(6100)

        assertTrue(states.last() is AaniPayVMState.Success)
    }

    @Test
    fun `onSubmit with QR_CODE polling retries on PENDING then transitions to Success`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(
            ClassLoader.getSystemResource("aaniPaymentResponse.json").readText(),
            AaniPayResponse::class.java
        )

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returnsMany listOf("PENDING", "CAPTURED")

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(12100)

        assertTrue(states.last() is AaniPayVMState.Success)
    }

    @Test
    fun `onSubmit with QR_CODE polling transitions to Error when network request fails`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        // Interactor returns FAILED on network error — polling loop must exit to Error
        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "FAILED"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(6100)

        assertTrue(states.last() is AaniPayVMState.Error)
    }

    @Test
    fun `alias polling internal timeout does not override Success when CAPTURED returned before timeout`() =
        runTest {
            val states: MutableList<AaniPayVMState> = mutableListOf()
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

            // CAPTURED on first poll — loop exits, withTimeout block completes normally
            coEvery {
                aaniPoolingApiInteractor.startPooling(any(), any())
            } returns "CAPTURED"

            sut.onSubmit(
                args = args,
                accessToken = accessToken,
                alias = AaniIDType.MOBILE_NUMBER,
                value = "1234567890",
                payerIp = "1.1.1.1"
            )

            advanceTimeBy(6100)
            assertTrue(states.last() is AaniPayVMState.Success)

            // Advancing well past POLLING_TIMEOUT_MS must not change the Success state
            advanceTimeBy(AaniPayViewModel.POLLING_TIMEOUT_MS)
            assertTrue(states.last() is AaniPayVMState.Success)
        }

    // endregion

    // region cancelQr

    @Test
    fun `cancelQr transitions state to Cancelled after QR is created`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        sut.cancelQr()

        assertTrue(states.last() is AaniPayVMState.Cancelled)
    }

    @Test
    fun `cancelQr calls interactor cancelQr with correct url, token, qrCodeId and qrTransactionId`() =
        runTest {
            val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

            coEvery {
                aaniQrApiInteractor.createQr(any(), any())
            } returns AaniQrCreateResponse.Success(aaniResponse)

            coEvery {
                aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
            } returns "PENDING"

            sut.onSubmit(
                args = args,
                accessToken = accessToken,
                alias = AaniIDType.QR_CODE,
                value = "",
                payerIp = "1.1.1.1"
            )

            sut.cancelQr()

            coVerify(exactly = 1) {
                aaniQrApiInteractor.cancelQr(
                    url = "anniQrPaymentLink",
                    accessToken = accessToken,
                    qrCodeId = QR_CODE_ID,
                    qrTransactionId = QR_TRANSACTION_ID
                )
            }
        }

    @Test
    fun `cancelQr does nothing if QR was never created`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        sut.cancelQr()

        assertEquals(1, states.size)
        assertTrue(states[0] is AaniPayVMState.Init)
        coVerify(exactly = 0) { aaniQrApiInteractor.cancelQr(any(), any(), any(), any()) }
    }

    @Test
    fun `cancelQr stops polling before first poll occurs`() = runTest {
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        sut.cancelQr()

        // Advance past two polling intervals — no poll should fire since job was cancelled
        advanceTimeBy(12100)

        coVerify(exactly = 0) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }
    }

    @Test
    fun `cancelQr stops further polling after first poll`() = runTest {
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        // Let first poll fire
        advanceTimeBy(6100)
        coVerify(exactly = 1) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }

        sut.cancelQr()

        // Advance past second interval — no second poll should happen
        advanceTimeBy(6100)
        coVerify(exactly = 1) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }
    }

    @Test
    fun `cancelQr state sequence is Init then Loading then QrDisplay then Cancelled`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        sut.cancelQr()

        assertTrue(states[0] is AaniPayVMState.Init)
        assertTrue(states[1] is AaniPayVMState.Loading)
        assertTrue(states[2] is AaniPayVMState.QrDisplay)
        assertTrue(states[3] is AaniPayVMState.Cancelled)
    }

    @Test
    fun `cancelQr does not override Success state when polling already completed`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "CAPTURED"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        // Let polling complete with CAPTURED
        advanceTimeBy(6100)
        assertTrue(states.last() is AaniPayVMState.Success)

        // cancelQr guards against overriding a terminal state — state must stay Success
        sut.cancelQr()
        assertTrue(states.last() is AaniPayVMState.Success)
    }

    // endregion

    // region QR flow - additional coverage

    @Test
    fun `onSubmit with QR_CODE calls createQr with correct URL from args`() = runTest {
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        coVerify(exactly = 1) {
            aaniQrApiInteractor.createQr("anniQrPaymentLink", accessToken)
        }
    }

    @Test
    fun `onSubmit with QR_CODE polls with correct qrCodeId and qrTransactionId from create response`() = runTest {
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(6100)

        coVerify {
            aaniQrApiInteractor.pollQrStatus(
                url = any(),
                accessToken = accessToken,
                qrCodeId = QR_CODE_ID,
                qrTransactionId = QR_TRANSACTION_ID
            )
        }
    }

    @Test
    fun `onSubmit with QR_CODE state remains QrDisplay while polling returns PENDING`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        // Advance through multiple poll cycles — state must stay QrDisplay while PENDING
        advanceTimeBy(15000)

        assertTrue(states.last() is AaniPayVMState.QrDisplay)
    }

    @Test
    fun `onSubmit with QR_CODE polling retries on PENDING then transitions to Success on PURCHASED`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returnsMany listOf("PENDING", "PURCHASED")

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(12100)

        assertTrue(states.last() is AaniPayVMState.Success)
    }

    // endregion

    // region polling stops on terminal state

    @Test
    fun `polling stops after CAPTURED - no further poll calls`() = runTest {
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "CAPTURED"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(6100)
        coVerify(exactly = 1) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }

        // Advance well past another interval — polling loop must have exited
        advanceTimeBy(12100)
        coVerify(exactly = 1) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }
    }

    @Test
    fun `polling stops after PURCHASED - no further poll calls`() = runTest {
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PURCHASED"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(6100)
        coVerify(exactly = 1) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }

        advanceTimeBy(12100)
        coVerify(exactly = 1) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }
    }

    @Test
    fun `polling stops after FAILED - no further poll calls`() = runTest {
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "FAILED"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        advanceTimeBy(6100)
        coVerify(exactly = 1) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }

        advanceTimeBy(12100)
        coVerify(exactly = 1) { aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any()) }
    }

    @Test
    fun `polling state is not Success or Error when cancelled mid-flight`() = runTest {
        val states: MutableList<AaniPayVMState> = mutableListOf()
        val aaniResponse = Gson().fromJson(qrResponseJson, AaniPayResponse::class.java)

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            aaniQrApiInteractor.createQr(any(), any())
        } returns AaniQrCreateResponse.Success(aaniResponse)

        coEvery {
            aaniQrApiInteractor.pollQrStatus(any(), any(), any(), any())
        } returns "PENDING"

        sut.onSubmit(
            args = args,
            accessToken = accessToken,
            alias = AaniIDType.QR_CODE,
            value = "",
            payerIp = "1.1.1.1"
        )

        sut.cancelQr()

        // After cancel, state must be Cancelled — never Success or Error
        assertFalse(states.any { it is AaniPayVMState.Success })
        assertFalse(states.any { it is AaniPayVMState.Error })
        assertTrue(states.last() is AaniPayVMState.Cancelled)
    }

    // endregion

    companion object {
        private val args =
            AaniPayLauncher.Config(100.0, "link", "anniQrPaymentLink", "AED", "anniPaymentLink", "1.1.1.1")
        private const val accessToken = "randomToken"

        private const val QR_CODE_ID = "test-qr-code-id"
        private const val QR_TRANSACTION_ID = "test-qr-transaction-id"

        private val qrResponseJson = """
            {
              "_id": "urn:payment:3a23b6cd",
              "_links": {
                "cnp:aani-status": { "href": "https://api.example.com/status" },
                "self": { "href": "https://api.example.com/self" }
              },
              "reference": "3a23b6cd",
              "state": "PENDING",
              "amount": { "currencyCode": "AED", "value": 10000 },
              "aani": {
                "deepLinkUrl": "https://deeplink.example.com",
                "qrCodeId": "$QR_CODE_ID",
                "qrCodeTransactionId": "$QR_TRANSACTION_ID"
              }
            }
        """.trimIndent()
    }
}