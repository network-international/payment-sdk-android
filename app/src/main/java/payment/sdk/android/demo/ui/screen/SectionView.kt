package payment.sdk.android.demo.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import payment.sdk.android.R

@Composable
fun SectionView(
    title: String,
    count: Int,
    showDialog: () -> Unit,
    isExpanded: Boolean = false,
    onExpand: (Boolean) -> Unit = {},
    onScan: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            if (onScan != null) {
                IconButton(onClick = onScan) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_qr_scan),
                        contentDescription = "Scan QR code"
                    )
                }
            }
            IconButton(onClick = showDialog) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = "add")
            }
            if (count != 0) {
                IconButton(onClick = {
                    onExpand(!isExpanded)
                }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "add"
                    )
                }
            }
        }

        HorizontalDivider()
        AnimatedVisibility(
            visible = isExpanded && count != 0,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
            ) {
                content()
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }
        }
    }
}
