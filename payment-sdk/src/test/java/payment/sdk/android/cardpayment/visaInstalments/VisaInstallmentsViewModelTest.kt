package payment.sdk.android.cardpayment.visaInstalments

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
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
import org.junit.Rule
import org.junit.Test
import payment.sdk.android.cardpayment.savedCard.SavedCardDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto
import payment.sdk.android.cardpayment.visaInstalments.model.InstallmentPlan
import payment.sdk.android.cardpayment.visaInstalments.model.NewCardDto
import payment.sdk.android.cardpayment.visaInstalments.model.PlanFrequency
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstalmentActivityArgs
import payment.sdk.android.cardpayment.visaInstalments.model.VisaInstallmentsVMState
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.TermsAndCondition
import payment.sdk.android.core.interactor.CardPaymentInteractor
import payment.sdk.android.core.interactor.CardPaymentResponse
import payment.sdk.android.core.interactor.GetPayerIpInteractor
import payment.sdk.android.core.interactor.SavedCardPaymentApiInteractor
import payment.sdk.android.core.interactor.VisaRequest
import java.lang.Exception

@OptIn(ExperimentalCoroutinesApi::class)
class VisaInstallmentsViewModelTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val cardPaymentInteractor: CardPaymentInteractor = mockk(relaxed = true)
    private val savedCardPaymentApiInteractor: SavedCardPaymentApiInteractor = mockk(relaxed = true)
    private val threeDSecureFactory: ThreeDSecureFactory = mockk(relaxed = true)
    private val getPayerIpInteractor: GetPayerIpInteractor = mockk(relaxed = true)

    private lateinit var sut: VisaInstallmentsViewModel

    private val savedCard = SavedCardDto(
        cardholderName = "",
        cardToken = "",
        recaptureCsc = false,
        expiry = "",
        maskedPan = "",
        scheme = ""
    )

    private val newCardDto = NewCardDto(
        cardNumber = "",
        customerName = "",
        cvv = "",
        expiry = ""
    )

    private val dummyPlans = listOf(
        InstallmentPlan(
            id = "10",
            currency = "AED",
            amount = "64",
            monthlyRate = "718",
            totalUpFrontFees = "15",
            numberOfInstallments = 0,
            frequency = PlanFrequency.PayInFull,
            terms = null
        ),
        InstallmentPlan(
            id = "11",
            currency = "AED",
            amount = "64",
            monthlyRate = "718",
            totalUpFrontFees = "15",
            numberOfInstallments = 3,
            frequency = PlanFrequency.MONTHLY,
            terms = TermsAndCondition(
                languageCode = "en",
                text = "terms",
                url = "xyz",
                version = 1
            )
        ),
        InstallmentPlan(
            id = "12",
            currency = "AED",
            amount = "64",
            monthlyRate = "718",
            totalUpFrontFees = "15",
            numberOfInstallments = 6,
            frequency = PlanFrequency.MONTHLY,
            terms = TermsAndCondition(
                languageCode = "en",
                text = "terms",
                url = "xyz",
                version = 1
            )
        ),
        InstallmentPlan(
            id = "13",
            currency = "AED",
            amount = "64",
            monthlyRate = "718",
            totalUpFrontFees = "15",
            numberOfInstallments = 12,
            frequency = PlanFrequency.MONTHLY,
            terms = TermsAndCondition(
                languageCode = "en",
                text = "terms",
                url = "xyz",
                version = 1
            )
        )
    )

    private val dummyPlanSelectionState = VisaInstallmentsVMState.PlanSelection(
        installmentPlans = dummyPlans,
        selectedPlan = null,
        savedCardDto = null,
        isValid = false,
        paymentUrl = null,
        paymentCookie = "",
        savedCardUrl = null,
        orderUrl = "",
        newCardDto = null
    )
    private val payPageUrl = "https://paypage.sandbox.ngenius-payments.com/?code=323eas"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = VisaInstallmentsViewModel(
            cardPaymentInteractor,
            savedCardPaymentApiInteractor,
            getPayerIpInteractor,
            threeDSecureFactory,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init`() = runTest {
        val states: MutableList<VisaInstallmentsVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        val visaInstalmentsActivityArgs = VisaInstalmentActivityArgs(
            payPageUrl = "",
            paymentUrl = "",
            paymentCookie = "",
            savedCard = savedCard,
            savedCardUrl = "",
            orderUrl = "",
            instalmentPlan = listOf(),
            newCard = NewCardDto(cardNumber = "", expiry = "", cvv = "", customerName = "")
        )

        sut.init(visaInstalmentsActivityArgs)

        assertTrue(states[0] is VisaInstallmentsVMState.Init)
        assertTrue(states[1] is VisaInstallmentsVMState.PlanSelection)
    }

    @Test
    fun `test selection invalid when plan is selected and terms is accepted`() = runTest {
        val states: MutableList<VisaInstallmentsVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        val selectedPlan = dummyPlans[2]
        sut.onSelectPlan(
            selectedPlan = selectedPlan.copy(termsAccepted = true),
            dummyPlanSelectionState
        )

        assertTrue(states[0] is VisaInstallmentsVMState.Init)
        assertTrue(states[1] is VisaInstallmentsVMState.PlanSelection)

        val result = (states[1] as VisaInstallmentsVMState.PlanSelection)

        assertTrue(result.isValid)
    }

    @Test
    fun `test selection invalid when plan is selected but terms not selected`() = runTest {
        val states: MutableList<VisaInstallmentsVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        val selectedPlan = dummyPlans[2]

        sut.onSelectPlan(
            selectedPlan = selectedPlan.copy(termsAccepted = false),
            dummyPlanSelectionState
        )

        assertTrue(states[0] is VisaInstallmentsVMState.Init)
        assertTrue(states[1] is VisaInstallmentsVMState.PlanSelection)

        val result = (states[1] as VisaInstallmentsVMState.PlanSelection)

        assertFalse(result.isValid)
    }

    @Test
    fun `make payment by card when pay in full selected`() = runTest {
        val response = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureTwoResponse.json").readText(),
            PaymentResponse::class.java
        )
        val states: MutableList<VisaInstallmentsVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        val instalmentPlan = InstallmentPlan(
            id = "12",
            currency = "AED",
            amount = "64",
            monthlyRate = "718",
            totalUpFrontFees = "15",
            numberOfInstallments = 6,
            frequency = PlanFrequency.PayInFull,
            terms = null
        )

        coEvery {
            getPayerIpInteractor.getPayerIp(any())
        } returns "1.1.1.1"

        coEvery {
            threeDSecureFactory.buildThreeDSecureTwoDto(any(), any(), any())
        } returns ThreeDSecureTwoDto(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        )

        coEvery {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns CardPaymentResponse.Success(response)

        sut.makeCardPayment(
            instalmentPlan,
            dummyPlanSelectionState.copy(paymentUrl = "paymentUrl", newCardDto = newCardDto),
            payPageUrl,
            "123"
        )

        assertTrue(states[0] is VisaInstallmentsVMState.Init)
        assertTrue(states[1] is VisaInstallmentsVMState.Loading)
        assertTrue(states[2] is VisaInstallmentsVMState.InitiateThreeDSTwo)

        coVerify { getPayerIpInteractor.getPayerIp(any()) }

        coVerify {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                null
            )
        }
    }

    @Test
    fun `make payment by card when instalment plan is selected`() = runTest {
        val response = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureTwoResponse.json").readText(),
            PaymentResponse::class.java
        )
        val states: MutableList<VisaInstallmentsVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        val instalmentPlan = InstallmentPlan(
            id = "12",
            currency = "AED",
            amount = "64",
            monthlyRate = "718",
            totalUpFrontFees = "15",
            numberOfInstallments = 6,
            frequency = PlanFrequency.MONTHLY,
            terms = TermsAndCondition(
                languageCode = "en",
                text = "terms",
                url = "xyz",
                version = 3
            )
        )

        coEvery {
            getPayerIpInteractor.getPayerIp(any())
        } returns "1.1.1.1"

        coEvery {
            threeDSecureFactory.buildThreeDSecureTwoDto(any(), any(), any())
        } returns ThreeDSecureTwoDto(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        )

        coEvery {
            cardPaymentInteractor.makeCardPayment(
                any(),
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
            instalmentPlan,
            dummyPlanSelectionState.copy(paymentUrl = "paymentUrl", newCardDto = newCardDto),
            payPageUrl,
            "123"
        )

        assertTrue(states[0] is VisaInstallmentsVMState.Init)
        assertTrue(states[1] is VisaInstallmentsVMState.Loading)
        assertTrue(states[2] is VisaInstallmentsVMState.InitiateThreeDSTwo)

        coVerify { getPayerIpInteractor.getPayerIp(any()) }

        coVerify {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                VisaRequest(planSelectionIndicator = true, acceptedTAndCVersion = 3, vPlanId = "12")
            )
        }
    }

    @Test
    fun `make payment fails if API return failure`() = runTest {
        val states: MutableList<VisaInstallmentsVMState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        val instalmentPlan = InstallmentPlan(
            id = "12",
            currency = "AED",
            amount = "64",
            monthlyRate = "718",
            totalUpFrontFees = "15",
            numberOfInstallments = 6,
            frequency = PlanFrequency.PayInFull,
            terms = null
        )

        coEvery {
            getPayerIpInteractor.getPayerIp(any())
        } returns "1.1.1.1"

        coEvery {
            cardPaymentInteractor.makeCardPayment(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                null
            )
        } returns CardPaymentResponse.Error(Exception("error"))

        sut.makeCardPayment(
            instalmentPlan,
            dummyPlanSelectionState.copy(paymentUrl = "paymentUrl", newCardDto = newCardDto),
            payPageUrl,
            "123"
        )

        assertTrue(states[0] is VisaInstallmentsVMState.Init)
        assertTrue(states[1] is VisaInstallmentsVMState.Loading)
        assertTrue(states[2] is VisaInstallmentsVMState.Failed)

        coVerify(exactly = 1) { getPayerIpInteractor.getPayerIp(any()) }
    }
}