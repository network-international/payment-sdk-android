package payment.sdk.android.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import payment.sdk.android.PaymentClient
import payment.sdk.android.core.getAuthorizationUrl
import payment.sdk.android.core.getPayPageUrl
import payment.sdk.android.demo.MainViewModel.Companion.CARD_PAYMENT_REQUEST_CODE
import payment.sdk.android.demo.ui.screen.environment.EnvironmentScreen
import payment.sdk.android.demo.ui.screen.home.HomeScreen
import payment.sdk.android.demo.ui.theme.NewMerchantAppTheme
import payment.sdk.android.payments.CardPaymentsLauncher
import payment.sdk.android.payments.PaymentsRequest
import payment.sdk.android.samsungpay.SamsungPayResponse

class MainActivity : ComponentActivity(), SamsungPayResponse {

    private val paymentClient: PaymentClient by lazy {
        PaymentClient(this, "DEMO_VAL")
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(this, this)
    }

    private val cardPaymentsClient = CardPaymentsLauncher(
        this,
    ) { result ->
        viewModel.onPaymentResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlePayments()
        setContent {
            val navController = rememberNavController()
            val state by viewModel.uiState.collectAsState()

            NewMerchantAppTheme {
                NavHost(
                    navController = navController,
                    startDestination = HOME_ROUTE
                ) {

                    composable(HOME_ROUTE) {
                        HomeScreen(
                            state,
                            onSelectProduct = {
                                viewModel.onSelectProduct(it)
                            },
                            onClickPayByCard = {
                                viewModel.createOrder(
                                    PaymentType.CARD,
                                    viewModel.createOrderRequest()
                                )
                            },
                            onClickSamsungPay = {
                                viewModel.createOrder(
                                    PaymentType.SAMSUNG_PAY,
                                    viewModel.createOrderRequest()
                                )
                            },
                            closeDialog = {
                                viewModel.closeDialog()
                            },
                            onClickEnvironment = {
                                navController.navigate(ENVIRONMENT_ROUTE)
                            },
                            onAddProduct = {
                                viewModel.onAddProduct(it)
                            },
                            onDeleteProduct = {
                                viewModel.onDeleteProduct(it)
                            },
                            onSelectSavedCard = {
                                viewModel.setSavedCard(it)
                            },
                            onDeleteSavedCard = {
                                viewModel.deleteSavedCard(it)
                            },
                            onPaySavedCard = {
                                viewModel.createOrder(
                                    PaymentType.SAVED_CARD,
                                    viewModel.createOrderRequest(it)
                                )
                            },
                            onRefresh = viewModel::onRefresh
                        )
                    }

                    composable(ENVIRONMENT_ROUTE) {
                        EnvironmentScreen(
                            onNavUp = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handlePayments() {
        lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                when (effect.type) {
                    PaymentType.SAMSUNG_PAY -> paymentClient.launchSamsungPay(
                        effect.order,
                        "",
                        this@MainActivity
                    )

                    PaymentType.CARD -> {
                        cardPaymentsClient.launch(
                            PaymentsRequest.builder()
                                .gatewayAuthorizationUrl(
                                    effect.order.getAuthorizationUrl().orEmpty()
                                )
                                .payPageUrl(effect.order.getPayPageUrl().orEmpty())
                                .setLanguageCode(viewModel.getLanguageCode())
                                .build()
                        )
                    }

                    PaymentType.SAVED_CARD -> {
                        try {
                            paymentClient.launchSavedCardPayment(
                                order = effect.order,
                                code = CARD_PAYMENT_REQUEST_CODE
                            )
                        } catch (e: IllegalArgumentException) {
                            viewModel.onFailure(e.message.orEmpty())
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val HOME_ROUTE = "home"
        const val ENVIRONMENT_ROUTE = "ENVIRONMENT_ROUTE"
    }

    override fun onSuccess() {
        viewModel.onSuccess()
    }

    override fun onFailure(error: String) {
        viewModel.onFailure(error)
    }
}






