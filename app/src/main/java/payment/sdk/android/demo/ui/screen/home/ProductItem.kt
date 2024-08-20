package payment.sdk.android.demo.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import payment.sdk.android.demo.formatCurrency
import payment.sdk.android.demo.model.Product

@Composable
fun ProductItem(
    product: Product,
    isSelected: Boolean,
    currency: String,
    onClick: () -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = Modifier
            .padding(8.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable {
                onClick()
            }
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$currency ${product.amount.formatCurrency()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (product.isLocal && !isSelected) {
                IconButton(modifier = Modifier.align(Alignment.TopEnd), onClick = {
                    onDeleteProduct(product)
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "")
                }
            }
        }

    }
}