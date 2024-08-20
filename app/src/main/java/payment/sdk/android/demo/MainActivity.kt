package payment.sdk.android.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import payment.sdk.android.demo.ui.screen.environment.EnvironmentScreen
import payment.sdk.android.demo.ui.screen.home.HomeScreen
import payment.sdk.android.demo.ui.theme.NewMerchantAppTheme
import payment.sdk.android.cardpayment.CardPaymentData

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                viewModel.onPayByCard()
                            },
                            onClickSamsungPay = {
                                viewModel.onSamsungPay()
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
                                viewModel.onPayBySavedCard(it)
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
        if (requestCode == MainViewModel.CARD_PAYMENT_REQUEST_CODE) {
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
}






