package payment.sdk.android.cardpayment.savedCard.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedCardViewBottomBar(
    bringIntoViewRequester: BringIntoViewRequester,
    amount: Int,
    currency: String,
    onPayClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .fillMaxWidth()
            .imePadding(),
        backgroundColor = Color.White,
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        elevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
            ) {
                Text(text = "Total")
                Text(text = "$currency $amount")
            }

            TextButton(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .width(96.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = Color.Black
                ),
                onClick = {
                    onPayClicked()
                },
                shape = RoundedCornerShape(percent = 15),

                ) {
                Text(text = "Pay", color = Color.White)
            }
        }
    }
}