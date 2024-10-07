package payment.sdk.android.cardpayment.payments

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
import payment.sdk.android.payments.PaymentsVMEffects
import payment.sdk.android.payments.PaymentsVMUiState
import payment.sdk.android.payments.PaymentsViewModel
import payment.sdk.android.payments.GooglePayUiConfig
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.core.Order
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.api.SDKHttpResponse
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.CardPaymentInteractor
import payment.sdk.android.core.interactor.CardPaymentResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.GetPayerIpInteractor
import payment.sdk.android.core.interactor.GooglePayAcceptInteractor
import payment.sdk.android.core.interactor.VisaInstallmentPlanInteractor
import payment.sdk.android.core.interactor.VisaPlansResponse
import payment.sdk.android.googlepay.GooglePayConfigFactory
import payment.sdk.android.payments.PaymentsRequest

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val intent: PaymentsRequest = PaymentsRequest.Builder()
        .payPageUrl(TEST_PAYMENT_URL)
        .gatewayAuthorizationUrl("authUrl")
        .setLanguageCode("en")
        .build()

    private val authApiInteractor: AuthApiInteractor = mockk(relaxed = true)
    private val cardPaymentInteractor: CardPaymentInteractor = mockk(relaxed = true)
    private val threeDSecureFactory: ThreeDSecureFactory = mockk(relaxed = true)
    private val visaInstalmentPlanInteractor: VisaInstallmentPlanInteractor = mockk(relaxed = true)
    private val getPayerIpInteractor: GetPayerIpInteractor = mockk(relaxed = true)
    private val googlePayConfigFactory: GooglePayConfigFactory = mockk(relaxed = true)
    private val googlePayAcceptInteractor: GooglePayAcceptInteractor = mockk(relaxed = true)
    private val getOrderApiInteractor: GetOrderApiInteractor = mockk(relaxed = true)

    private lateinit var sut: PaymentsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = PaymentsViewModel(
            cardPaymentsIntent = intent,
            authApiInteractor = authApiInteractor,
            cardPaymentInteractor = cardPaymentInteractor,
            getPayerIpInteractor = getPayerIpInteractor,
            visaInstalmentPlanInteractor = visaInstalmentPlanInteractor,
            threeDSecureFactory = threeDSecureFactory,
            googlePayConfigFactory = googlePayConfigFactory,
            googlePayAcceptInteractor = googlePayAcceptInteractor,
            getOrderApiInteractor = getOrderApiInteractor,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test authorize success without googlePayConfig`() = runTest {
        val states: MutableList<PaymentsVMUiState> = mutableListOf()

        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )
        backgroundScope.launch(testDispatcher) {
            sut.uiState.toList(states)
        }

        coEvery { authApiInteractor.authenticate(any(), any()) } returns AuthResponse.Success(
            listOf(PAYMENT_TOKEN_COOKIE, ACCESS_TOKEN_COOKIE), "orderUrl"
        )

        coEvery {
            googlePayConfigFactory.checkGooglePayConfig(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns null

        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse

        sut.authorize()

        coVerify(exactly = 1) { authApiInteractor.authenticate(any(), any()) }

        assertTrue(states[0] is PaymentsVMUiState.Init)
        assertTrue(states[1] is PaymentsVMUiState.Loading)
        assertTrue(states[2] is PaymentsVMUiState.Authorized)
    }

    @Test
    fun `test authorize success without with googlePayConfig but cannot pay with googlePay`() =
        runTest {
            val states: MutableList<PaymentsVMUiState> = mutableListOf()
            val orderResponse = Gson().fromJson(
                ClassLoader.getSystemResource("orderResponse.json").readText(),
                Order::class.java
            )

            coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse
            backgroundScope.launch(testDispatcher) {
                sut.uiState.toList(states)
            }

            coEvery { authApiInteractor.authenticate(any(), any()) } returns AuthResponse.Success(
                listOf(PAYMENT_TOKEN_COOKIE, ACCESS_TOKEN_COOKIE), "orderUrl"
            )

            coEvery {
                googlePayConfigFactory.checkGooglePayConfig(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns GooglePayUiConfig(
                allowedPaymentMethods = "",
                task = mockk(),
                canUseGooglePay = false,
                googlePayAcceptUrl = ""
            )

            sut.authorize()

            coVerify(exactly = 1) { authApiInteractor.authenticate(any(), any()) }

            assertTrue(states[0] is PaymentsVMUiState.Init)
            assertTrue(states[1] is PaymentsVMUiState.Loading)
            assertTrue(states.last() is PaymentsVMUiState.Authorized)

            val state = (states.last() as PaymentsVMUiState.Authorized)

            assertFalse(state.showWallets)
        }

    @Test
    fun `test authorize failure with invalid auth code`() = runTest {
        val states: MutableList<PaymentsVMUiState> = mutableListOf()

        val effects: MutableList<PaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.uiState.toList(states)
        }

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        coEvery { authApiInteractor.authenticate(any(), any()) } returns AuthResponse.Error(
            Exception()
        )

        sut.authorize()

        coVerify(exactly = 1) { authApiInteractor.authenticate(any(), any()) }

        assertTrue(states[0] is PaymentsVMUiState.Init)
        assertTrue(states[1] is PaymentsVMUiState.Loading)
        assertTrue(effects.first() is PaymentsVMEffects.Failed)
    }

    @Test
    fun `test make card payment success`() = runTest {
        val response = Gson().fromJson(
            ClassLoader.getSystemResource("paymentResponse.json").readText(),
            PaymentResponse::class.java
        )
        val effects: MutableList<PaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        coEvery {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns CardPaymentResponse.Success(response)

        sut.makeCardPayment(
            "selfUrl",
            "cardPaymentUrl",
            "accessToken",
            "paymentCookie",
            "1234567812345678",
            "orderUrl",
            "12/24",
            "123",
            "John Doe",
            0.0,
            "AED"
        )

        coVerify(exactly = 1) {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        assertTrue(effects.isNotEmpty())
        assertTrue(effects.first() is PaymentsVMEffects.Captured)
    }

    @Test
    fun `test make card payment failure`() = runTest {
        val effects: MutableList<PaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        coEvery {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns CardPaymentResponse.Error(
            Exception()
        )

        coEvery {
            getPayerIpInteractor.getPayerIp(TEST_PAYMENT_URL)
        } returns "1.1.1.1"

        sut.makeCardPayment(
            "selfUrl",
            "cardPaymentUrl",
            "accessToken",
            "paymentCookie",
            "1234567812345678",
            "orderUrl",
            "12/24",
            "123",
            "John Doe",
            0.0,
            "AED"
        )

        coVerify(exactly = 1) {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        assertTrue(effects.isNotEmpty())
        assertTrue(effects.first() is PaymentsVMEffects.Failed)
    }

    @Test
    fun `test make card payment shows visa plans`() = runTest {
        val effects: MutableList<PaymentsVMEffects> = mutableListOf()

        val visaResponse = Gson().fromJson(
            ClassLoader.getSystemResource("visaEligibilityResponse.json").readText(),
            VisaPlans::class.java
        )

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        coEvery {
            getPayerIpInteractor.getPayerIp(TEST_PAYMENT_URL)
        } returns "1.1.1.1"

        coEvery {
            visaInstalmentPlanInteractor.getPlans(
                any(),
                any(),
                any(),
                any()
            )
        } returns VisaPlansResponse.Success(visaResponse)

        sut.makeCardPayment(
            "selfUrl",
            "cardPaymentUrl",
            "accessToken",
            "paymentCookie",
            "1234567812345678",
            "orderUrl",
            "12/24",
            "123",
            "John Doe",
            0.0,
            "AED"
        )

        coVerify(exactly = 1) { visaInstalmentPlanInteractor.getPlans(any(), any(), any(), any()) }

        assertTrue(effects.isNotEmpty())
        assertTrue(effects.first() is PaymentsVMEffects.ShowVisaPlans)
    }

    @Test
    fun `test initiate 3ds payment flow`() = runTest {
        val response = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureTwoResponse.json").readText(),
            PaymentResponse::class.java
        )

        val effects: MutableList<PaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        coEvery {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns CardPaymentResponse.Success(
            response
        )

        coEvery {
            getPayerIpInteractor.getPayerIp(TEST_PAYMENT_URL)
        } returns "1.1.1.1"

        coEvery {
            threeDSecureFactory.buildThreeDSecureDto(any())
        } returns ThreeDSecureDto("", "", "", "")

        sut.makeCardPayment(
            "selfUrl",
            "cardPaymentUrl",
            "accessToken",
            "paymentCookie",
            "1234567812345678",
            "orderUrl",
            "12/24",
            "123",
            "John Doe",
            0.0,
            "AED"
        )

        coVerify(exactly = 1) { threeDSecureFactory.buildThreeDSecureTwoDto(any(), any(), any()) }

        assertTrue(effects.isNotEmpty())
        assertTrue(effects.first() is PaymentsVMEffects.InitiateThreeDSTwo)
    }

    @Test
    fun `test acceptGooglePay success`() = runTest {
        val effects: MutableList<PaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        coEvery { authApiInteractor.authenticate(any(), any()) } returns AuthResponse.Success(
            listOf(PAYMENT_TOKEN_COOKIE, ACCESS_TOKEN_COOKIE), "orderUrl"
        )

        coEvery {
            googlePayConfigFactory.checkGooglePayConfig(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns GooglePayUiConfig(
            allowedPaymentMethods = "",
            task = mockk(),
            canUseGooglePay = false,
            googlePayAcceptUrl = ""
        )
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )

        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse
        sut.authorize()

        coEvery {
            googlePayAcceptInteractor.accept(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Success(
            emptyMap(), ""
        )

        sut.acceptGooglePay("paymentDataJson")

        coVerify(exactly = 1) { googlePayAcceptInteractor.accept(any(), any(), any()) }

        assertTrue(effects.isNotEmpty())
        assertTrue(effects.first() is PaymentsVMEffects.Captured)
    }

    @Test
    fun `test acceptGooglePay failure if not authorized`() = runTest {
        val effects: MutableList<PaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        coEvery {
            googlePayAcceptInteractor.accept(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Success(
            emptyMap(), ""
        )

        sut.acceptGooglePay("paymentDataJson")

        coVerify(exactly = 0) { googlePayAcceptInteractor.accept(any(), any(), any()) }

        assertTrue(effects.isNotEmpty())
        assertTrue(effects.first() is PaymentsVMEffects.Failed)
        assertEquals(
            "Authorization or Google Pay URL is missing",
            (effects.first() as PaymentsVMEffects.Failed).error
        )
    }

    @Test
    fun `test acceptGooglePay failure when authorized but accept fails`() = runTest {
        val effects: MutableList<PaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )

        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse
        coEvery { authApiInteractor.authenticate(any(), any()) } returns AuthResponse.Success(
            listOf(PAYMENT_TOKEN_COOKIE, ACCESS_TOKEN_COOKIE), "orderUrl"
        )

        coEvery {
            googlePayConfigFactory.checkGooglePayConfig(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns GooglePayUiConfig(
            allowedPaymentMethods = "",
            task = mockk(),
            canUseGooglePay = false,
            googlePayAcceptUrl = ""
        )

        sut.authorize()

        coEvery {
            googlePayAcceptInteractor.accept(
                any(),
                any(),
                any()
            )
        } returns SDKHttpResponse.Failed(Exception("502"))

        sut.acceptGooglePay("paymentDataJson")

        coVerify(exactly = 1) { googlePayAcceptInteractor.accept(any(), any(), any()) }

        assertTrue(effects.isNotEmpty())
        assertTrue(effects.first() is PaymentsVMEffects.Failed)
        assertEquals(
            "Google Pay accept failed: 502",
            (effects.first() as PaymentsVMEffects.Failed).error
        )
    }

    companion object {
        private const val TEST_PAYMENT_URL = "https://test.com/?code=authCode"
        private const val ACCESS_TOKEN_VALUE = "randomToken"
        private const val ACCESS_TOKEN_COOKIE =
            "${AuthResponse.ACCESS_TOKEN}=$ACCESS_TOKEN_VALUE;secure;Httponly"
        private const val PAYMENT_TOKEN_COOKIE =
            "${AuthResponse.PAYMENT_TOKEN}=somepaytoken;secure;Httponly"
    }
}