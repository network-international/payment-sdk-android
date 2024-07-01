package payment.sdk.android.cardpayment.aaniPay.Views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import payment.sdk.android.sdk.R

@Composable
fun AaniCaptureMobileNumber(
    isValid: (Boolean) -> Unit
) {
    var mobileNumber by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth(),
    ) {
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
        OutlinedTextField(
            label = { Text(stringResource(R.string.aani_mobile_number)) },
            value = mobileNumber,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .background(Color.White),
            onValueChange = {
                if (it.length <= 10 && it.isDigitsOnly()) {
                    mobileNumber = it
                    isValid(false)
                }
                if (it.length >= 10 && it.isDigitsOnly()) {
                    isValid(true)
                }
            },
            textStyle = MaterialTheme.typography.subtitle1,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
            ),
            placeholder = { Text(stringResource(R.string.aani_mobile_number)) },
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
fun AaniCaptureMobileNumberPreview() {
    Box {
        AaniCaptureMobileNumber() {

        }
    }
}