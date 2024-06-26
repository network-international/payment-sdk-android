package payment.sdk.android.cardpayment.aaniPay

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly

@Composable
fun AaniPayScreen(
    onNavigationUp: () -> Unit,
) {
    var mobileNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mobile Number",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCA9A00)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please enter your mobile number to activate Aani",
            fontSize = 16.sp,
            color = Color(0xFF174163)
        )
        val focusRequester = remember { FocusRequester() }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = MaterialTheme.shapes.small,
            color = Color.White,
            border = BorderStroke(width = 1.dp, color = Color.Gray),
            modifier = Modifier.clip(MaterialTheme.shapes.small)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White)
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                Text("AE +971")
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = mobileNumber,
                    onValueChange = {
                        if (it.length <= 10 && it.isDigitsOnly()) {
                            mobileNumber = it
                        }
                    },
                    placeholder = { Text(text = "Mobile Number") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .background(Color.White)
                )
                IconButton(onClick = {
                    onNavigationUp()
                }, enabled = mobileNumber.length >= 10) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Submit"
                    )
                }
            }
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
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