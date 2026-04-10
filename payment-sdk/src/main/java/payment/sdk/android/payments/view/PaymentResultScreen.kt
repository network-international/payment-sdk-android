package payment.sdk.android.payments.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import payment.sdk.android.payments.model.PaymentResultArgs
import payment.sdk.android.sdk.R

private val SuccessGreen = Color(0xFF2EB852)
private val FailureRed = Color(0xFFE63835)

@Composable
fun PaymentResultScreen(
    args: PaymentResultArgs,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Icon
        Icon(
            painter = painterResource(
                id = if (args.isSuccess) R.drawable.ic_payment_success else R.drawable.ic_payment_failure
            ),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .semantics { testTag = "sdk_result_image_status" },
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = if (args.isSuccess) {
                if (args.formattedAmount != null) {
                    stringResource(R.string.payment_result_success_title, args.formattedAmount)
                } else {
                    stringResource(R.string.payment_result_success_title_no_amount)
                }
            } else {
                stringResource(R.string.payment_result_failure_title)
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.semantics { testTag = "sdk_result_label_title" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = if (args.isSuccess) {
                stringResource(R.string.payment_result_success_subtitle)
            } else {
                stringResource(R.string.payment_result_failure_subtitle)
            },
            fontSize = 14.sp,
            color = Color(0xFF8F8F8F),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Details card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (args.isSuccess) {
                        stringResource(R.string.payment_result_transaction_id)
                    } else {
                        stringResource(R.string.payment_result_reference_number)
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF8F8F8F)
                )
                Text(
                    text = args.transactionId,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFE0E0E0)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.payment_result_date_time),
                    fontSize = 14.sp,
                    color = Color(0xFF8F8F8F)
                )
                Text(
                    text = args.dateTime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer
        PaymentFooterView(
            supportedCards = args.supportedCards
        )

        // Done button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { testTag = "sdk_result_button_done" },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2E6FF2),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(R.string.payment_result_done),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
