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
import payment.sdk.android.demo.model.Region
import payment.sdk.android.demo.ui.screen.AppDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEnvironmentDialog(
    onCancel: () -> Unit,
    onAddEnvironment: (environment: Environment) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var outletReference by remember { mutableStateOf("") }
    var realm by remember { mutableStateOf("") }
    val entries = EnvironmentType.values()
    var selectedEnvironment by remember { mutableIntStateOf(0) }
    val regionEntries = Region.values()
    var selectedRegion by remember { mutableIntStateOf(0) }

    AppDialog(title = "Add Environment", onCancel = onCancel) {
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
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            regionEntries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selectedRegion == index,
                    onClick = { selectedRegion = index },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = regionEntries.count()
                    )
                ) {
                    Text(text = option.code)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
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
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = realm,
            onValueChange = { realm = it },
            label = { Text("Realm") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    onAddEnvironment(
                        Environment(
                            type = entries[selectedEnvironment],
                            name = name,
                            apiKey = apiKey,
                            outletReference = outletReference,
                            realm = realm,
                            region = regionEntries[selectedRegion]
                        )
                    )
                    onCancel()
                },
                enabled = name.isNotBlank() &&
                        apiKey.isNotBlank() &&
                        outletReference.isNotBlank() &&
                        realm.isNotBlank(),
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