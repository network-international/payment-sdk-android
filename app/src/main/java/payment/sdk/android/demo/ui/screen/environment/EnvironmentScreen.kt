package payment.sdk.android.demo.ui.screen.environment

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ListItem
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.material3.OutlinedTextField
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import payment.sdk.android.BuildConfig
import payment.sdk.android.SDKConfig
import payment.sdk.android.demo.MainActivity
import payment.sdk.android.demo.isTablet
import payment.sdk.android.demo.model.AppLanguage
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.ScannedEnvironment
import payment.sdk.android.demo.ui.screen.SectionView
import payment.sdk.android.sdk.R as SdkR
import payment.sdk.android.core.testId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(
    onNavUp: () -> Unit,
    onClickSdkAppearance: () -> Unit,
    onClickTransfer: () -> Unit,
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
    var qrScanned by remember { mutableStateOf<ScannedEnvironment?>(null) }
    var editingEnvironment by remember { mutableStateOf<Environment?>(null) }
    var environmentPendingDeletion by remember { mutableStateOf<Environment?>(null) }

    var isExpandedEnvironments by remember { mutableStateOf(false) }
    var isExpandedMerchantAttributes by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val scanOptions = ScanOptions().apply {
        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        setPrompt("Scan environment QR code")
        setBeepEnabled(false)
        setOrientationLocked(true)
    }
    var scanAnotherPrompt by remember { mutableStateOf<String?>(null) }
    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@rememberLauncherForActivityResult
        val batch = ScannedEnvironment.parseAll(contents)
        when {
            batch.environments.isEmpty() -> Toast.makeText(
                context,
                "Invalid QR format. Expected one per line: " +
                    "nickname|realm|outletReference|apiKey|applePayMerchantId|region|currency|type|orderAction|orderType",
                Toast.LENGTH_LONG
            ).show()
            // One outlet: confirm before saving, as before. A batch is imported
            // directly — the point is not to confirm ten dialogs.
            batch.environments.size == 1 && batch.rejected == 0 -> {
                qrScanned = batch.environments.single()
                showQrConfirmDialog = true
            }
            else -> {
                val r = viewModel.importEnvironments(batch.environments.map { it.toEnvironment() })
                isExpandedEnvironments = true
                val skipped = if (batch.rejected > 0) ", ${batch.rejected} line(s) skipped" else ""
                // The scanner is one-shot on Android (ZXing ScanContract), so a bulk
                // export split across several codes is collected by relaunching it.
                scanAnotherPrompt = "${r.added} added, ${r.replaced} updated$skipped."
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavUp, modifier = Modifier.testId("environment_button_back")) {
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
                        .padding(vertical = 6.dp)
                        .testId("environment_text_version"),
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
                    EnvironmentFormSheet(
                        existing = null,
                        onCancel = { showAddEnvironmentDialog = false },
                        onSave = { environment ->
                            viewModel.saveEnvironment(environment)
                            isExpandedEnvironments = true
                            showAddEnvironmentDialog = false
                        },
                    )
                }
                qrScanned?.takeIf { showQrConfirmDialog }?.let { scanned ->
                    QrEnvironmentDialog(
                        scanned = scanned,
                        onCancel = { showQrConfirmDialog = false }
                    ) { environment ->
                        viewModel.saveEnvironment(environment)
                        isExpandedEnvironments = true
                    }
                }
                environmentPendingDeletion?.let { env ->
                    DeleteEnvironmentDialog(
                        environment = env,
                        onCancel = { environmentPendingDeletion = null },
                        onConfirm = {
                            viewModel.onDeleteEnvironment(env)
                            environmentPendingDeletion = null
                        },
                    )
                }
                scanAnotherPrompt?.let { summary ->
                    AlertDialog(
                        onDismissRequest = { scanAnotherPrompt = null },
                        title = { Text("Environments imported") },
                        text = { Text("$summary Scan another code?") },
                        confirmButton = {
                            TextButton(onClick = {
                                scanAnotherPrompt = null
                                qrScanLauncher.launch(scanOptions)
                            }) { Text("Scan another") }
                        },
                        dismissButton = {
                            TextButton(onClick = { scanAnotherPrompt = null }) { Text("Done") }
                        }
                    )
                }
                editingEnvironment?.let { env ->
                    EnvironmentFormSheet(
                        existing = env,
                        onCancel = { editingEnvironment = null },
                        onSave = { updated ->
                            viewModel.updateEnvironment(updated)
                            editingEnvironment = null
                        }
                    )
                }

                HorizontalDivider()

                // Order action, order type, currency and region are outlet configuration, not
                // app settings — which actions and payment methods an outlet accepts differ per
                // outlet, and a key only works against its own region's deployment. They live in
                // the add/edit environment dialogs; only app-wide settings remain here.
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

                // SDK appearance is about how the SDK draws, not which outlet is
                // charged, and eleven colour rows crowd a screen that is mostly
                // about environments. It lives on its own page.
                ListItem(
                    modifier = Modifier
                        .clickable { onClickTransfer() }
                        .testId("environment_row_transfer"),
                    headlineContent = { Text("Transfer Environments") },
                    supportingContent = { Text("Copy, paste, scan or show a QR") },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                )

                ListItem(
                    modifier = Modifier
                        .clickable { onClickSdkAppearance() }
                        .testId("environment_row_sdkAppearance"),
                    headlineContent = { Text("SDK Appearance") },
                    supportingContent = { Text("SDK colours") },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                )

                HorizontalDivider()

                SectionView(
                    title = "Merchant Attributes",
                    count = state.merchantAttributes.size,
                    isExpanded = isExpandedMerchantAttributes,
                    onExpand = { isExpandedMerchantAttributes = it },
                    showDialog = { showMerchantAttributeDialog = true },
                    addButtonId = "environment_button_addMerchantAttribute",
                    toggleButtonId = "environment_button_toggleMerchantAttributes"
                ) {
                    Column {
                        state.merchantAttributes.forEach { merchantAttribute ->
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
                    addButtonId = "environment_button_addEnvironment",
                    scanButtonId = "environment_button_scanQR",
                    toggleButtonId = "environment_button_toggleEnvironments",
                    onScan = {
                        qrScanLauncher.launch(scanOptions)
                    }
                ) {
                    // A plain Column, not a height-capped lazy grid: the grid
                    // scrolled inside its own 300dp window while the page scrolled
                    // separately, so only three or four outlets were ever reachable
                    // and it was unclear the list continued. The page's own scroll
                    // now carries everything.
                    Column {
                        state.environments.forEach { environment ->
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
                                    environmentPendingDeletion = environment
                                })
                        }
                    }
                }
            }
        }
    )
}
