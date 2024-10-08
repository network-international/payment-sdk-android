package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import payment.sdk.android.demo.model.AppCurrency

@Composable
fun CurrencyPickerView(setCurrency: AppCurrency, onCurrencySelected: (AppCurrency) -> Unit) {
    val currencies = AppCurrency.entries.sortedBy { it.code }
    var selectedCurrency by remember { mutableStateOf(setCurrency) }

    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "Currency",
                style = MaterialTheme.typography.titleMedium
            )

            Text(selectedCurrency.code, style = MaterialTheme.typography.titleMedium)

            Icon(
                modifier = Modifier.padding(horizontal = 8.dp),
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Keyboard Arrow Down"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.code} (${currency.countryName})") },
                    onClick = {
                        selectedCurrency = currency
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}