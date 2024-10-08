package payment.sdk.android.demo

import android.content.Intent
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
import payment.sdk.android.cardpayment.CardPaymentData
import payment.sdk.android.cardpayment.CardPaymentRequest
import payment.sdk.android.aaniPay.AaniPayLauncher
import payment.sdk.android.demo.MainViewModel.Companion.CARD_PAYMENT_REQUEST_CODE
import payment.sdk.android.demo.ui.screen.environment.EnvironmentScreen
import payment.sdk.android.demo.ui.screen.home.HomeScreen
import payment.sdk.android.demo.ui.theme.NewMerchantAppTheme
import payment.sdk.android.samsungpay.SamsungPayResponse

class MainActivity : ComponentActivity(), SamsungPayResponse {

    private val paymentClient: PaymentClient by lazy {
        PaymentClient(this, "DEMO_VAL")
    }
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcher = AaniPayLauncher(this) { result ->
            when (result) {
                AaniPayLauncher.Result.Success -> viewModel.onSuccess()
                is AaniPayLauncher.Result.Failed -> viewModel.onFailure(result.error)
                AaniPayLauncher.Result.Canceled -> viewModel.onCanceled()
            }
        }
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state.state == MainViewModelStateType.PAYMENT_PROCESSING) {
                    when (state.paymentType) {
                        PaymentType.SAMSUNG_PAY -> paymentClient.launchSamsungPay(
                            state.order,
                            "",
                            this@MainActivity
                        )

                        PaymentType.AANI_PAY -> {
                            try {
                                launcher.launch(AaniPayLauncher.Config.create(state.order))
                            } catch (e: IllegalArgumentException) {
                                viewModel.onFailure(e.message.orEmpty())
                            }
                        }

                        PaymentType.CARD -> {
                            val authUrl: String =
                                state.order.links?.paymentAuthorizationUrl?.href.orEmpty()
                            val code = state.order.links?.paymentUrl?.href
                                ?.takeIf { it.isNotBlank() }
                                ?.split("=")
                                ?.getOrNull(1)
                                .orEmpty()
                            paymentClient.launchCardPayment(
                                request = CardPaymentRequest.builder()
                                    .gatewayUrl(authUrl)
                                    .code(code)
                                    .build(),
                                requestCode = CARD_PAYMENT_REQUEST_CODE
                            )
                        }

                        PaymentType.SAVED_CARD -> {
                            try {
                                paymentClient.launchSavedCardPayment(
                                    order = state.order,
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
        setContent {
            val navController = rememberNavController()
            val state by viewModel.state.collectAsState()

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
                            onClickAaniPay = {
                                viewModel.createOrder(
                                    PaymentType.AANI_PAY,
                                    viewModel.createOrderRequest()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CARD_PAYMENT_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> viewModel.onCardPaymentResponse(
                    CardPaymentData.getFromIntent(data!!)
                )

                RESULT_CANCELED -> viewModel.onCardPaymentCancelled()
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






