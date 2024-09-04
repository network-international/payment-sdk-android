package payment.sdk.android.demo.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    topAppBarText: String,
    onSettingClicked: () -> Unit,
    onAddProduct: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(text = topAppBarText) },
        navigationIcon = {
            IconButton(onClick = onAddProduct) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            Spacer(modifier = Modifier.width(68.dp))
            IconButton(onClick = onSettingClicked) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
    )
}