package payment.sdk.android.cardpayment.savedCard

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.core.CostInfo
import payment.sdk.android.core.LastInstallment
import payment.sdk.android.core.MatchedPlan
import payment.sdk.android.core.Order
import payment.sdk.android.core.OrderAmount
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.SavedCard
import payment.sdk.android.core.TermsAndCondition
import payment.sdk.android.core.VisaPlans
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.GetPayerIpInteractor
import payment.sdk.android.core.interactor.SavedCardPaymentApiInteractor
import payment.sdk.android.core.interactor.SavedCardPaymentApiRequest
import payment.sdk.android.core.interactor.SavedCardResponse
import payment.sdk.android.core.interactor.VisaInstallmentPlanInteractor
import payment.sdk.android.core.interactor.VisaPlansResponse
import payment.sdk.android.savedCard.SavedPaymentViewModel
import payment.sdk.android.savedCard.model.SavedCardPaymentState
import payment.sdk.android.savedCard.model.SavedCardPaymentsVMEffects

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCardViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val authApiInteractor: AuthApiInteractor = mockk(relaxed = true)
    private val savedCardPaymentApiInteractor: SavedCardPaymentApiInteractor = mockk(relaxed = true)
    private val threeDSecureFactory: ThreeDSecureFactory = mockk(relaxed = true)
    private val visaInstalmentPlanInteractor: VisaInstallmentPlanInteractor = mockk(relaxed = true)
    private val getPayerIpInteractor: GetPayerIpInteractor = mockk(relaxed = true)
    private val getOrderApiInteractor: GetOrderApiInteractor = mockk(relaxed = true)

    private lateinit var sut: SavedPaymentViewModel

    private val mockVisaPlans = VisaPlans(
        matchedPlans = listOf(
            MatchedPlan(
                costInfo = CostInfo(
                    annualPercentageRate = 12.5,
                    currency = "USD",
                    lastInstallment = LastInstallment(
                        amount = 100.0,
                        installmentFee = 5.0,
                        totalAmount = 105.0,
                        upfrontFee = 2.0
                    ),
                    totalFees = 5.0,
                    totalPlanCost = 1005.0,
                    totalRecurringFees = 10.0,
                    totalUpfrontFees = 2.0
                ),
                fundedBy = listOf("Bank1", "Bank2"),
                installmentFrequency = "Monthly",
                name = "12-Month Installment Plan",
                numberOfInstallments = 12,
                termsAndConditions = listOf(
                    TermsAndCondition(
                        languageCode = "en",
                        text = "Terms and Conditions apply",
                        url = "https://example.com/terms",
                        version = 1
                    )
                ),
                type = "Standard",
                vPlanID = "V123",
                vPlanIDRef = "V123-REF"
            )
        )
    )

    private val savedCardPaymentRequest = SavedCardPaymentApiRequest(
        accessToken = "accessToken",
        savedCardUrl = "savedCardUrl",
        savedCard = SavedCard(
            cardholderName = "Mr Something",
            expiry = "2027-12",
            maskedPan = "476108******2022",
            scheme = "VISA",
            cardToken = "MTcxYzU4N2MtMmE2ZS00YjI1LTg0MmEtY2U2NTIwODJiODlm",
            recaptureCsc = true
        ),
        cvv = "123",
        payerIp = "127.0.0.1"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = SavedPaymentViewModel(
            authApiInteractor = authApiInteractor,
            savedCardPaymentApiInteractor = savedCardPaymentApiInteractor,
            getPayerIpInteractor = getPayerIpInteractor,
            visaInstalmentPlanInteractor = visaInstalmentPlanInteractor,
            threeDSecureFactory = threeDSecureFactory,
            getOrderApiInteractor = getOrderApiInteractor,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `authorize state is error when AuthApiInteractor return failed`() = runTest {
        val effects: MutableList<SavedCardPaymentsVMEffects> = mutableListOf()
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Error(Exception("error"))

        sut.authorize(paymentUrl, authUrl, "cvv")

        coVerify(exactly = 1) { authApiInteractor.authenticate(any(), any()) }
        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Loading)
        assertTrue(effects.first() is SavedCardPaymentsVMEffects.Failed)
    }

    @Test
    fun `when auth success verify CaptureCvv state`() =
        runTest {
            val states: MutableList<SavedCardPaymentState> = mutableListOf()

            val orderResponse = Gson().fromJson(
                ClassLoader.getSystemResource("orderResponse.json").readText(),
                Order::class.java
            )

            backgroundScope.launch(testDispatcher) {
                sut.state.toList(states)
            }

            coEvery {
                authApiInteractor.authenticate(any(), any())
            } returns AuthResponse.Success(listOf(paymentCookie, accessTokenCookie), "orderUrl")

            coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse

            coEvery { getPayerIpInteractor.getPayerIp(any()) } returns "1.1.1.1"

            sut.authorize(paymentUrl, authUrl, null)

            coVerify(exactly = 1) { authApiInteractor.authenticate(any(), any()) }

            assertTrue(states[0] is SavedCardPaymentState.Init)
            assertTrue(states[1] is SavedCardPaymentState.Loading)
            assertTrue(states.last() is SavedCardPaymentState.CaptureCvv)
        }

    @Test
    fun `authorize state is CaptureCvv when AuthApiInteractor return success and cvv is null`() =
        runTest {
            val states: MutableList<SavedCardPaymentState> = mutableListOf()

            val visaResponse = Gson().fromJson(
                ClassLoader.getSystemResource("visaEligibilityResponse.json").readText(),
                VisaPlans::class.java
            )

            val orderResponse = Gson().fromJson(
                ClassLoader.getSystemResource("orderResponse.json").readText(),
                Order::class.java
            )

            backgroundScope.launch(testDispatcher) {
                sut.state.toList(states)
            }

            coEvery {
                authApiInteractor.authenticate(any(), any())
            } returns AuthResponse.Success(listOf(paymentCookie, accessTokenCookie), "orderUrl")
            coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse
            coEvery {
                visaInstalmentPlanInteractor.getPlans(any(), any(), any(), any())
            } returns VisaPlansResponse.Success(visaResponse)

            sut.authorize(paymentUrl, authUrl, "123")

            assertTrue(states[0] is SavedCardPaymentState.Init)
            assertTrue(states[1] is SavedCardPaymentState.Loading)
            assertTrue(states.last() is SavedCardPaymentState.ShowVisaPlans)
        }

    @Test
    fun `getOrder returns incomplete data emits Failed effect`() = runTest {
        val effects: MutableList<SavedCardPaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }
        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(paymentCookie, accessTokenCookie), "orderUrl")
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns null

        sut.authorize(paymentUrl, authUrl, "cvv")

        assertTrue(effects.last() is SavedCardPaymentsVMEffects.Failed)
    }

    @Test
    fun `initiatePayment threeDSTwo during success flow`() = runTest {
        val effects: MutableList<SavedCardPaymentsVMEffects> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }
        val paymentResponse = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureTwoResponse.json").readText(),
            PaymentResponse::class.java
        )
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )
        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(paymentCookie, accessTokenCookie), "orderUrl")
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse
        coEvery {
            threeDSecureFactory.buildThreeDSecureDto(any())
        } returns ThreeDSecureDto("", "", "", "")
        coEvery { getPayerIpInteractor.getPayerIp(any()) } returns "1.1.1.1"
        coEvery { savedCardPaymentApiInteractor.doSavedCardPayment(any()) } returns SavedCardResponse.Success(
            paymentResponse
        )

        sut.authorize(paymentUrl, authUrl, "123")

        assertTrue(effects.last() is SavedCardPaymentsVMEffects.InitiateThreeDSTwo)
    }

    @Test
    fun `doSavedCardPayment should show visa plans if available`() = runTest {
        val effects: MutableList<SavedCardPaymentsVMEffects> = mutableListOf()
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        val orderUrl = "orderUrl"
        val paymentCookie = "paymentCookie"
        val orderAmount = OrderAmount(1000.0, "USD")

        val paymentResponse = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureTwoResponse.json").readText(),
            PaymentResponse::class.java
        )
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )
        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(Companion.paymentCookie, accessTokenCookie), "orderUrl")
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse
        coEvery {
            threeDSecureFactory.buildThreeDSecureDto(any())
        } returns ThreeDSecureDto("", "", "", "")
        coEvery { getPayerIpInteractor.getPayerIp(any()) } returns "1.1.1.1"
        coEvery { savedCardPaymentApiInteractor.doSavedCardPayment(any()) } returns SavedCardResponse.Success(
            paymentResponse
        )

        sut.doSavedCardPayment(orderUrl, paymentCookie, orderAmount, savedCardPaymentRequest, visaPlans = mockVisaPlans)

        assertTrue(states.last() is SavedCardPaymentState.ShowVisaPlans)
    }

    @Test
    fun `doSavedCardPayment should initiate payment if no visa plans are available`() = runTest {
        val effects: MutableList<SavedCardPaymentsVMEffects> = mutableListOf()
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        val orderUrl = "orderUrl"
        val paymentCookie = "paymentCookie"
        val orderAmount = OrderAmount(1000.0, "USD")

        val paymentResponse = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureTwoResponse.json").readText(),
            PaymentResponse::class.java
        )
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )
        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(Companion.paymentCookie, accessTokenCookie), "orderUrl")
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse
        coEvery {
            threeDSecureFactory.buildThreeDSecureDto(any())
        } returns ThreeDSecureDto("", "", "", "")
        coEvery { getPayerIpInteractor.getPayerIp(any()) } returns "1.1.1.1"
        coEvery { savedCardPaymentApiInteractor.doSavedCardPayment(any()) } returns SavedCardResponse.Success(
            paymentResponse
        )

        sut.doSavedCardPayment(orderUrl, paymentCookie, orderAmount, savedCardPaymentRequest, visaPlans = null)
        assertTrue(effects.last() is SavedCardPaymentsVMEffects.InitiateThreeDSTwo)
    }

    @Test
    fun `doSavedCardPayment should emit error effect if payment fails`() = runTest {
        val effects: MutableList<SavedCardPaymentsVMEffects> = mutableListOf()
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.effect.toList(effects)
        }

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        val orderUrl = "orderUrl"
        val paymentCookie = "paymentCookie"
        val orderAmount = OrderAmount(1000.0, "USD")

        val paymentResponse = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureTwoResponse.json").readText(),
            PaymentResponse::class.java
        )
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )
        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(Companion.paymentCookie, accessTokenCookie), "orderUrl")
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns orderResponse
        coEvery {
            threeDSecureFactory.buildThreeDSecureDto(any())
        } returns ThreeDSecureDto("", "", "", "")
        coEvery { getPayerIpInteractor.getPayerIp(any()) } returns "1.1.1.1"
        coEvery { savedCardPaymentApiInteractor.doSavedCardPayment(any()) } returns SavedCardResponse.Error(IllegalArgumentException())

        // When
        sut.doSavedCardPayment(orderUrl, paymentCookie, orderAmount, savedCardPaymentRequest, visaPlans = null)

        assertTrue(effects.last() is SavedCardPaymentsVMEffects.Failed)
    }

    companion object {
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