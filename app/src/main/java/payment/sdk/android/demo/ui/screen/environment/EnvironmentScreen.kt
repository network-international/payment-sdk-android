package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import payment.sdk.android.demo.MainActivity
import payment.sdk.android.demo.isTablet
import payment.sdk.android.demo.ui.screen.SectionView
import payment.sdk.android.demo.ui.screen.SegmentedButtonItem
import payment.sdk.android.demo.ui.screen.SegmentedButtons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(
    onNavUp: () -> Unit
) {
    val activity = LocalContext.current as MainActivity
    val viewModel: EnvironmentViewModel = viewModel(
        factory = EnvironmentViewModel.provideFactory(activity, activity),
        viewModelStoreOwner = activity
    )

    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuration",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        content = { contentPadding ->
            var showAddEnvironmentDialog by remember { mutableStateOf(false) }
            var showMerchantAttributeDialog by remember { mutableStateOf(false) }
            val orderAction = remember { listOf("AUTH", "SALE", "PURCHASE") }
            var actionIndex by remember {
                mutableIntStateOf(orderAction.indexOf(state.orderAction))
            }

            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(10.dp)
            ) {
                if (showMerchantAttributeDialog) {
                    AddMerchantAttributeDialog(
                        onCancel = { showMerchantAttributeDialog = false }) {
                        viewModel.saveMerchantAttribute(it)
                    }
                }
                if (showAddEnvironmentDialog) {
                    AddEnvironmentDialog(
                        onCancel = { showAddEnvironmentDialog = false }
                    ) { environment ->
                        viewModel.saveEnvironment(environment)
                    }
                }

                Divider()
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    text = "Order Action",
                    style = MaterialTheme.typography.titleMedium
                )

                SegmentedButtons(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)) {
                    orderAction.forEachIndexed { index, option ->
                        SegmentedButtonItem(
                            selected = actionIndex == index,
                            onClick = {
                                actionIndex = index
                                viewModel.onOrderActionSelected(orderAction[index])
                            },
                            label = { Text(text = option) },
                        )
                    }
                }

                Divider()

                SectionView(
                    title = "Merchant Attributes",
                    count = state.merchantAttributes.size,
                    showDialog = { showMerchantAttributeDialog = true }
                ) {
                    LazyVerticalGrid(columns = GridCells.Fixed(if (isTablet()) 2 else 1)) {
                        items(state.merchantAttributes) { merchantAttribute ->
                            MerchantAttributeItem(
                                merchantAttribute = merchantAttribute,
                                deleteMerchantAttribute = {
                                    viewModel.deleteMerchantAttribute(merchantAttribute)
                                },
                                onChecked = {
                                    viewModel.updateMerchantAttribute(
                                        merchantAttribute.copy(
                                            isActive = it
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                SectionView(
                    title = "Environments",
                    count = state.environments.size,
                    showDialog = { showAddEnvironmentDialog = true }
                ) {
                    LazyVerticalGrid(columns = GridCells.Fixed(if (isTablet()) 2 else 1)) {
                        items(state.environments) { environment ->
                            val isSelected = state.selectedEnvironment?.id == environment.id
                            EnvironmentViewItem(
                                environment = environment,
                                isSelected,
                                onClick = {
                                    viewModel.onSelectEnvironment(environment)
                                },
                                onDelete = {
                                    viewModel.onDeleteEnvironment(environment)
                                })
                        }
                    }
                }
            }
        }
    )
}