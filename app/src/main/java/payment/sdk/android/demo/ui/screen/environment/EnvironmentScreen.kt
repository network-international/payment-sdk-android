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
import androidx.compose.ui.platform.testTag
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
import payment.sdk.android.demo.model.AppCurrency
import payment.sdk.android.demo.model.AppLanguage
import payment.sdk.android.demo.model.OrderAction
import payment.sdk.android.demo.model.OrderType
import payment.sdk.android.demo.model.Region
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
                    IconButton(onClick = onNavUp, modifier = Modifier.testTag("environment_button_back")) {
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
                        .testTag("environment_text_version"),
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
                        region = viewModel.getRegion(),
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
                        region = viewModel.getRegion(),
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

                PickerView(
                    title = "Order Action",
                    items = OrderAction.entries,
                    selectedItem = viewModel.getOrderAction()
                ) {
                    @Suppress("UNCHECKED_CAST")
                    viewModel.setOrderAction(it as OrderAction)
                }

                HorizontalDivider()

                PickerView(
                    title = "Order Type",
                    items = OrderType.entries,
                    selectedItem = viewModel.getOrderType()
                ) {
                    @Suppress("UNCHECKED_CAST")
                    viewModel.setOrderType(it as OrderType)
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

                PickerView(
                    title = "Region",
                    items = Region.entries,
                    selectedItem = viewModel.getRegion()
                ) {
                    @Suppress("UNCHECKED_CAST")
                    viewModel.setRegion(it as Region)
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
    var showPicker by remember { mutableStateOf(false) }

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
        Text(
            text = hex,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(parseHexToColor(hex), RoundedCornerShape(6.dp))
                .border(1.5.dp, Color.Gray, RoundedCornerShape(6.dp))
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        ColorPickerDialog(
            initialColor = parseHexToColor(hex),
            onColorSelected = { color ->
                val r = (color.red * 255).toInt()
                val g = (color.green * 255).toInt()
                val b = (color.blue * 255).toInt()
                onHexChanged("#%02X%02X%02X".format(r, g, b))
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHsv = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                255,
                (initialColor.red * 255).toInt(),
                (initialColor.green * 255).toInt(),
                (initialColor.blue * 255).toInt()
            ),
            hsv
        )
        hsv
    }

    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var brightness by remember { mutableStateOf(initialHsv[2]) }

    val currentColor = Color.hsv(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Saturation-Brightness panel
                SatBrightnessPanel(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onSatBrightnessChanged = { s, v ->
                        saturation = s
                        brightness = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hue slider
                HueBar(
                    hue = hue,
                    onHueChanged = { hue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preview
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(initialColor, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("→", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(currentColor, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val r = (currentColor.red * 255).toInt()
                    val g = (currentColor.green * 255).toInt()
                    val b = (currentColor.blue * 255).toInt()
                    Text(
                        text = "#%02X%02X%02X".format(r, g, b),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(currentColor) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SatBrightnessPanel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onSatBrightnessChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSatBrightnessChanged(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSatBrightnessChanged(s, v)
                }
            }
    ) {
        val pureHueColor = Color.hsv(hue, 1f, 1f)

        // White to hue (horizontal saturation gradient)
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.White, pureHueColor))
        )
        // Transparent to black (vertical brightness gradient)
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
        )

        // Draw selector circle
        val cx = saturation * size.width
        val cy = (1f - brightness) * size.height
        drawCircle(
            color = Color.White,
            radius = with(density) { 8.dp.toPx() },
            center = Offset(cx, cy),
            style = Stroke(width = with(density) { 2.dp.toPx() })
        )
        drawCircle(
            color = Color.Black,
            radius = with(density) { 6.dp.toPx() },
            center = Offset(cx, cy),
            style = Stroke(width = with(density) { 1.dp.toPx() })
        )
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hueColors = remember {
        (0..360 step 1).map { Color.hsv(it.toFloat(), 1f, 1f) }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onHueChanged((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onHueChanged((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
    ) {
        drawRect(brush = Brush.horizontalGradient(hueColors))

        // Draw selector
        val cx = (hue / 360f) * size.width
        drawCircle(
            color = Color.White,
            radius = size.height / 2f,
            center = Offset(cx, size.height / 2f),
            style = Stroke(width = with(density) { 2.dp.toPx() })
        )
        drawCircle(
            color = Color.Black,
            radius = size.height / 2f - with(density) { 1.dp.toPx() },
            center = Offset(cx, size.height / 2f),
            style = Stroke(width = with(density) { 1.dp.toPx() })
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
