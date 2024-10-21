package payment.sdk.android.demo

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import payment.sdk.android.PaymentClient
import payment.sdk.android.cardpayment.CardPaymentRequest
import payment.sdk.android.core.getAuthorizationUrl
import payment.sdk.android.core.getPayPageUrl
import payment.sdk.android.demo.MainViewModel.Companion.CARD_PAYMENT_REQUEST_CODE
import payment.sdk.android.demo.ui.screen.environment.EnvironmentScreen
import payment.sdk.android.demo.ui.screen.home.HomeScreen
import payment.sdk.android.demo.ui.theme.NewMerchantAppTheme
import payment.sdk.android.payments.PaymentsLauncher
import payment.sdk.android.payments.PaymentsRequest
import payment.sdk.android.samsungpay.SamsungPayResponse

class MainActivity : ComponentActivity(), SamsungPayResponse {

    private val paymentClient: PaymentClient by lazy {
        PaymentClient(this, "DEMO_VAL")
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(this, this)
    }

    private val cardPaymentsClient = PaymentsLauncher(
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
                            },
                            onChangeLanguage = {
                                localeSelection(this@MainActivity, it.code)
                            }
                        )
                    }
                }
            }
        }
    }

    fun localeSelection(context: Context, localeTag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(localeTag)
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(localeTag)
            )
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
                        makeCardPaymentNew(
                            authUrl = effect.order.getAuthorizationUrl().orEmpty(),
                            payPageUrl = effect.order.getPayPageUrl().orEmpty()
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

    private fun makeCardPaymentNew(authUrl: String, payPageUrl: String) {
        cardPaymentsClient.launch(
            PaymentsRequest.builder()
                .gatewayAuthorizationUrl(authUrl)
                .payPageUrl(payPageUrl)
                .setLanguageCode(viewModel.getLanguageCode())
                .build()
        )
    }

    private fun makeCardPayment(authUrl: String, payPageUrl: String) {
        val code = payPageUrl
            .takeIf { it.isNotBlank() }
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