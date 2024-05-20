package payment.sdk.android.demo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Rule
import org.junit.Test
import payment.sdk.android.PaymentClient
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.core.Order
import payment.sdk.android.core.SavedCard
import payment.sdk.android.demo.data.DataStore
import payment.sdk.android.demo.http.CreateOrderApiInteractor
import payment.sdk.android.demo.http.GetOrderApiInteractor
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentType
import payment.sdk.android.demo.model.Product

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var sut: MainViewModel

    private val paymentClient: PaymentClient = mockk(relaxed = true)
    private val createOrderApiInteractor: CreateOrderApiInteractor = mockk(relaxed = true)
    private val getOrderApiInteractor: GetOrderApiInteractor = mockk(relaxed = true)
    private val dataStore: DataStore = mockk(relaxed = true)

    private val environment = Environment(
        type = EnvironmentType.DEV,
        id = "some",
        name = "test",
        apiKey = "key",
        outletReference = "ref",
        realm = "name"
    )

    private val savedCard = SavedCard(
        cardholderName = "",
        scheme = "",
        expiry = "",
        cardToken = "",
        recaptureCsc = true,
        maskedPan = ""
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = MainViewModel(
            paymentClient,
            createOrderApiInteractor,
            getOrderApiInteractor,
            dataStore,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test on select product`() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getProducts() } returns listOf()
        val product = Product(id = "1", name = "name", amount = 10.99, isLocal = true)
        sut.onAddProduct(product)

        coVerify { dataStore.addProduct(product) }
    }

    @Test
    fun `test onPayByCard successful payment`() = runTest {
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("order_response.json").readText(),
            Order::class.java
        )

        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getSelectedEnvironment() } returns environment
        coEvery { dataStore.getOrderAction() } returns "SALE"
        coEvery { dataStore.getMerchantAttributes() } returns listOf()
        coEvery { createOrderApiInteractor.createOrder(any(), any()) } returns Result.Success(
            orderResponse
        )
        coEvery { paymentClient.launchCardPayment(any(), any()) } returns Unit

        sut.onPayByCard()

        assertEquals(states[0].state, MainViewModelStateType.INIT)
        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[1].message, "Creating Order...")
        assertEquals(states[2].state, MainViewModelStateType.INIT)
        assertEquals(states[2].orderReference, orderResponse.reference)
        assertEquals(states[2].selectedProducts, emptyList<Product>())
        assertEquals(states[2].total, 0.0)
    }

    @Test
    fun `test onPayByCard if selected environment is not set`() = runTest {

        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getSelectedEnvironment() } returns null

        sut.onPayByCard()

        assertEquals(states[0].state, MainViewModelStateType.INIT)
        assertEquals(states[1].state, MainViewModelStateType.ERROR)
        assertEquals(states[1].message, "No environment selected")
    }

    @Test
    fun `test onPayByCard if getAccessToken failed`() = runTest {

        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getSelectedEnvironment() } returns environment
        coEvery {
            createOrderApiInteractor.createOrder(
                any(),
                any()
            )
        } returns Result.Error("failed")

        sut.onPayByCard()

        assertEquals(states[0].state, MainViewModelStateType.INIT)
        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[2].state, MainViewModelStateType.ERROR)
        assertEquals(states[2].message, "failed")
    }

    @Test
    fun `show error onCardPaymentResponse if payment is successful but no environment selected`() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getSelectedEnvironment() } returns null

        sut.onCardPaymentResponse(CardPaymentData(code = CardPaymentData.STATUS_PAYMENT_CAPTURED))

        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[2].state, MainViewModelStateType.ERROR)
        assertEquals(states[2].message, "No environment selected")
    }

    @Test
    fun `show success onCardPaymentResponse if payment is successful with environment selected`() = runTest {
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("order_response.json").readText(),
            Order::class.java
        )
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getSelectedEnvironment() } returns environment
        coEvery { dataStore.getSavedCards() } returns emptyList()
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns Result.Success(orderResponse)

        sut.onCardPaymentResponse(CardPaymentData(code = CardPaymentData.STATUS_PAYMENT_CAPTURED))

        coVerify(exactly = 1) { dataStore.saveCard(any()) }
        coVerify(exactly = 1) { dataStore.setSavedCard(any()) }

        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[2].state, MainViewModelStateType.PAYMENT_SUCCESS)
        assertEquals(states[2].message, "Payment Successful")
        assertEquals(states[2].savedCards, emptyList<SavedCard>())
    }

    @Test
    fun `show Success onCardPaymentResponse if get order returns error`() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getSelectedEnvironment() } returns environment
        coEvery { dataStore.getSavedCards() } returns emptyList()
        coEvery { getOrderApiInteractor.getOrder(any(), any()) } returns Result.Error("error")

        sut.onCardPaymentResponse(CardPaymentData(code = CardPaymentData.STATUS_PAYMENT_CAPTURED))

        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[2].state, MainViewModelStateType.PAYMENT_SUCCESS)
    }

    @Test
    fun `test onCardPaymentResponse is non success calls`() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        sut.onCardPaymentResponse(CardPaymentData(code = CardPaymentData.STATUS_PAYMENT_FAILED))

        assertEquals(states.last().state, MainViewModelStateType.PAYMENT_FAILED)


        sut.onCardPaymentResponse(CardPaymentData(code = CardPaymentData.STATUS_POST_AUTH_REVIEW))

        assertEquals(states.last().state, MainViewModelStateType.PAYMENT_POST_AUTH_REVIEW)

        sut.onCardPaymentResponse(CardPaymentData(code = 100))

        assertEquals(states.last().state, MainViewModelStateType.PAYMENT_FAILED)
    }

    @Test
    fun `onSavedCardPayment successful`() = runTest {
        val orderResponse = Gson().fromJson(
            ClassLoader.getSystemResource("order_response.json").readText(),
            Order::class.java
        )
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { createOrderApiInteractor.createOrder(any(), any()) } returns Result.Success(orderResponse)
        coEvery { dataStore.getSelectedEnvironment() } returns environment

        sut.onPayBySavedCard(savedCard)

        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[2].state, MainViewModelStateType.INIT)
        assertEquals(states[2].selectedProducts, emptyList<Product>())
        assertEquals(states[2].total, 0.0)
    }

    @Test
    fun `onSavedCardPayment environment not selected`() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getSelectedEnvironment() } returns null

        sut.onPayBySavedCard(savedCard)

        assertEquals(states[1].state, MainViewModelStateType.ERROR)
        assertEquals(states[1].message, "No environment selected")
    }

    @Test
    fun `onSavedCardPayment error while creating order`() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { createOrderApiInteractor.createOrder(any(), any()) } returns Result.Error("error")
        coEvery { dataStore.getSelectedEnvironment() } returns environment

        sut.onPayBySavedCard(savedCard)

        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[2].state, MainViewModelStateType.ERROR)
        assertEquals(states[2].message, "error")
//        assertEquals(states[2].selectedProducts, emptyList<Product>())
//        assertEquals(states[2].total, 0.0)
    }

    @Test
    fun onCardPaymentCancelledTest() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        sut.onCardPaymentCancelled()
        assertEquals(states[1].state, MainViewModelStateType.PAYMENT_CANCELLED)
    }

    @Test
    fun closeDialogTest() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        sut.closeDialog()
        assertEquals(states[0].state, MainViewModelStateType.INIT)
    }

    @Test
    fun onDeleteProductTest() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getProducts() } returns listOf()
        val product = Product(id = "1", name = "name", amount = 10.99, isLocal = true)
        sut.onDeleteProduct(product)

        coVerify { dataStore.getProducts() }
        coVerify { dataStore.deleteProduct(product) }
    }

    @Test
    fun deleteSavedCardTest() = runTest {
        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getProducts() } returns listOf()
        sut.deleteSavedCard(savedCard)

        coVerify { dataStore.deleteSavedCard(savedCard) }
        coVerify { dataStore.getSavedCards() }
    }
}