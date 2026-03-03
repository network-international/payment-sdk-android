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
import payment.sdk.android.SDKConfig
import payment.sdk.android.PaymentClient
import payment.sdk.android.demo.data.DataStoreImpl
import payment.sdk.android.core.getAuthorizationUrl
import payment.sdk.android.core.getPayPageUrl
import payment.sdk.android.demo.ui.screen.environment.EnvironmentScreen
import payment.sdk.android.demo.ui.screen.home.HomeScreen
import payment.sdk.android.demo.ui.screen.whatyouneed.WhatYouNeedScreen
import payment.sdk.android.demo.ui.theme.NewMerchantAppTheme
import payment.sdk.android.core.interactor.ClickToPayConfig
import payment.sdk.android.googlepay.GooglePayConfig
import payment.sdk.android.payments.UnifiedPaymentPageLauncher
import payment.sdk.android.payments.UnifiedPaymentPageRequest
import payment.sdk.android.samsungpay.SamsungPayResponse
import payment.sdk.android.savedCard.SavedCardPaymentLauncher
import payment.sdk.android.savedCard.SavedCardPaymentRequest

/**
 * Payment SDK Integration Guide
 *
 * PREREQUISITES:
 *   - Add payment-sdk dependency to build.gradle
 *   - Obtain API key and outlet reference from N-Genius portal
 *
 * STEPS:
 *   1. Initialize PaymentClient with your service ID
 *   2. Register payment launchers (UnifiedPaymentPageLauncher, SavedCardPaymentLauncher)
 *   3. Configure SDK options (SDKConfig.shouldShowOrderAmount, etc.)
 *   4. Create an order via your backend (see MainViewModel.createOrder)
 *   5. Launch payment with the order's auth URL and pay-page URL
 *   6. Handle results in the launcher callback (see MainViewModel.onPaymentResult)
 *
 * OPTIONAL:
 *   - Samsung Pay: Implement SamsungPayResponse, call paymentClient.launchSamsungPay()
 *   - Saved Cards: Use SavedCardPaymentLauncher
 *   - SDK Colors: Override payment_sdk_* colors in your app's colors.xml
 */
class MainActivity : ComponentActivity(), SamsungPayResponse {

    // Step 1: Initialize PaymentClient with your service ID
    private val paymentClient: PaymentClient by lazy {
        PaymentClient(this, "6b50b00a4a324030a0c671")
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(this, this)
    }

    // Step 2: Register payment launchers and handle results
    private val paymentsLauncher = UnifiedPaymentPageLauncher(
        this,
    ) { result ->
        viewModel.onPaymentResult(result)
    }

    private val savedCardPaymentLauncher = SavedCardPaymentLauncher(this) {
        viewModel.onPaymentResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Step 3: Configure SDK options
        SDKConfig.shouldShowOrderAmount(true)
        applySDKColors()
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
                            onRefresh = viewModel::onRefresh,
                            onClickWhatYouNeed = {
                                navController.navigate(WHAT_YOU_NEED_ROUTE)
                            }
                        )
                    }

                    composable(WHAT_YOU_NEED_ROUTE) {
                        WhatYouNeedScreen(
                            onNavUp = {
                                navController.popBackStack()
                            }
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

    private fun localeSelection(context: Context, localeTag: String) {
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
                        "WestZone",
                        this@MainActivity
                    )

                    PaymentType.CARD -> {
                        launchPaymentPage(
                            authUrl = effect.order.getAuthorizationUrl().orEmpty(),
                            payPageUrl = effect.order.getPayPageUrl().orEmpty()
                        )
                    }

                    PaymentType.SAVED_CARD -> {
                        savedCardPaymentLauncher.launch(
                            SavedCardPaymentRequest.Builder()
                                .payPageUrl(effect.order.getPayPageUrl().orEmpty())
                                .gatewayAuthorizationUrl(
                                    effect.order.getAuthorizationUrl().orEmpty()
                                )
                                .build()
                        )
                    }
                }
            }
        }
    }

    private fun launchPaymentPage(authUrl: String, payPageUrl: String) {
        val dataStore = DataStoreImpl(this)
        SDKConfig.setLanguage(dataStore.getLanguage().code)

        val googlePayConfig = GooglePayConfig(
            environment = GooglePayConfig.Environment.Test,
            merchantGatewayId = "BCR2DN4T263KB4BO"
        )
        // Click to Pay configuration - enable Visa Unified Click to Pay
        val clickToPayConfig = ClickToPayConfig(
            dpaId = "6BDAU1LI2WBPBQR665ED212rYO7vsj9wje83XQxlwzACNikj8",
            dpaClientId = "10c4cb74-3493-4515-ab72-2b303f790241",
            dpaName = "Demo Merchant",
            cardBrands = listOf("visa", "mastercard"),
            isSandbox = true,
            testOtpMode = false
        )
        paymentsLauncher.launch(
            UnifiedPaymentPageRequest.builder()
                .gatewayAuthorizationUrl(authUrl)
                .payPageUrl(payPageUrl)
                .setGooglePayConfig(googlePayConfig)
                .setClickToPayConfig(clickToPayConfig)
                .build()
        )
    }

    private fun applySDKColors() {
        val dataStore = DataStoreImpl(this)
        val colorMap = mapOf(
            "sdk_color_button" to payment.sdk.android.sdk.R.color.payment_sdk_pay_button_background_color,
            "sdk_color_button_text" to payment.sdk.android.sdk.R.color.payment_sdk_pay_button_text_color,
            "sdk_color_button_disabled" to payment.sdk.android.sdk.R.color.payment_sdk_button_disabled_background_color,
            "sdk_color_button_disabled_text" to payment.sdk.android.sdk.R.color.payment_sdk_button_disabled_text_color,
            "sdk_color_toolbar" to payment.sdk.android.sdk.R.color.payment_sdk_toolbar_color,
            "sdk_color_toolbar_text" to payment.sdk.android.sdk.R.color.payment_sdk_toolbar_text_color,
        )
        colorMap.forEach { (key, resId) ->
            val hex = dataStore.getSDKColor(key, "")
            if (hex.isNotEmpty()) {
                try {
                    val sanitized = hex.removePrefix("#")
                    if (sanitized.length == 6) {
                        val colorInt = (0xFF shl 24) or sanitized.toLong(16).toInt()
                        SDKConfig.setColor(resId, colorInt)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    companion object {
        const val HOME_ROUTE = "home"
        const val ENVIRONMENT_ROUTE = "ENVIRONMENT_ROUTE"
        const val WHAT_YOU_NEED_ROUTE = "what_you_need"
    }

    override fun onSuccess() {
        viewModel.onSuccess()
    }

    override fun onFailure(error: String) {
        viewModel.onFailure(error)
    }
}