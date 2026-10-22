package payment.sdk.android.demo.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import payment.sdk.android.core.testId
import payment.sdk.android.R

@Composable
fun Alert(
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    onTechnicalDetails: (() -> Unit)? = null,
) {
    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = onConfirmation,
        confirmButton = {
            TextButton(onClick = onConfirmation, modifier = Modifier.testId("alert_button_ok")) {
                Text("Ok")
            }
        },
        dismissButton = onTechnicalDetails?.let {
            {
                TextButton(onClick = it, modifier = Modifier.testId("alert_button_technical_details")) {
                    Text(stringResource(R.string.technical_details))
                }
            }
        }
    )
}