package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentType
import payment.sdk.android.demo.model.Region
import payment.sdk.android.demo.ui.screen.AppDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrEnvironmentDialog(
    realm: String,
    outletReference: String,
    apiKey: String,
    region: Region,
    onCancel: () -> Unit,
    onAddEnvironment: (environment: Environment) -> Unit
) {
    val entries = EnvironmentType.values()
    var selectedEnvironment by remember { mutableIntStateOf(0) }

    AppDialog(title = "Add Scanned Environment", onCancel = onCancel) {
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Realm",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = realm,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Outlet Reference",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = outletReference,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "API Key",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = if (apiKey.length > 20) "${apiKey.take(10)}...${apiKey.takeLast(6)}" else apiKey,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
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
                            name = realm,
                            apiKey = apiKey,
                            outletReference = outletReference,
                            realm = realm,
                            region = region
                        )
                    )
                    onCancel()
                },
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
