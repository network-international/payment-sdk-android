package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import payment.sdk.android.demo.model.MerchantAttribute
import payment.sdk.android.demo.ui.screen.AppDialog

@Composable
fun AddMerchantAttributeDialog(
    onCancel: () -> Unit,
    onAddMerchantAttribute: (MerchantAttribute) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AppDialog(title = "Add Merchant Attribute", onCancel = onCancel) {
        TextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Value") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    onAddMerchantAttribute(
                        MerchantAttribute(
                            key = key,
                            value = value
                        )
                    )
                    onCancel()
                },
                enabled = key.isNotBlank() && value.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text("Add")
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}