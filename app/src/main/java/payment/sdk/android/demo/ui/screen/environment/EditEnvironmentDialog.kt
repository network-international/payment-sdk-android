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
import payment.sdk.android.core.testId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEnvironmentDialog(
    environment: Environment,
    onCancel: () -> Unit,
    onSave: (environment: Environment) -> Unit
) {
    var nickname by remember { mutableStateOf(environment.nickname.orEmpty()) }
    var apiKey by remember { mutableStateOf(environment.apiKey) }
    var outletReference by remember { mutableStateOf(environment.outletReference) }
    var realm by remember { mutableStateOf(environment.realm) }
    var clickToPayMerchantId by remember { mutableStateOf(environment.clickToPayMerchantId.orEmpty()) }
    val entries = EnvironmentType.values()
    var selectedEnvironment by remember { mutableIntStateOf(entries.indexOf(environment.type)) }
    val regionEntries = Region.values()
    var selectedRegion by remember {
        mutableIntStateOf(regionEntries.indexOf(environment.region).coerceAtLeast(0))
    }

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
        Text("Region")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().testId("editenv_picker_region")) {
            regionEntries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selectedRegion == index,
                    onClick = { selectedRegion = index },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = regionEntries.count()
                    )
                ) {
                    Text(text = option.displayValue)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Nickname (optional)") },
            modifier = Modifier.fillMaxWidth().testId("editenv_field_nickname")
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = realm,
            onValueChange = { realm = it },
            label = { Text("Realm") },
            modifier = Modifier.fillMaxWidth().testId("editenv_field_realm")
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth().testId("editenv_field_apiKey")
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = outletReference,
            onValueChange = { outletReference = it },
            label = { Text("Outlet Reference") },
            modifier = Modifier.fillMaxWidth().testId("editenv_field_outletReference")
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = clickToPayMerchantId,
            onValueChange = { clickToPayMerchantId = it },
            label = { Text("Click to Pay Merchant ID (optional)") },
            modifier = Modifier.fillMaxWidth().testId("editenv_field_clickToPayMerchantId")
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
                            nickname = nickname,
                            apiKey = apiKey,
                            outletReference = outletReference,
                            realm = realm,
                            region = regionEntries[selectedRegion],
                            clickToPayMerchantId = clickToPayMerchantId.takeIf { it.isNotBlank() }
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
                    .testId("editenv_button_save")
            ) {
                Text("Save")
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testId("editenv_button_cancel")
            ) {
                Text("Cancel")
            }
        }
    }
}
