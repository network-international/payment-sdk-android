package payment.sdk.android.cardpayment.googlePay

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
import payment.sdk.android.core.Order
import payment.sdk.android.core.api.SDKHttpResponse
import payment.sdk.android.core.interactor.AuthApiInteractor
import payment.sdk.android.core.interactor.AuthResponse
import payment.sdk.android.core.interactor.GetOrderApiInteractor
import payment.sdk.android.core.interactor.GooglePayAcceptInteractor
import payment.sdk.android.googlepay.GooglePayConfig
import payment.sdk.android.googlepay.GooglePayConfigFactory
import payment.sdk.android.googlepay.GooglePayLauncher
import payment.sdk.android.googlepay.GooglePayVMEffects
import payment.sdk.android.googlepay.GooglePayViewModel
import payment.sdk.android.payments.GooglePayUiConfig

@OptIn(ExperimentalCoroutinesApi::class)
class GooglePayViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val authApiInteractor: AuthApiInteractor = mockk(relaxed = true)
    private val getOrderApiInteractor: GetOrderApiInteractor = mockk(relaxed = true)
    private val googlePayConfigFactory: GooglePayConfigFactory = mockk(relaxed = true)
    private val googlePayAcceptInteractor: GooglePayAcceptInteractor = mockk(relaxed = true)

    private lateinit var sut: GooglePayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = viewModel(
            GooglePayLauncher.Config(
                gatewayAuthorizationUrl = "https://auth.example",
                payPageUrl = "https://pay.example/?code=authCode",
                googlePayConfig = GooglePayConfig(
                    environment = GooglePayConfig.Environment.Test,
                    merchantGatewayId = "gateway-id"
                )
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start fails when auth code is missing`() = runTest {
        sut = viewModel(
            GooglePayLauncher.Config(
                gatewayAuthorizationUrl = "https://auth.example",
                payPageUrl = "https://pay.example/",
                googlePayConfig = GooglePayConfig(
                    environment = GooglePayConfig.Environment.Test,
                    merchantGatewayId = "gateway-id"
                )
            )
        )
        val effects = collectEffects()

        sut.start()

        val finished = effects.first() as GooglePayVMEffects.Finished
        assertTrue(finished.result is GooglePayLauncher.Result.Failed)
        assertEquals(
            "Authorization code is missing from the pay page URL",
            (finished.result as GooglePayLauncher.Result.Failed).error
        )
    }

    @Test
    fun `start fails when order has no google pay`() = runTest {
        val effects = collectEffects()
        val order = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )
        order.paymentMethods?.wallet = arrayOf("SAMSUNG_PAY")
        coEvery { authApiInteractor.authenticate(any(), any()) } returns AuthResponse.Success(
            listOf(PAYMENT_TOKEN_COOKIE, ACCESS_TOKEN_COOKIE), "orderUrl"
        )
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns order

        sut.start()

        val finished = effects.first() as GooglePayVMEffects.Finished
        assertEquals(
            "Google Pay is not enabled on this order",
            (finished.result as GooglePayLauncher.Result.Failed).error
        )
    }

    @Test
    fun `start fails when google pay is not available on device`() = runTest {
        val effects = collectEffects()
        authorizeOrder()
        coEvery {
            googlePayConfigFactory.checkGooglePayConfig(any(), any(), any(), any(), any())
        } returns GooglePayUiConfig(
            allowedPaymentMethods = "",
            canUseGooglePay = false,
            googlePayAcceptUrl = "https://accept",
            paymentsClient = mockk(relaxed = true),
            paymentDataRequest = mockk(relaxed = true)
        )

        sut.start()

        val finished = effects.first() as GooglePayVMEffects.Finished
        assertEquals(
            "Google Pay is not available on this device",
            (finished.result as GooglePayLauncher.Result.Failed).error
        )
    }

    @Test
    fun `start launches google pay sheet when ready`() = runTest {
        val effects = collectEffects()
        authorizeOrder()
        val uiConfig = readyUiConfig()
        coEvery {
            googlePayConfigFactory.checkGooglePayConfig(any(), any(), any(), any(), any())
        } returns uiConfig

        sut.start()

        assertTrue(effects.first() is GooglePayVMEffects.LaunchSheet)
    }

    @Test
    fun `acceptGooglePay maps authorised captured and review`() = runTest {
        authorizeAndPrepareSheet()

        assertMappedState("AUTHORISED", GooglePayLauncher.Result.Authorised)
        assertMappedState("CAPTURED", GooglePayLauncher.Result.Captured)
        assertMappedState("POST_AUTH_REVIEW", GooglePayLauncher.Result.PostAuthReview)
        assertMappedState("PURCHASED", GooglePayLauncher.Result.Success)
    }

    @Test
    fun `acceptGooglePay reports api failure`() = runTest {
        authorizeAndPrepareSheet()
        val effects = collectEffects()
        coEvery {
            googlePayAcceptInteractor.accept(any(), any(), any())
        } returns SDKHttpResponse.Failed(Exception("502"))

        sut.acceptGooglePay("token")

        val finished = effects.filterIsInstance<GooglePayVMEffects.Finished>().last()
        assertEquals(
            "Google Pay accept failed: 502",
            (finished.result as GooglePayLauncher.Result.Failed).error
        )
    }

    @Test
    fun `onUserCancelled emits cancelled`() = runTest {
        val effects = collectEffects()

        sut.onUserCancelled()

        assertEquals(
            GooglePayLauncher.Result.Cancelled,
            (effects.first() as GooglePayVMEffects.Finished).result
        )
    }

    private fun assertMappedState(state: String, expected: GooglePayLauncher.Result) {
        val effects: MutableList<GooglePayVMEffects> = mutableListOf()
        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            sut.effect.toList(effects)
        }
        coEvery {
            googlePayAcceptInteractor.accept(any(), any(), any())
        } returns SDKHttpResponse.Success(emptyMap(), """{"state":"$state"}""")
        sut.acceptGooglePay("token")
        val finished = effects.filterIsInstance<GooglePayVMEffects.Finished>().last()
        assertEquals(expected, finished.result)
        job.cancel()
    }

    private fun authorizeAndPrepareSheet() {
        authorizeOrder()
        coEvery {
            googlePayConfigFactory.checkGooglePayConfig(any(), any(), any(), any(), any())
        } returns readyUiConfig()
        sut.start()
    }

    private fun authorizeOrder() {
        val order = Gson().fromJson(
            ClassLoader.getSystemResource("orderResponse.json").readText(),
            Order::class.java
        )
        coEvery { authApiInteractor.authenticate(any(), any()) } returns AuthResponse.Success(
            listOf(PAYMENT_TOKEN_COOKIE, ACCESS_TOKEN_COOKIE), "orderUrl"
        )
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns order
    }

    private fun readyUiConfig() = GooglePayUiConfig(
        allowedPaymentMethods = "",
        canUseGooglePay = true,
        googlePayAcceptUrl = "https://example.com/google-pay/accept",
        paymentsClient = mockk(relaxed = true),
        paymentDataRequest = mockk(relaxed = true)
    )

    private fun collectEffects(): MutableList<GooglePayVMEffects> {
        val effects: MutableList<GooglePayVMEffects> = mutableListOf()
        kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            sut.effect.toList(effects)
        }
        return effects
    }

    private fun viewModel(config: GooglePayLauncher.Config) = GooglePayViewModel(
        config = config,
        authApiInteractor = authApiInteractor,
        getOrderApiInteractor = getOrderApiInteractor,
        googlePayConfigFactory = googlePayConfigFactory,
        googlePayAcceptInteractor = googlePayAcceptInteractor,
        dispatcher = testDispatcher
    )

    companion object {
        private const val ACCESS_TOKEN_COOKIE = "${AuthResponse.ACCESS_TOKEN}=randomToken;secure;Httponly"
        private const val PAYMENT_TOKEN_COOKIE = "${AuthResponse.PAYMENT_TOKEN}=somepaytoken;secure;Httponly"
    }
}
