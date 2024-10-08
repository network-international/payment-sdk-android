package payment.sdk.android.aaniPay.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import payment.sdk.android.aaniPay.model.AaniIDType

@Composable
internal fun AliasTypeView(
    options: List<AaniIDType>,
    type: AaniIDType,
    expanded: Boolean,
    onSelected: (AaniIDType) -> Unit,
    onExpand: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(62.dp)
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = stringResource(type.resourceId),
            enabled = false,
            modifier = Modifier.fillMaxSize(),
            colors = TextFieldDefaults.textFieldColors(
                disabledTextColor = Color.Black,
                disabledLabelColor = Color.Black,
                backgroundColor = Color.Transparent,
                disabledTrailingIconColor = Color.Black,
            ),
            textStyle = MaterialTheme.typography.subtitle1,
            trailingIcon = {
                val icon =
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                Icon(icon, "")
            },
            onValueChange = { },
            readOnly = false,
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = true) { onExpand() },
            color = Color.Transparent,
        ) {}

        DropdownMenu(
            modifier = Modifier.fillMaxWidth(0.90f),
            expanded = expanded,
            onDismissRequest = onExpand
        ) {
            options.forEach { type ->
                DropdownMenuItem(onClick = {
                    onSelected(type)
                }) {
                    Text(stringResource(type.resourceId))
                }
            }
        }
    }
}