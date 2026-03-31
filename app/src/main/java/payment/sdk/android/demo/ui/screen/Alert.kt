package payment.sdk.android.demo.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun Alert(
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
) {
    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = onConfirmation,
        confirmButton = {
            TextButton(onClick = onConfirmation, modifier = Modifier.testTag("alert_button_ok")) {
                Text("Ok")
            }
        }
    )
}