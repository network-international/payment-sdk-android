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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureDto
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureFactory
import payment.sdk.android.cardpayment.threedsecuretwo.ThreeDSecureTwoDto
import payment.sdk.android.core.PaymentResponse
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.SavedCardPaymentApiInteractor
import payment.sdk.android.core.interactor.SavedCardResponse

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCardViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val authApiInteractor: AuthApiInteractor = mockk(relaxed = true)
    private val savedCardPaymentApiInteractor: SavedCardPaymentApiInteractor = mockk(relaxed = true)
    private val threeDSecureFactory: ThreeDSecureFactory = mockk(relaxed = true)

    private lateinit var sut: SavedPaymentViewModel

    private val savedCard = SavedCardDto(
        cardholderName = "",
        cardToken = "",
        recaptureCsc = false,
        expiry = "",
        maskedPan = "",
        scheme = ""
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = SavedPaymentViewModel(
            authApiInteractor,
            savedCardPaymentApiInteractor,
            threeDSecureFactory,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `authorize state is Error when auth code in payment url not found`() = runTest {
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        sut.authorize(authUrl = "blank", paymentUrl = "", false, null)

        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Failed)
    }

    @Test
    fun `authorize state is error when AuthApiInteractor return failed`() = runTest {
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Error(Exception("error"))

        sut.authorize(authUrl = authUrl, paymentUrl = paymentUrl, false, null)

        coVerify(exactly = 1) { authApiInteractor.authenticate(authUrl, authCode) }

        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Loading)
        assertTrue(states[2] is SavedCardPaymentState.Failed)
    }

    @Test
    fun `authorize state is Authorized when AuthApiInteractor return success and cvv is null`() = runTest {
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(paymentCookie, accessTokenCookie), "orderUrl")

        sut.authorize(authUrl = authUrl, paymentUrl = paymentUrl, false, null)

        coVerify(exactly = 1) { authApiInteractor.authenticate(authUrl, authCode) }


        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Loading)
        assertTrue(states[2] is SavedCardPaymentState.Authorized)
    }

    @Test
    fun `authorize state is CaptureCvv when AuthApiInteractor return success and cvv is null`() = runTest {
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(paymentCookie, accessTokenCookie), "orderUrl")

        sut.authorize(authUrl = authUrl, paymentUrl = paymentUrl, true, null)

        coVerify(exactly = 1) { authApiInteractor.authenticate(authUrl, authCode) }

        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Loading)
        assertTrue(states[2] is SavedCardPaymentState.CaptureCvv)
    }

    @Test
    fun `authorize state is Authorized when AuthApiInteractor return success and cvv is not null`() = runTest {
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        val cvv = "123"

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            authApiInteractor.authenticate(any(), any())
        } returns AuthResponse.Success(listOf(paymentCookie, accessTokenCookie), "orderUrl")

        sut.authorize(authUrl = authUrl, paymentUrl = paymentUrl, true, cvv)

        coVerify(exactly = 1) { authApiInteractor.authenticate(authUrl, authCode) }

        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Loading)
        assertTrue(states[2] is SavedCardPaymentState.Authorized)
        val resultCvv = (states[2] as SavedCardPaymentState.Authorized).cvv
        assertEquals(cvv, resultCvv)
    }

    @Test
    fun `doSavedCardPayment state is InitiateThreeDSTwo when success`() = runTest {
        val response = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureTwoResponse.json").readText(),
            PaymentResponse::class.java
        )

        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            savedCardPaymentApiInteractor.doSavedCardPayment(
                any(),
                any(),
                any(),
                any()
            )
        } returns SavedCardResponse.Success(response)

        coEvery {
            threeDSecureFactory.buildThreeDSecureDto(any())
        } returns ThreeDSecureDto("", "", "", "")

        val savedCard = SavedCardDto(
            cardholderName = "",
            cardToken = "",
            recaptureCsc = false,
            expiry = "",
            maskedPan = "",
            scheme = ""
        )

        sut.doSavedCardPayment(
            accessToken,
            "saved card",
            savedCard,
            "123",
            "order Url",
            paymentCookie
        )

        coVerify(exactly = 1) {
            savedCardPaymentApiInteractor.doSavedCardPayment(
                accessToken,
                "saved card",
                savedCard.toSavedCard(),
                "123"
            )
        }

        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Loading)
    }


    @Test
    fun `doSavedCardPayment state is InitiateThreeDS when success`() = runTest {
        val response = Gson().fromJson(
            ClassLoader.getSystemResource("threeDSecureResponse.json").readText(),
            PaymentResponse::class.java
        )

        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery {
            savedCardPaymentApiInteractor.doSavedCardPayment(any(), any(), any(), any())
        } returns SavedCardResponse.Success(response)

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

        val savedCard = SavedCardDto(
            cardholderName = "",
            cardToken = "",
            recaptureCsc = false,
            expiry = "",
            maskedPan = "",
            scheme = ""
        )

        sut.doSavedCardPayment(
            accessToken,
            "saved card",
            savedCard,
            "123",
            "order Url",
            paymentCookie
        )

        coVerify(exactly = 1) {
            savedCardPaymentApiInteractor.doSavedCardPayment(
                accessToken,
                "saved card",
                savedCard.toSavedCard(),
                "123"
            )
        }

        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Loading)
    }

    @Test
    fun `state is error when SavedCardPaymentApiInteractor returns failed`() = runTest {

        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        coEvery {
            savedCardPaymentApiInteractor.doSavedCardPayment(any(), any(), any(), any())
        } returns SavedCardResponse.Error(Exception("error"))

        sut.doSavedCardPayment(accessToken, "saved card", savedCard, "123", "", "")

        coVerify(exactly = 1) {
            savedCardPaymentApiInteractor.doSavedCardPayment(
                accessToken,
                "saved card",
                savedCard.toSavedCard(),
                "123"
            )
        }

        assertTrue(states[1] is SavedCardPaymentState.Loading)
    }

    @Test
    fun `is is captured when SavedCardPaymentApiInteractor returns state captured`() = runTest {
        val response = Gson().fromJson(
            ClassLoader.getSystemResource("paymentResponse.json").readText(),
            PaymentResponse::class.java
        )
        val states: MutableList<SavedCardPaymentState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        coEvery {
            savedCardPaymentApiInteractor.doSavedCardPayment(any(), any(), any(), any())
        } returns SavedCardResponse.Success(response)

        coEvery {
            threeDSecureFactory.buildThreeDSecureDto(any())
        } throws IllegalArgumentException("argument not found")


        sut.doSavedCardPayment(accessToken, "saved card", savedCard, "123", "", "")

        coVerify(exactly = 1) {
            savedCardPaymentApiInteractor.doSavedCardPayment(
                accessToken,
                "saved card",
                savedCard.toSavedCard(),
                "123"
            )
        }

        assertTrue(states[0] is SavedCardPaymentState.Init)
        assertTrue(states[1] is SavedCardPaymentState.Loading)
        assertTrue(states[2] is SavedCardPaymentState.Captured)
    }

    @Test
    fun `state is error when SavedCardPaymentApiInteractor returns failed when IllegalArgumentException`() =
        runTest {
            val response = Gson().fromJson(
                ClassLoader.getSystemResource("threeDSecureResponse.json").readText(),
                PaymentResponse::class.java
            )

            val states: MutableList<SavedCardPaymentState> = mutableListOf()

            backgroundScope.launch(testDispatcher) {
                sut.state.toList(states)
            }
            coEvery {
                savedCardPaymentApiInteractor.doSavedCardPayment(any(), any(), any(), any())
            } returns SavedCardResponse.Success(response)

            coEvery {
                threeDSecureFactory.buildThreeDSecureDto(any())
            } throws IllegalArgumentException("argument not found")


            sut.doSavedCardPayment(accessToken, "saved card", savedCard, "123", "", "")

            coVerify(exactly = 1) {
                savedCardPaymentApiInteractor.doSavedCardPayment(
                    accessToken,
                    "saved card",
                    savedCard.toSavedCard(),
                    "123"
                )
            }

            assertTrue(states[0] is SavedCardPaymentState.Init)
            assertTrue(states[1] is SavedCardPaymentState.Loading)
            assertTrue(states[2] is SavedCardPaymentState.Failed)
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