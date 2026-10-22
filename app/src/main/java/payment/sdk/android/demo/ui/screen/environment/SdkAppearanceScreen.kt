package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import payment.sdk.android.core.testId
import payment.sdk.android.demo.MainActivity

/**
 * SDK Appearance — the SDK's runtime look-and-feel, on its own page.
 *
 * Separate from the environment screen because these settings describe how the
 * SDK draws, not which outlet is charged, and eleven colour rows crowded a
 * screen that is mostly about environments. The iOS and Flutter demos show the
 * same list under the same name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdkAppearanceScreen(onNavUp: () -> Unit) {
    // Scoped to the activity, the same instance the environment screen uses, so a
    // colour changed here is already applied when either screen is next shown.
    val activity = LocalContext.current as MainActivity
    val viewModel: EnvironmentViewModel = viewModel(
        factory = EnvironmentViewModel.provideFactory(activity, activity),
        viewModelStoreOwner = activity,
    )
    val sdkColors by viewModel.sdkColors.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SDK Appearance") },
                navigationIcon = {
                    IconButton(onClick = onNavUp, modifier = Modifier.testId("sdkappearance_button_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetSDKColors() },
                        modifier = Modifier.testId("sdkappearance_button_reset"),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset to SDK defaults")
                    }
                },
            )
        },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Tap a swatch to pick a colour. These apply to the SDK's own screens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                viewModel.sdkColorDefs.forEach { def ->
                    SDKColorRow(
                        label = def.label,
                        hex = sdkColors[def.key] ?: def.defaultHex,
                        onHexChanged = { viewModel.setSDKColor(def.key, it) },
                    )
                }
                Spacer(modifier = Modifier.padding(bottom = 24.dp))
            }
        },
    )
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
