package payment.sdk.android.cardpayment.aaniPay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import payment.sdk.android.cardpayment.aaniPay.Views.AaniCaptureEmailID
import payment.sdk.android.cardpayment.aaniPay.Views.AaniCaptureEmiratesID
import payment.sdk.android.cardpayment.aaniPay.Views.AaniCaptureMobileNumber
import payment.sdk.android.cardpayment.aaniPay.Views.AaniCapturePassportID
import payment.sdk.android.cardpayment.aaniPay.model.AaniIDType
import payment.sdk.android.cardpayment.aaniPay.model.getName
import payment.sdk.android.sdk.R

@Composable
fun AaniPayScreen(
    onNavigationUp: () -> Unit,
) {
    var isValid by remember { mutableStateOf(false) }
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
        val aliasType = AaniIDType.entries
        var selectedIndex by remember { mutableStateOf(AaniIDType.MOBILE_NUMBER) }
        Box(
            modifier = Modifier
                .height(62.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                label = { Text(stringResource(R.string.aani_alias_type)) },
                value = selectedIndex.getName(),
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
                modifier = Modifier.fillMaxWidth(0.90f), expanded = expanded, onDismissRequest = {
                    expanded = !expanded
                }
            ) {
                aliasType.forEach { type ->
                    DropdownMenuItem(onClick = {
                        expanded = !expanded
                        selectedIndex = type
                    }) {
                        Text(type.getName())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedIndex) {
            AaniIDType.MOBILE_NUMBER -> AaniCaptureMobileNumber {
                isValid = it
            }
            AaniIDType.EMIRATES_ID -> AaniCaptureEmiratesID()
            AaniIDType.PASSPORT_ID -> AaniCapturePassportID()
            AaniIDType.EMAIL_ID -> AaniCaptureEmailID()
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = if (isValid) colorResource(id = R.color.payment_sdk_pay_button_background_color) else Color.Gray,
            ),
            onClick = onNavigationUp,
            enabled = isValid,
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