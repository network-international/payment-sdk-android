package payment.sdk.android.cardpayment.aaniPay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import payment.sdk.android.cardpayment.aaniPay.model.AaniIDType
import payment.sdk.android.sdk.R

@Composable
fun AaniPayScreen(
    onNavigationUp: () -> Unit,
) {
    var selectedInputType by remember { mutableStateOf(AaniIDType.MOBILE_NUMBER) }
    var inputValue by remember { mutableStateOf("") }
    var isInputValid by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val inputTypes = AaniIDType.entries

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier.height(80.dp),
            painter = painterResource(R.drawable.aani_logo),
            contentScale = ContentScale.Fit,
            contentDescription = ""
        )

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .height(62.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                label = { Text(stringResource(R.string.aani_alias_type)) },
                value = stringResource(selectedInputType.resourceId),
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
                    .clickable(enabled = true) { expanded = !expanded },
                color = Color.Transparent,
            ) {}

            DropdownMenu(
                modifier = Modifier.fillMaxWidth(0.90f),
                expanded = expanded,
                onDismissRequest = {
                    expanded = !expanded
                }
            ) {
                inputTypes.forEach { type ->
                    DropdownMenuItem(onClick = {
                        expanded = !expanded
                        selectedInputType = type
                        inputValue = ""
                    }) {
                        Text(stringResource(type.resourceId))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth(),
        ) {
            if (selectedInputType == AaniIDType.MOBILE_NUMBER) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color.White,
                    border = BorderStroke(width = 1.dp, color = Color.Gray),
                    modifier = Modifier.clip(MaterialTheme.shapes.small)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            modifier = Modifier
                                .height(56.dp)
                                .padding(horizontal = 12.dp)
                                .wrapContentSize(Alignment.Center),
                            text = "+971",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.subtitle1,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            OutlinedTextField(
                label = { Text(stringResource(selectedInputType.resourceId)) },
                value = inputValue,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .background(Color.White),
                onValueChange = {
                    if (selectedInputType.length >= it.length) {
                        inputValue = it
                        isInputValid = selectedInputType.validate(it)
                    }
                },
                textStyle = MaterialTheme.typography.subtitle1,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = selectedInputType.keyboardType,
                ),
                placeholder = { Text(selectedInputType.sample) },
            )
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = if (isInputValid) colorResource(id = R.color.payment_sdk_pay_button_background_color) else Color.Gray,
            ),
            onClick = {

            },
            enabled = isInputValid,
            shape = RoundedCornerShape(percent = 15),
        ) {
            Text(
                text = stringResource(R.string.make_payment),
                color = colorResource(id = R.color.payment_sdk_pay_button_text_color)
            )
        }
    }
}

@Preview
@Composable
fun AaniPayScreenPreview() {
    Box {
        AaniPayScreen {

        }
    }
}