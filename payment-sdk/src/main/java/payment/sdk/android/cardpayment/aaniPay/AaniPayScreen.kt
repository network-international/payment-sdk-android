package payment.sdk.android.cardpayment.aaniPay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import payment.sdk.android.cardpayment.aaniPay.Views.AliasTypeView
import payment.sdk.android.cardpayment.aaniPay.Views.CountryCodeView
import payment.sdk.android.cardpayment.aaniPay.model.AaniIDType
import payment.sdk.android.cardpayment.widget.PayButton
import payment.sdk.android.sdk.R

@Composable
fun AaniPayScreen(
    onSubmit: (alias: AaniIDType, value: String) -> Unit
) {
    var selectedInputType by remember { mutableStateOf(AaniIDType.MOBILE_NUMBER) }
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
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
            modifier = Modifier.height(120.dp),
            painter = painterResource(R.drawable.aani_logo),
            contentScale = ContentScale.Fit,
            contentDescription = ""
        )

        Spacer(modifier = Modifier.height(16.dp))

        AliasTypeView(
            options = inputTypes,
            type = selectedInputType,
            expanded = expanded,
            onExpand = { expanded = !expanded },
            onSelected = {
                expanded = !expanded
                selectedInputType = it
                inputValue = TextFieldValue(
                    text = if (it == AaniIDType.EMIRATES_ID) "784" else "",
                    selection = if (it == AaniIDType.EMIRATES_ID) TextRange(
                        3,
                        3
                    ) else TextRange.Zero
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth(),
        ) {
            if (selectedInputType == AaniIDType.MOBILE_NUMBER) {
                CountryCodeView()
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
                    if (selectedInputType.length >= it.text.length) {
                        inputValue = it
                        isInputValid = selectedInputType.validate(
                            selectedInputType.inputFormatter.filter(AnnotatedString(it.text)).text.text
                        )
                    }
                },
                visualTransformation = selectedInputType.inputFormatter,
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

        PayButton(
            text = stringResource(R.string.make_payment),
            isValid = isInputValid
        ) {
            onSubmit(selectedInputType, inputValue.text)
        }
    }
}

@Preview
@Composable
fun AaniPayScreenPreview() {
    Box {
        AaniPayScreen { _, _ ->

        }
    }
}