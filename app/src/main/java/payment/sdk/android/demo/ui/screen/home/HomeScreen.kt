package payment.sdk.android.demo.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import payment.sdk.android.demo.MainViewModelUiState
import payment.sdk.android.demo.MainViewModelStateType
import payment.sdk.android.demo.getAlertMessage
import payment.sdk.android.demo.isTablet
import payment.sdk.android.demo.model.Product
import payment.sdk.android.demo.ui.screen.Alert
import payment.sdk.android.demo.ui.screen.CircularProgressDialog
import payment.sdk.android.demo.ui.screen.TopBar
import payment.sdk.android.core.SavedCard

@Composable
fun HomeScreen(
    state: MainViewModelUiState,
    onSelectProduct: (Product) -> Unit,
    onAddProduct: (Product) -> Unit,
    onClickPayByCard: () -> Unit,
    onClickSamsungPay: () -> Unit,
    closeDialog: () -> Unit,
    onClickEnvironment: () -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onSelectSavedCard: (SavedCard) -> Unit,
    onDeleteSavedCard: (SavedCard) -> Unit,
    onPaySavedCard: (SavedCard) -> Unit,
    onRefresh: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(LocalLifecycleOwner.current) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showAddProductDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopBar(
                topAppBarText = "Demo Store",
                onAddProduct = { showAddProductDialog = true },
                onSettingClicked = onClickEnvironment
            )
        },
        content = { contentPadding ->
            Column(Modifier.padding(contentPadding)) {
                if (showAddProductDialog) {
                    AddProductDialog(onCancel = { showAddProductDialog = false }) {
                        onAddProduct(it)
                    }
                }
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    columns = GridCells.Fixed(if (isTablet()) 4 else 2),
                ) {
                    items(state.products) { product ->
                        ProductItem(
                            product = product,
                            isSelected = state.selectedProducts.contains(product),
                            currency = state.currency,
                            onClick = { onSelectProduct(product) },
                            onDeleteProduct = { onDeleteProduct(product) }
                        )
                    }
                }
                AnimatedVisibility(
                    visible = state.total != 0.00,
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    HomeBottomBar(
                        modifier = Modifier,
                        total = state.total,
                        isSamsungPayAvailable = state.isSamsungPayAvailable,
                        currency = state.currency,
                        onClickPayByCard = onClickPayByCard,
                        onClickSamsungPay = onClickSamsungPay,
                        savedCard = state.savedCard,
                        savedCards = state.savedCards,
                        onSelectCard = onSelectSavedCard,
                        onDeleteSavedCard = onDeleteSavedCard,
                        onPaySavedCard = onPaySavedCard,
                    )
                }

                when (state.state) {
                    MainViewModelStateType.INIT -> {}
                    MainViewModelStateType.LOADING -> CircularProgressDialog(message = state.message)
                    MainViewModelStateType.PAYMENT_SUCCESS,
                    MainViewModelStateType.ERROR,
                    MainViewModelStateType.PAYMENT_POST_AUTH_REVIEW,
                    MainViewModelStateType.PAYMENT_FAILED,
                    MainViewModelStateType.PAYMENT_PARTIAL_AUTH_DECLINED,
                    MainViewModelStateType.PAYMENT_PARTIAL_AUTH_DECLINE_FAILED,
                    MainViewModelStateType.PAYMENT_CANCELLED,
                    MainViewModelStateType.PAYMENT_PARTIALLY_AUTHORISED,
                    MainViewModelStateType.AUTHORIZED -> {
                        val (title, message) = state.state.getAlertMessage(state.message)
                        Alert(
                            onConfirmation = closeDialog,
                            dialogTitle = title,
                            dialogText = message
                        )
                    }

                    MainViewModelStateType.PAYMENT_PROCESSING -> { }
                }
            }
        }
    )
}