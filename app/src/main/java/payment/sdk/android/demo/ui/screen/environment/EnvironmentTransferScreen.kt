package payment.sdk.android.demo.ui.screen.environment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import payment.sdk.android.core.testId
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.ScannedEnvironment

/**
 * Bytes per QR. Byte-mode capacity at error level M is 2331; the headroom keeps the
 * encoder from throwing on a long key and matches the outlet-cred-puller's limit, so a
 * batch splits the same way whichever end generates it.
 */
private const val QR_CHUNK_BYTES = 2000

/**
 * Moving environments between devices and the outlet-cred-puller.
 *
 * Four routes, all speaking the same pipe-delimited line format: copy to the clipboard,
 * paste from it, show a QR for another device to scan, and scan one. A set of outlets is
 * tedious to retype and easy to mistype — an API key is 200-odd characters of base64 —
 * so every route moves the whole list at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentTransferScreen(
    environments: List<Environment>,
    onNavUp: () -> Unit,
    onScan: () -> Unit,
    onImport: (String) -> Unit,
) {
    val context = LocalContext.current
    var pasted by remember { mutableStateOf("") }
    var qrChunks by remember { mutableStateOf<List<List<String>>?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transfer Environments") },
                navigationIcon = {
                    IconButton(onClick = onNavUp, modifier = Modifier.testId("transfer_button_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            ) {
                Text("Share", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Sends all ${environments.size} saved environment(s). The payload " +
                        "contains API keys — treat a copied blob or a displayed code as " +
                        "a credential.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (environments.isEmpty()) {
                                toast(context, "No environments to copy.")
                            } else {
                                copyToClipboard(context, ScannedEnvironment.encodeAll(environments, context))
                                toast(context, "${environments.size} environment(s) copied.")
                            }
                        },
                        modifier = Modifier.weight(1f).testId("transfer_button_copy"),
                    ) { Text("Copy all") }
                    OutlinedButton(
                        onClick = {
                            if (environments.isEmpty()) {
                                toast(context, "No environments to share.")
                            } else {
                                qrChunks = ScannedEnvironment.chunkLines(
                                    environments.map { ScannedEnvironment.encode(it, context) },
                                    QR_CHUNK_BYTES,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).testId("transfer_button_showQr"),
                    ) { Text("Show QR") }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Receive", style = MaterialTheme.typography.titleMedium)
                Text(
                    "An outlet already saved is updated in place rather than duplicated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                OutlinedButton(
                    onClick = onScan,
                    modifier = Modifier.fillMaxWidth().testId("transfer_button_scan"),
                ) { Text("Scan QR") }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pasted,
                    onValueChange = { pasted = it },
                    label = { Text("Paste environments") },
                    placeholder = { Text("One per line") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testId("transfer_field_paste"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { pasted = readClipboard(context) ?: pasted },
                        modifier = Modifier.weight(1f),
                    ) { Text("From clipboard") }
                    Button(
                        onClick = { onImport(pasted); pasted = "" },
                        modifier = Modifier.weight(1f).testId("transfer_button_import"),
                    ) { Text("Import") }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            qrChunks?.let { chunks ->
                QrPagesDialog(chunks = chunks, total = environments.size) { qrChunks = null }
            }
        },
    )
}

/**
 * The generated codes, one page at a time.
 *
 * A batch larger than one QR is split across several rather than shrunk to unscannable
 * density; the receiving scanner accumulates codes, so they can be scanned in any order.
 */
@Composable
private fun QrPagesDialog(chunks: List<List<String>>, total: Int, onDismiss: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val chunk = chunks[page]
    val many = chunks.size > 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$total environment(s)") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val bitmap = remember(chunk) { encodeQr(chunk.joinToString("\n")) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Environments QR code",
                        // A light quiet zone, regardless of the dialog's theme.
                        modifier = Modifier.size(280.dp).background(Color.White).padding(8.dp),
                    )
                } else {
                    Text("Could not generate a code for this batch.")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (many) "Code ${page + 1} of ${chunks.size} — ${chunk.size} outlet(s). Scan them all."
                    else "${chunk.size} outlet(s)",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (many) {
                    Row {
                        TextButton(onClick = { page-- }, enabled = page > 0) { Text("Previous") }
                        TextButton(onClick = { page++ }, enabled = page < chunks.size - 1) { Text("Next") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** Renders [text] as a QR bitmap, or null if it will not fit even one code. */
private fun encodeQr(text: String, size: Int = 720): Bitmap? = try {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 2,
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565).apply {
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
    }
} catch (e: Exception) {
    null
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("environments", text))
}

private fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
}

private fun toast(context: Context, message: String) =
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

/**
 * Hosts [EnvironmentTransferScreen] with the shared view model and the scanner.
 *
 * Kept separate so the screen above stays a plain function of its inputs — the scanner
 * launcher needs an Activity result contract, which a previewable composable should not
 * have to carry.
 */
@Composable
fun EnvironmentTransferRoute(onNavUp: () -> Unit) {
    val activity = LocalContext.current as payment.sdk.android.demo.MainActivity
    val viewModel: EnvironmentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = EnvironmentViewModel.provideFactory(activity, activity),
        viewModelStoreOwner = activity,
    )
    val state by viewModel.state.collectAsState()

    val scanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        val contents = result.contents ?: return@rememberLauncherForActivityResult
        val summary = viewModel.importPayload(contents)
        toast(activity, summary ?: "No environment found in that code.")
    }

    EnvironmentTransferScreen(
        environments = state.environments,
        onNavUp = onNavUp,
        onScan = {
            scanLauncher.launch(
                com.journeyapps.barcodescanner.ScanOptions().apply {
                    setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                    setPrompt("Scan environment QR code")
                    setBeepEnabled(false)
                    setOrientationLocked(true)
                }
            )
        },
        onImport = { payload ->
            val summary = viewModel.importPayload(payload)
            toast(activity, summary ?: "No line matched the expected format.")
        },
    )
}
