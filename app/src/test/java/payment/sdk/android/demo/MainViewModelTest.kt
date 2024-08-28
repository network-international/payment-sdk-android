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
import payment.sdk.android.core.Order
import payment.sdk.android.core.SavedCard
import payment.sdk.android.demo.data.DataStore
import payment.sdk.android.demo.http.CreateOrderApiInteractor
import payment.sdk.android.demo.http.GetOrderApiInteractor
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentType
import payment.sdk.android.demo.model.OrderRequest
import payment.sdk.android.demo.model.PaymentOrderAmount
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
        sut.createOrder(PaymentType.CARD, orderRequest = OrderRequest(
            action = "SALE",
            amount = PaymentOrderAmount(
                value = 100.00,
                currencyCode = "AED"
            ),
            language = "en",
            merchantAttributes = emptyMap(),
        ))

        assertEquals(states[0].state, MainViewModelStateType.INIT)
        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[1].message, "Creating Order...")
        assertEquals(states[2].state, MainViewModelStateType.PAYMENT_PROCESSING)
        assertEquals(states[2].orderReference, orderResponse.reference)
        assertEquals(states[2].selectedProducts, emptyList<Product>())
        assertEquals(states[2].total, 0.0)
    }

    @Test
    fun `test createOrder if selected environment is not set`() = runTest {

        val states: MutableList<MainViewModelState> = mutableListOf()

        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }

        coEvery { dataStore.getSelectedEnvironment() } returns null

        sut.createOrder(PaymentType.CARD, orderRequest = OrderRequest(
            action = "SALE",
            amount = PaymentOrderAmount(
                value = 100.00,
                currencyCode = "AED"
            ),
            language = "en",
            merchantAttributes = emptyMap(),
        ))

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

        sut.createOrder(PaymentType.CARD, OrderRequest(action = "SALE",
            amount = PaymentOrderAmount(
                value = 100.00,
                currencyCode = "AED"
            ),
            language = "en",
            merchantAttributes = emptyMap()))

        assertEquals(states[0].state, MainViewModelStateType.INIT
        )
        assertEquals(states[1].state, MainViewModelStateType.LOADING)
        assertEquals(states[2].state, MainViewModelStateType.ERROR)
        assertEquals(states[2].message, "failed")
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