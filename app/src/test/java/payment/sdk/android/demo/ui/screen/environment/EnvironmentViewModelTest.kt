package payment.sdk.android.demo.ui.screen.environment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import payment.sdk.android.demo.data.DataStore
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentType
import payment.sdk.android.demo.model.MerchantAttribute

@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentViewModelTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dataStore: DataStore = mockk(relaxed = true)

    private lateinit var sut: EnvironmentViewModel
    private val environment = Environment(
        type = EnvironmentType.DEV,
        id = "some",
        name = "test",
        apiKey = "key",
        outletReference = "ref",
        realm = "name"
    )

    private val merchantAttribute = MerchantAttribute(key = "key", value = "value")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = EnvironmentViewModel(dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test saveMerchantAttribute`() = runTest {
        val states: MutableList<EnvironmentViewModelState> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }
        coEvery { dataStore.getMerchantAttributes() } returns listOf(merchantAttribute)
        sut.saveMerchantAttribute(merchantAttribute)

        coVerify { dataStore.saveMerchantAttribute(merchantAttribute) }

        assertEquals(states[1].merchantAttributes, listOf(merchantAttribute))
    }

    @Test
    fun `test deleteMerchantAttribute`() = runTest {
        val states: MutableList<EnvironmentViewModelState> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        coEvery { dataStore.getMerchantAttributes() } returns listOf(merchantAttribute)
        sut.deleteMerchantAttribute(merchantAttribute)

        coVerify { dataStore.deleteMerchantAttribute(merchantAttribute) }

        assertEquals(states[1].merchantAttributes, listOf(merchantAttribute))
    }

    @Test
    fun `test onDeleteEnvironment`() = runTest {
        val states: MutableList<EnvironmentViewModelState> = mutableListOf()
        backgroundScope.launch(testDispatcher) {
            sut.state.toList(states)
        }
        coEvery { dataStore.getEnvironments() } returns listOf(environment)

        sut.onDeleteEnvironment(environment)

        coVerify { dataStore.deleteEnvironment(environment) }
        assertEquals(states[1].environments, listOf(environment))
    }

    @Test
    fun `test saveEnvironment`() = runTest {
        val states: MutableList<EnvironmentViewModelState> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }
        coEvery { dataStore.getEnvironments() } returns listOf(environment)

        sut.saveEnvironment(environment)
        coVerify { dataStore.saveEnvironment(environment) }
        assertEquals(states[1].environments, listOf(environment))
    }

    @Test
    fun `test saveEnvironment when environments empty`() = runTest {
        val states: MutableList<EnvironmentViewModelState> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }
        coEvery { dataStore.getEnvironments() } returns emptyList()

        sut.saveEnvironment(environment)

        coVerify { dataStore.setSelectedEnvironment(any()) }
        coVerify { dataStore.saveEnvironment(environment) }
    }

    @Test
    fun `test onSelectEnvironment`() = runTest {
        val states: MutableList<EnvironmentViewModelState> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        sut.onSelectEnvironment(environment)

        coVerify { dataStore.setSelectedEnvironment(environment) }

        assertEquals(states[1].selectedEnvironment, environment)
    }

    @Test
    fun `test onOrderActionSelected`() = runTest {
        val states: MutableList<EnvironmentViewModelState> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        sut.onOrderActionSelected("SALE")

        coVerify { dataStore.setOrderAction("SALE") }
        assertEquals(states[1].orderAction, "SALE")
    }

    @Test
    fun `test updateMerchantAttribute`() = runTest {
        val states: MutableList<EnvironmentViewModelState> = mutableListOf()
        backgroundScope.launch(testDispatcher) { sut.state.toList(states) }

        coEvery { dataStore.getMerchantAttributes() } returns listOf(merchantAttribute)

        sut.updateMerchantAttribute(merchantAttribute)

        coVerify { dataStore.updateMerchantAttribute(merchantAttribute) }
        assertEquals(states[1].merchantAttributes, listOf(merchantAttribute))
    }
}