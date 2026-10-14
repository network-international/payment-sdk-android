package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.core.testId
import payment.sdk.android.demo.model.Environment
import payment.sdk.android.demo.model.EnvironmentType
import payment.sdk.android.demo.model.Region

/**
 * Add / edit an environment, as one scrolling bottom sheet.
 *
 * Replaces the pair of centred dialogs this screen used to show. Those duplicated the
 * same eleven fields twice and, once region, currency, order action and order type moved
 * onto the outlet, grew past the screen with no way to reach what was cut off. A sheet
 * scrolls naturally, gets out of the way of the keyboard, and gives both flows one
 * layout to keep consistent — mirroring the Flutter demo's single form widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentFormSheet(
    /** The environment being edited, or null to add a new one. */
    existing: Environment?,
    onCancel: () -> Unit,
    onSave: (Environment) -> Unit,
) {
    val context = LocalContext.current
    val isEdit = existing != null
    val idPrefix = if (isEdit) "editenv" else "addenv"

    var type by remember { mutableStateOf(existing?.type ?: EnvironmentType.DEV) }
    var nickname by remember { mutableStateOf(existing?.nickname.orEmpty()) }
    var realm by remember { mutableStateOf(existing?.realm.orEmpty()) }
    var apiKey by remember { mutableStateOf(existing?.apiKey.orEmpty()) }
    var outletReference by remember { mutableStateOf(existing?.outletReference.orEmpty()) }
    var clickToPay by remember { mutableStateOf(existing?.clickToPayMerchantId.orEmpty()) }
    // Resolved through the environment's own accessors so a pre-migration null shows the
    // old app-wide value rather than a default that would silently overwrite it on save.
    var region by remember { mutableStateOf(existing?.region ?: Region.UAE) }
    var currency by remember { mutableStateOf(currencyOf(existing?.currencyCode(context))) }
    var orderAction by remember { mutableStateOf(orderActionOf(existing?.orderActionCode(context))) }
    var orderType by remember { mutableStateOf(orderTypeOf(existing?.orderTypeCode(context))) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (isEdit) "Edit Environment" else "Add Environment",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().testId("${idPrefix}_picker_type")
            ) {
                EnvironmentType.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = { type = option },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = EnvironmentType.entries.size,
                        ),
                    ) { Text(option.value) }
                }
            }

            Field("Nickname (optional)", nickname, "${idPrefix}_field_nickname") { nickname = it }
            Field("Realm", realm, "${idPrefix}_field_realm") { realm = it }
            Field("API Key", apiKey, "${idPrefix}_field_apiKey") { apiKey = it }
            Field("Outlet Reference", outletReference, "${idPrefix}_field_outletReference") {
                outletReference = it
            }

            OutletSettingsFields(
                region = region,
                currency = currency,
                orderAction = orderAction,
                orderType = orderType,
                idPrefix = idPrefix,
                onRegionChange = { region = it },
                onCurrencyChange = { currency = it },
                onOrderActionChange = { orderAction = it },
                onOrderTypeChange = { orderType = it },
            )

            Field(
                "Click to Pay Merchant ID (optional)", clickToPay,
                "${idPrefix}_field_clickToPayMerchantId",
            ) { clickToPay = it }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // Built from `existing` where there is one, so a field this form does
                    // not show (or a future one) is carried over rather than reset.
                    val base = existing ?: Environment(
                        type = type, name = realm, apiKey = apiKey,
                        outletReference = outletReference, realm = realm, region = region,
                    )
                    onSave(
                        base.copy(
                            type = type,
                            name = nickname.ifBlank { realm },
                            nickname = nickname.ifBlank { null },
                            apiKey = apiKey,
                            outletReference = outletReference,
                            realm = realm,
                            region = region,
                            currency = currency.code,
                            orderAction = orderAction.code,
                            orderType = orderType.code,
                            clickToPayMerchantId = clickToPay.ifBlank { null },
                        )
                    )
                },
                enabled = apiKey.isNotBlank() && outletReference.isNotBlank() && realm.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testId("${idPrefix}_button_save"),
            ) { Text("Save") }

            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().testId("${idPrefix}_button_cancel"),
            ) { Text("Cancel") }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    testTag: String,
    onValueChange: (String) -> Unit,
) {
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testId(testTag),
    )
}

/** Confirms an environment deletion. The key it discards may need another portal trip. */
@Composable
fun DeleteEnvironmentDialog(
    environment: Environment,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete environment") },
        text = {
            Text("Remove ${environment.nickname ?: environment.realm}? This cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testId("environment_button_confirmDelete"),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
