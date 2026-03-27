package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentType
import payment.sdk.android.demo.ui.screen.AppDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEnvironmentDialog(
    environment: Environment,
    onCancel: () -> Unit,
    onSave: (environment: Environment) -> Unit
) {
    var apiKey by remember { mutableStateOf(environment.apiKey) }
    var outletReference by remember { mutableStateOf(environment.outletReference) }
    var realm by remember { mutableStateOf(environment.realm) }
    val entries = EnvironmentType.values()
    var selectedEnvironment by remember { mutableIntStateOf(entries.indexOf(environment.type)) }

    AppDialog(title = "Edit Environment", onCancel = onCancel) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selectedEnvironment == index,
                    onClick = { selectedEnvironment = index },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = entries.count()
                    )
                ) {
                    Text(text = option.value)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = realm,
            onValueChange = { realm = it },
            label = { Text("Realm") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = outletReference,
            onValueChange = { outletReference = it },
            label = { Text("Outlet Reference") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    onSave(
                        environment.copy(
                            type = entries[selectedEnvironment],
                            name = realm,
                            apiKey = apiKey,
                            outletReference = outletReference,
                            realm = realm,
                            region = environment.region
                        )
                    )
                    onCancel()
                },
                enabled = apiKey.isNotBlank() &&
                        outletReference.isNotBlank() &&
                        realm.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text("Save")
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
