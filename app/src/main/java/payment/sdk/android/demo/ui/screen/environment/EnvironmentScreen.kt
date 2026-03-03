package payment.sdk.android.demo.ui.screen.environment

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextField
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import payment.sdk.android.BuildConfig
import payment.sdk.android.SDKConfig
import payment.sdk.android.demo.MainActivity
import payment.sdk.android.demo.isTablet
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.AppLanguage
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.ui.screen.SectionView
import payment.sdk.android.sdk.R as SdkR

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

    var showAddEnvironmentDialog by remember { mutableStateOf(false) }
    var showMerchantAttributeDialog by remember { mutableStateOf(false) }
    var showQrConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var qrRealm by rememberSaveable { mutableStateOf("") }
    var qrOutletReference by rememberSaveable { mutableStateOf("") }
    var qrApiKey by rememberSaveable { mutableStateOf("") }
    var editingEnvironment by remember { mutableStateOf<Environment?>(null) }

    var isExpandedEnvironments by remember { mutableStateOf(false) }
    var isExpandedMerchantAttributes by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val parts = result.contents.split("|")
            if (parts.size == 3) {
                qrRealm = parts[0]
                qrOutletReference = parts[1]
                qrApiKey = parts[2]
                showQrConfirmDialog = true
            } else {
                Toast.makeText(
                    context,
                    "Invalid QR format. Expected: realm|outletReference|apiKey",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

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
                    .verticalScroll(rememberScrollState())
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
                if (showQrConfirmDialog) {
                    QrEnvironmentDialog(
                        realm = qrRealm,
                        outletReference = qrOutletReference,
                        apiKey = qrApiKey,
                        onCancel = { showQrConfirmDialog = false }
                    ) { environment ->
                        viewModel.saveEnvironment(environment)
                        isExpandedEnvironments = true
                    }
                }
                editingEnvironment?.let { env ->
                    EditEnvironmentDialog(
                        environment = env,
                        onCancel = { editingEnvironment = null },
                        onSave = { updated ->
                            viewModel.updateEnvironment(updated)
                            editingEnvironment = null
                        }
                    )
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
                    @Suppress("UNCHECKED_CAST")
                    onChangeLanguage(it as AppLanguage)
                    @Suppress("UNCHECKED_CAST")
                    viewModel.setLanguage(it as AppLanguage)
                }

                HorizontalDivider()

                PickerView(
                    title = "Currency",
                    items = AppCurrency.entries,
                    selectedItem = viewModel.getCurrency()
                ) {
                    @Suppress("UNCHECKED_CAST")
                    viewModel.setCurrency(it as AppCurrency)
                }

                HorizontalDivider()

                SDKColorsSection(viewModel)

                HorizontalDivider()

                SectionView(
                    title = "Merchant Attributes",
                    count = state.merchantAttributes.size,
                    isExpanded = isExpandedMerchantAttributes,
                    onExpand = { isExpandedMerchantAttributes = it },
                    showDialog = { showMerchantAttributeDialog = true }
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (isTablet()) 2 else 1),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
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
                    showDialog = { showAddEnvironmentDialog = true },
                    onScan = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Scan environment QR code")
                            setBeepEnabled(false)
                            setOrientationLocked(true)
                        }
                        qrScanLauncher.launch(options)
                    }
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (isTablet()) 2 else 1),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(state.environments) { environment ->
                            val isSelected = state.selectedEnvironment?.id == environment.id
                            EnvironmentViewItem(
                                environment = environment,
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.onSelectEnvironment(environment)
                                },
                                onEdit = {
                                    editingEnvironment = environment
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

@Composable
private fun SDKColorsSection(viewModel: EnvironmentViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    val sdkColors by viewModel.sdkColors.collectAsState()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SDK Colors",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "toggle"
                )
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                viewModel.sdkColorDefs.forEach { def ->
                    SDKColorRow(
                        label = def.label,
                        hex = sdkColors[def.key] ?: def.defaultHex,
                        onHexChanged = { viewModel.setSDKColor(def.key, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SDKColorRow(label: String, hex: String, onHexChanged: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = hex,
            onValueChange = { newValue ->
                onHexChanged(newValue.uppercase())
            },
            modifier = Modifier.width(120.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(parseHexToColor(hex), RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
        )
    }
}

private fun parseHexToColor(hex: String): Color {
    return try {
        val sanitized = hex.removePrefix("#")
        if (sanitized.length == 6) {
            Color(android.graphics.Color.parseColor("#$sanitized"))
        } else {
            Color.Gray.copy(alpha = 0.3f)
        }
    } catch (e: Exception) {
        Color.Gray.copy(alpha = 0.3f)
    }
}
