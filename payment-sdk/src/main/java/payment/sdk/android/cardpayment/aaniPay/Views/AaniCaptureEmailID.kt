package payment.sdk.android.cardpayment.aaniPay.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import payment.sdk.android.sdk.R

@Composable
fun AaniCaptureEmailID() {
    var emailId by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    Column {
        OutlinedTextField(
            label = { Text(stringResource(R.string.aani_email_id)) },
            value = emailId,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .background(Color.White),
            onValueChange = {
                emailId = it
            },
            textStyle = MaterialTheme.typography.subtitle1,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
            ),
            placeholder = { Text(stringResource(R.string.aani_email_id)) },
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}