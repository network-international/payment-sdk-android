package payment.sdk.android.demo.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.sdk.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    topAppBarText: String,
    onSettingClicked: () -> Unit,
    onAddProduct: () -> Unit,
    onInfoClicked: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.network_international_logo),
                    contentDescription = "Network International",
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = "Demo",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
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
            IconButton(onClick = onInfoClicked) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "What You Need",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
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
