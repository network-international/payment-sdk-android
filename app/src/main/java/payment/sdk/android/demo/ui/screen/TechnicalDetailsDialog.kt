package payment.sdk.android.demo.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.CompositionLocalProvider
import payment.sdk.android.core.testId
import payment.sdk.android.R

/** Full-screen, selectable view of a failure report, with Copy and Share. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalDetailsDialog(report: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val title = stringResource(R.string.technical_details)
    val copied = stringResource(R.string.technical_details_copied)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testId("techdetails_button_close")) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                copyToClipboard(context, title, report)
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testId("techdetails_button_copy")
                        ) { Text(stringResource(R.string.technical_details_copy)) }
                        IconButton(
                            onClick = { share(context, title, report) },
                            modifier = Modifier.testId("techdetails_button_share")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.technical_details_share)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            // Report text is machine output; keep it left-to-right in Arabic too.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                SelectionContainer(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = report,
                        modifier = Modifier
                            .padding(16.dp)
                            .testId("techdetails_text"),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            textDirection = TextDirection.Ltr
                        )
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun share(context: Context, subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, subject))
}
