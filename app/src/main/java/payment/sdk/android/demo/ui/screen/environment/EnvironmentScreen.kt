package payment.sdk.android.demo.ui.screen.environment

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import payment.sdk.android.BuildConfig
import payment.sdk.android.SDKConfig
import payment.sdk.android.demo.MainActivity
import payment.sdk.android.demo.isTablet
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.AppLanguage
import payment.sdk.android.demo.ui.screen.SectionView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(
    onNavUp: () -> Unit,
    onChangeLanguage: (AppLanguage) -> Unit
) {
    val activity = LocalContext.current as MainActivity
    val viewModel: EnvironmentViewModel = viewModel(
        factory = EnvironmentViewModel.provideFactory(activity, activity),
        viewModelStoreOwner = activity
    )

    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Configuration") },
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

            var isExpandedEnvironments by remember { mutableStateOf(false) }
            var isExpandedMerchantAttributes by remember { mutableStateOf(false) }
            val orderAction = remember { listOf("AUTH", "SALE", "PURCHASE") }
            val orderType = remember { listOf("SINGLE", "RECURRING", "UNSCHEDULED", "INSTALLMENT") }
            var actionIndex by remember {
                mutableIntStateOf(orderAction.indexOf(state.orderAction))
            }
            var typeIndex by remember {
                mutableIntStateOf(orderType.indexOf(state.orderType))
            }

            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(10.dp)
            ) {
                HorizontalDivider()
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    text = "Build: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) - SDK: v${SDKConfig.getSDKVersion()}",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
                if (showMerchantAttributeDialog) {
                    AddMerchantAttributeDialog(
                        onCancel = { showMerchantAttributeDialog = false }) {
                        viewModel.saveMerchantAttribute(it)
                        isExpandedMerchantAttributes = true
                    }
                }
                if (showAddEnvironmentDialog) {
                    AddEnvironmentDialog(
                        onCancel = { showAddEnvironmentDialog = false }
                    ) { environment ->
                        viewModel.saveEnvironment(environment)
                        isExpandedEnvironments = true
                    }
                }

                HorizontalDivider()
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    text = "Order Action",
                    style = MaterialTheme.typography.titleMedium
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    orderAction.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = actionIndex == index,
                            onClick = {
                                actionIndex = index
                                viewModel.onOrderActionSelected(orderAction[index])
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = orderAction.count()
                            )
                        ) {
                            Text(text = option)
                        }
                    }
                }

                HorizontalDivider()
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    text = "Order type",
                    style = MaterialTheme.typography.titleMedium
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orderType.size) { index ->
                        AssistChip(
                            onClick = {
                                typeIndex = index
                                viewModel.onOrderTypeSelected(orderType[index])
                            },
                            label = { Text(orderType[index]) },
                            leadingIcon = if (typeIndex == index) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }


                HorizontalDivider()

                PickerView(
                    title = "Language",
                    items = AppLanguage.entries,
                    selectedItem = viewModel.getLanguage()
                ) {
                    onChangeLanguage(it as AppLanguage)
                    viewModel.setLanguage(it as AppLanguage)
                }

                HorizontalDivider()

                PickerView(
                    title = "Currency",
                    items = AppCurrency.entries,
                    selectedItem = viewModel.getCurrency()
                ) {
                    viewModel.setCurrency(it as AppCurrency)
                }

                HorizontalDivider()

                SectionView(
                    title = "Merchant Attributes",
                    count = state.merchantAttributes.size,
                    isExpanded = isExpandedMerchantAttributes,
                    onExpand = { isExpandedMerchantAttributes = it },
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
                    isExpanded = isExpandedEnvironments,
                    onExpand = { isExpandedEnvironments = it },
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
