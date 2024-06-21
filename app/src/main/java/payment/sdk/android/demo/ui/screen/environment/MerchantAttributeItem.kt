package payment.sdk.android.demo.ui.screen.environment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import payment.sdk.android.demo.model.MerchantAttribute

@Composable
fun MerchantAttributeItem(
    merchantAttribute: MerchantAttribute,
    deleteMerchantAttribute: () -> Unit,
    onChecked: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .clip(MaterialTheme.shapes.small),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = merchantAttribute.key, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.width(16.dp))

            Text(text = merchantAttribute.value, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.weight(1f))

            Checkbox(checked = merchantAttribute.isActive, onCheckedChange = onChecked)

            IconButton(onClick = deleteMerchantAttribute) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "delete")
            }
        }
    }
}